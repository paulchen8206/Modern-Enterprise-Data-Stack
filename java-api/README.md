# Java Workflow API

Spring Boot implementation of the orchestration API.

## Implementation Context

- Compose integration profile uses hostnames and services from `infra/compose/docker-compose.yaml`.
- Local Kubernetes profile validation runs through `k8s/kind/stack.yaml` and `ops/deploy-kind.sh`.
- Repository automation is split into `.github/workflows/ci.yml` and `.github/workflows/cd.yml`.

## Run

```bash
cd java-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Run with Compose network hostnames (when Java API runs in a container/network with other services):

```bash
cd java-api
mvn spring-boot:run -Dspring-boot.run.profiles=compose
```

## Build

```bash
cd java-api
mvn clean package
```

## Endpoints

- `POST /api/batch/ingest`
- `POST /api/stream/produce`
- `POST /api/stream/run`
- `POST /api/governance/lineage`
- `POST /api/ml/run`
- `POST /api/ci/trigger`
- `GET /api/monitor/health`

## Component Procedures

### Batch component

1. Call `POST /api/batch/ingest` with `sourceTable` and optional `limit`.
2. Verify response includes `objectKey` and optional `runId`.
3. Confirm raw object in MinIO and transformed records downstream.

Best practices:

- Keep `sourceTable` allow-listed in production.
- Use bounded `limit` values for test and smoke runs.

### Streaming component

1. Call `POST /api/stream/produce` with `partition` and `payload`.
2. Trigger orchestration via `POST /api/stream/run`.
3. Validate consumer lag and sink write success.

Best practices:

- Keep payload schema versioned.
- Use deterministic partition strategy for ordering-sensitive streams.

### Governance and ML components

1. Register lineage via `POST /api/governance/lineage` after data publish.
2. Start experiment run with `POST /api/ml/run`.
3. Trigger CI workflow with `POST /api/ci/trigger` for release automation.

Best practices:

- Include dataset/version metadata in lineage payloads.
- Keep ML run parameters and artifacts reproducible.

### Monitoring component

1. Poll `GET /api/monitor/health` as readiness signal.
2. Wire health status into dashboards and alerting.

Best practices:

- Alert on sustained degraded state, not transient spikes.
- Track dependency-level and overall status together.

## Configuration

See `src/main/resources/application.yml` for defaults matching the local stack:

- MySQL/Postgres connection strings
- MinIO endpoint and buckets
- Kafka broker/topic
- Airflow base URL and credentials
- Great Expectations CLI path
- Atlas/MLflow/GitHub endpoints and credentials
