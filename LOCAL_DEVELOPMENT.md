# Local Development Procedure

This guide is optimized for fast, repeatable local development with minimal environment drift.

For local documentation preview, run `make run-wiki` (override with `WIKI_PORT=<port>`).

## Scope

Use this guide when you need:

- Platform dependencies running in Docker Compose.
- Java API running on host (recommended) or as a container.
- Airflow, Kafka, MinIO, Postgres, and Conduktor integration available for end-to-end testing.
- Local Kubernetes deployment on Kind for container-level validation.

## Architecture Notes (Local)

This stack intentionally uses **two Postgres services**:

- `postgres` on `localhost:5432`: main project database (`processed_db`).
- `postgres-conduktor` on `localhost:5433`: Conduktor metadata database (`conduktor_db`).

This separation prevents Conduktor schema/state from interfering with project data.

## Prerequisites

1. Docker Desktop running.
2. Docker Compose available.
3. Java 17 and Maven installed on host.
4. Optional: Python environment for validation utilities.

## Quick Start (Recommended Daily Path)

Run these commands from repository root:

```bash
make up
make ps
make run-java-api-local-safe
```

In another terminal, validate readiness:

```bash
curl -sS http://localhost:8081/actuator/health
curl -sS http://localhost:8081/api/monitor/health
```

If both checks are healthy, start testing API and pipeline flows.

## Quick Start (Local Kubernetes with Kind)

Use this path when you want to validate Kubernetes manifests and container behavior locally:

```bash
make kind-deploy
make kind-status
```

Kind endpoints (from host):

- Airflow UI: http://localhost:8080
- Workflow API: http://localhost:8081
- MinIO API: http://localhost:9000
- MinIO Console: http://localhost:9001
- Kafka broker: localhost:9092

Destroy local Kind cluster:

```bash
make kind-down
```

## 1. Start Platform Services

```bash
make up
```

Verify service state:

```bash
make ps
```

View logs when startup is slow or unhealthy:

```bash
make logs
```

## 2. Local Endpoints and Credentials

Core endpoints:

- Airflow UI: http://localhost:8080
- Java API (host run): http://localhost:8081
- MinIO API: http://localhost:9000
- MinIO Console: http://localhost:9001
- Conduktor UI: http://localhost:8085/app
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

Database endpoints:

- Project Postgres: localhost:5432 (`processed_db`)
- Conduktor Postgres: localhost:5433 (`conduktor_db`)

Default local credentials:

- Airflow: `airflow_user` / `airflow_pass`
- Conduktor: `admin@local.dev` / `admin123`

## 3. Run Java API (Profile Decision)

Use the correct profile based on where the API runs.

- Host-run API: use `local` profile.
- Container-run API: use `compose` profile.

Recommended (safe host run):

```bash
make run-java-api-local-safe
```

Why this is preferred:

- Automatically handles stale process conflicts on port `8081`.
- Uses the right host networking assumptions for local development.
- Reduces startup failures from manual process cleanup mistakes.

Alternative commands:

```bash
make run-java-api-local
make run-java-api-compose
```

Direct Maven command (host run):

```bash
cd java-api
mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests
```

Run API as container service:

```bash
make build-java-api-container
make run-java-api-container
```

Container API logs and stop:

```bash
make logs-java-api-container
make stop-java-api-container
```

## 4. Development Loop (Fast Iteration)

1. Ensure `make up` is active.
2. Start API with `make run-java-api-local-safe`.
3. Execute API requests.
4. Validate side effects in Airflow, Kafka, MinIO, and Postgres.
5. Repeat after code changes.

Common API endpoints:

- `POST /api/batch/ingest`
- `POST /api/stream/produce`
- `POST /api/stream/run`
- `POST /api/governance/lineage`
- `POST /api/ml/run`
- `POST /api/ci/trigger`
- `GET /api/monitor/health`

## 5. Pipeline and Data Commands (Container Side)

From repository root:

```bash
make run-kafka-producer
make run-streaming-job
make run-batch-job
make run-iceberg-demo
```

Behavior note:

- `run-batch-job` and `run-iceberg-demo` auto-seed demo data via `prepare-demo-data`.

## 6. Validation Before Commit

```bash
make validate
```

Optional formatting:

```bash
make format
```

## 7. Shutdown and Cleanup

Stop services:

```bash
make down
```

Stop and remove containers plus volumes:

```bash
make clean
```

## Troubleshooting Matrix

### A) Java API startup fails with exit code 1

Likely causes:

- Port `8081` already in use.
- Wrong runtime profile for current network topology.

Actions:

```bash
make run-java-api-local-safe
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

If running host-side API, ensure profile is `local`, not `compose`.

### B) API is up but dependency health fails

Actions:

```bash
make ps
make logs
curl -sS http://localhost:8081/api/monitor/health
```

Interpretation:

- If dependencies show unavailable, confirm service containers are healthy before retrying API workflows.

### C) Airflow DAG not running

Likely causes:

- DAG is paused.
- Task is retrying due to dependency errors.

Actions:

```bash
docker-compose --project-directory . -f infra/compose/docker-compose.yaml exec -T airflow-webserver airflow dags list
docker-compose --project-directory . -f infra/compose/docker-compose.yaml exec -T airflow-webserver airflow dags unpause batch_ingestion_dag
docker-compose --project-directory . -f infra/compose/docker-compose.yaml exec -T airflow-webserver airflow dags trigger batch_ingestion_dag
```

### D) Conduktor cannot start or cannot persist state

Checks:

- Ensure `postgres-conduktor` is up on `5433`.
- Ensure Conduktor points to `postgres-conduktor` in compose env.

Actions:

```bash
docker-compose --project-directory . -f infra/compose/docker-compose.yaml ps postgres-conduktor conduktor
docker-compose --project-directory . -f infra/compose/docker-compose.yaml logs conduktor
```

### E) Iceberg/batch demo missing source data

```bash
make prepare-demo-data
make run-iceberg-demo
```

## Operational Tips

- Keep one terminal for long-running services (`make up`, API run command).
- Use a second terminal for `curl`, DAG triggers, and diagnostics.
- Prefer Make targets over direct long commands to reduce local inconsistencies.
