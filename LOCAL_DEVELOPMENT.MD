# Local Development Procedure

This guide defines the daily local workflow for container services and the Java API.

For local documentation preview, run `make run-wiki` (override with `WIKI_PORT=<port>`).

## Scope

Use this guide when you are developing locally and need:

- The platform services running in Docker Compose.
- The Java API running either inside containers (service dependencies) or on your host machine.

## Prerequisites

1. Docker Desktop running.
2. Docker Compose available.
3. Java 17 and Maven installed on host.
4. Optional Python venv for validation commands.

## 1. Start Local Container Platform

From repository root:

```bash
make up
```

Verify containers:

```bash
make ps
```

Check logs for core services:

```bash
make logs
```

## 2. Local Service Endpoints

After startup, use these endpoints:

- Airflow UI: http://localhost:8080
- Airflow API (internal from containers): http://airflow:8080/api/v1
- Java API (host run): http://localhost:8081
- MinIO API: http://localhost:9000
- MinIO Console: http://localhost:9001
- Conduktor (Kafka UI): http://localhost:8085/app
- Conduktor Postgres: localhost:5433 (database: conduktor_db)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

Airflow default local credentials:

- Username: airflow_user
- Password: airflow_pass

Conduktor default local credentials:

- Username: admin@local.dev
- Password: admin123

## 3. Run Java API Locally (Recommended for Development)

Keep containers running, then start the API from repository root with the safe target:

```bash
make run-java-api-local-safe
```

This target automatically stops any process already using port 8081, then starts the API with the local profile.

Alternative direct command (from java-api directory):

```bash
cd java-api
mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests
```

Expected startup port: 8081.

If you run the Java API in a containerized environment that resolves service names like kafka, mysql, and postgres, use:

```bash
cd java-api
mvn spring-boot:run -Dspring-boot.run.profiles=compose -DskipTests
```

Equivalent root-level Makefile shortcuts:

```bash
make run-java-api-local
make run-java-api-compose
make run-java-api-local-safe
```

Run Java API as a container service (compose profile) from repository root:

```bash
make build-java-api-container
make run-java-api-container
```

Tail/stop containerized API:

```bash
make logs-java-api-container
make stop-java-api-container
```

Health check:

```bash
curl http://localhost:8081/actuator/health
```

Then verify app dependency status:

```bash
curl http://localhost:8081/api/monitor/health
```

## 4. Java API Development Loop

1. Start or restart the API with make run-java-api-local-safe.
2. Send requests to target endpoints.
3. Validate effects in Airflow/MinIO/Kafka/Postgres.
4. Stop API with Ctrl+C and repeat.

Useful endpoints:

- POST /api/batch/ingest
- POST /api/stream/produce
- POST /api/stream/run
- POST /api/governance/lineage
- POST /api/ml/run
- POST /api/ci/trigger
- GET /api/monitor/health

## 5. Container-Only Pipeline Commands

Run from repository root:

```bash
make run-kafka-producer
make run-streaming-job
make run-batch-job
make run-iceberg-demo
```

Note:

- run-batch-job and run-iceberg-demo auto-seed MinIO demo data using prepare-demo-data.

## 6. Validate Before Commit

Run full project validation:

```bash
make validate
```

Optional formatting pass:

```bash
make format
```

## 7. Stop and Clean Up

Stop services:

```bash
make down
```

Stop and remove volumes:

```bash
make clean
```

## Troubleshooting

1. Containers not healthy:

```bash
make ps
make logs
```

2. Java API cannot connect to dependencies:

- Confirm containers are up with make ps.
- Confirm you are using the correct profile:
  - local profile for host-run Java API.
  - compose profile only when Java API runs in containerized network with service hostnames.

3. Java API start fails with Exit Code 1:

- Common cause: port 8081 is already in use.
- Use make run-java-api-local-safe instead of raw mvn command.
- If needed, verify active process with lsof -nP -iTCP:8081 -sTCP:LISTEN.

4. Iceberg demo fails on missing source object:

- Run make prepare-demo-data, then rerun make run-iceberg-demo.
