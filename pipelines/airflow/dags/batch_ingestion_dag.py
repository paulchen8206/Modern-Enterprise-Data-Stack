"""Batch ingestion DAG.

This DAG extracts orders from MySQL, performs lightweight validation,
publishes raw data to MinIO, applies a local transform fallback, and then
loads the final dataset into Postgres.
"""

from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.providers.mysql.hooks.mysql import MySqlHook
from airflow.providers.postgres.hooks.postgres import PostgresHook
from datetime import datetime
import pandas as pd
import boto3
import logging

default_args = {"owner": "airflow", "start_date": datetime(2023, 1, 1), "retries": 1}


def extract_data_from_mysql(**kwargs):
    """
    Extract batch data from MySQL.
    Pulls data from the 'orders' table and writes it to /tmp/orders.csv.
    """
    logging.info("Starting extraction from MySQL...")
    mysql_hook = MySqlHook(mysql_conn_id="mysql_default")
    df = mysql_hook.get_pandas_df("SELECT * FROM orders;")
    df.to_csv("/tmp/orders.csv", index=False)
    logging.info(
        f"Extracted {len(df)} records from MySQL and saved to /tmp/orders.csv."
    )


def validate_data_with_ge(**kwargs):
    """
    Validate extracted data with Great Expectations.
    - Ensures 'order_id' is not null
    - Ensures 'amount' is not zero or negative
    """
    logging.info("Validating /tmp/orders.csv...")
    df = pd.read_csv("/tmp/orders.csv")

    if "order_id" not in df.columns:
        raise ValueError("Data validation failed: missing required column 'order_id'")
    if "amount" not in df.columns:
        raise ValueError("Data validation failed: missing required column 'amount'")

    # 1) Ensure no null in 'order_id'
    if df["order_id"].isnull().any():
        raise ValueError("Data validation failed: 'order_id' has null values")

    # 2) Ensure 'amount' is strictly greater than 0
    if (df["amount"] <= 0).any():
        raise ValueError("Data validation failed: 'amount' is not strictly positive")

    logging.info("Data validation checks passed.")


def load_to_minio(**kwargs):
    """
    Load raw data CSV to MinIO (S3-compatible).
    Bucket: 'raw-data'
    Key: 'orders/orders.csv'
    """
    logging.info("Uploading CSV to MinIO (raw-data bucket)...")
    s3 = boto3.client(
        "s3",
        endpoint_url="http://minio:9000",
        aws_access_key_id="minio",
        aws_secret_access_key="minio123",
        region_name="us-east-1",
    )
    bucket_name = "raw-data"
    # Attempt to create bucket if not exists
    try:
        s3.create_bucket(Bucket=bucket_name)
        logging.info(f"Bucket '{bucket_name}' created or already exists.")
    except Exception as e:
        logging.info(f"Bucket creation skipped (possibly exists): {e}")

    # Upload file
    s3.upload_file("/tmp/orders.csv", bucket_name, "orders/orders.csv")
    logging.info("File successfully uploaded to MinIO.")


def transform_data_locally(**kwargs):
    """
    Local fallback transform for Airflow runtime.
    Produces /tmp/transformed_orders.csv expected by the Postgres load task.
    """
    logging.info("Transforming data locally from /tmp/orders.csv...")
    df = pd.read_csv("/tmp/orders.csv")

    required_cols = ["order_id", "customer_id", "amount"]
    missing_cols = [c for c in required_cols if c not in df.columns]
    if missing_cols:
        raise ValueError(f"Transform failed: missing required columns {missing_cols}")

    df["order_id"] = pd.to_numeric(df["order_id"], errors="raise").astype(int)
    df["customer_id"] = pd.to_numeric(df["customer_id"], errors="raise").astype(int)
    df["amount"] = pd.to_numeric(df["amount"], errors="raise").astype(float)

    df = df[df["amount"] > 0]
    df = df.drop_duplicates(subset=["order_id"])
    df["processed_timestamp"] = pd.Timestamp.utcnow().isoformat()

    df.to_csv("/tmp/transformed_orders.csv", index=False)
    logging.info(
        f"Local transform complete. Wrote {len(df)} rows to /tmp/transformed_orders.csv."
    )


def load_to_postgres(**kwargs):
    """
    Load final transformed data into Postgres table 'orders_transformed'.
    Assumes Spark job wrote /tmp/transformed_orders.csv locally.

    Table schema:
    - order_id INT
    - customer_id INT
    - amount DECIMAL(10,2)
    - processed_timestamp TIMESTAMP
    """
    logging.info("Starting load into Postgres from /tmp/transformed_orders.csv...")
    pg_hook = PostgresHook(postgres_conn_id="postgres_default")
    df = pd.read_csv("/tmp/transformed_orders.csv")

    pg_hook.run(
        "CREATE TABLE IF NOT EXISTS orders_transformed ( \
        order_id INT, \
        customer_id INT, \
        amount DECIMAL(10,2), \
        processed_timestamp TIMESTAMP );"
    )

    # Clear table before load
    pg_hook.run("TRUNCATE TABLE orders_transformed;")

    # Insert row by row (simple approach)
    rows_loaded = 0
    for _, row in df.iterrows():
        insert_sql = """
        INSERT INTO orders_transformed(order_id, customer_id, amount, processed_timestamp)
        VALUES (%s, %s, %s, %s)
        """
        pg_hook.run(
            insert_sql,
            parameters=(
                row["order_id"],
                row["customer_id"],
                row["amount"],
                row["processed_timestamp"],
            ),
        )
        rows_loaded += 1

    logging.info(
        f"Finished loading {rows_loaded} records into Postgres (orders_transformed)."
    )


with DAG(
    dag_id="batch_ingestion_dag",
    default_args=default_args,
    schedule_interval="@daily",
    catchup=False,
) as dag:
    # The transform task is intentionally local so the DAG can run even when
    # Spark infrastructure is unavailable in lightweight environments.
    extract_task = PythonOperator(
        task_id="extract_mysql", python_callable=extract_data_from_mysql
    )

    validate_task = PythonOperator(
        task_id="validate_data", python_callable=validate_data_with_ge
    )

    load_to_minio_task = PythonOperator(
        task_id="load_to_minio", python_callable=load_to_minio
    )

    spark_transform_task = PythonOperator(
        task_id="spark_transform",
        python_callable=transform_data_locally,
    )

    load_postgres_task = PythonOperator(
        task_id="load_to_postgres", python_callable=load_to_postgres
    )

    # Define task dependencies
    (
        extract_task
        >> validate_task
        >> load_to_minio_task
        >> spark_transform_task
        >> load_postgres_task
    )
