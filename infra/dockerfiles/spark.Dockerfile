FROM apache/spark:3.5.1

USER root
RUN apt-get update && apt-get install -y python3 python3-pip
RUN pip3 install boto3 pandas great_expectations

# Add Kafka connector for Spark (for streaming)
RUN wget https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/3.5.1/spark-sql-kafka-0-10_2.12-3.5.1.jar \
    -P $SPARK_HOME/jars
RUN wget https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/3.5.1/kafka-clients-3.5.1.jar \
    -P $SPARK_HOME/jars

WORKDIR /opt/spark_jobs

COPY . /opt/spark_jobs
