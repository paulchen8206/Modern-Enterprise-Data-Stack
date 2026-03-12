# 🧊 Apache Iceberg Tables Guide

This project supports writing Spark batch output to Apache Iceberg tables.

## <span style="color: #0ea5e9;">What Was Added</span>

- Iceberg runtime JAR in Spark image:
  - `infra/dockerfiles/spark.Dockerfile`
- Optional Iceberg write path in batch job:
  - `pipelines/spark/spark_batch_job.py`
- Make target for Iceberg demo run:
  - `make run-iceberg-demo`

## <span style="color: #0ea5e9;">Default Iceberg Settings</span>

The batch job reads these environment variables:

- `ENABLE_ICEBERG` (default: `false`)
- `ICEBERG_CATALOG` (default: `local`)
- `ICEBERG_NAMESPACE` (default: `analytics`)
- `ICEBERG_TABLE` (default: `orders`)
- `ICEBERG_WAREHOUSE` (default: `file:///tmp/iceberg_warehouse`)

## <span style="color: #0ea5e9;">Run Iceberg Demo</span>

1. Rebuild/start the stack so Spark has the Iceberg JAR:

```bash
make up
```

2. Run batch job with Iceberg enabled:

```bash
make run-iceberg-demo
```

The job still writes CSV outputs as before, and additionally writes to Iceberg table:

- `local.analytics.orders`

## <span style="color: #0ea5e9;">Validation Checklist</span>

- Confirm demo command exits successfully:

  ```bash
  make run-iceberg-demo
  ```

- Confirm logs include:
  - `Iceberg write complete: local.analytics.orders`
  - `Batch ETL process completed successfully`

- Confirm Spark image includes required jars:
  - Iceberg runtime jar
  - Hadoop AWS + AWS SDK bundle jars

## <span style="color: #0ea5e9;">Run Without Iceberg</span>

To use legacy behavior (CSV outputs only):

```bash
make run-batch-job
```

## <span style="color: #0ea5e9;">Notes</span>

- Iceberg is implemented with Spark Catalog type `hadoop` and a local warehouse by default.
- You can switch to object storage-backed warehouse paths by setting `ICEBERG_WAREHOUSE` and related Spark/Hadoop settings for your environment.

## <span style="color: #0ea5e9;">Troubleshooting</span>

- `spark-submit` not found:
  - Use explicit binary path `/opt/spark/bin/spark-submit` in container commands.
- `S3AFileSystem not found`:
  - Ensure Spark image includes `hadoop-aws` and `aws-java-sdk-bundle` jars.
- MinIO bucket not found:
  - Ensure `raw-data` and `processed-data` buckets exist before running batch jobs.
