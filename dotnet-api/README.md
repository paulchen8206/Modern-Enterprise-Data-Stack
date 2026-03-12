# 🧩 Sample .NET Backend for the Modern Data Stack 💎

This project exposes a production-ready API that orchestrates the pipeline components (MySQL, PostgreSQL, MinIO, Kafka, Airflow, Great Expectations, Atlas, MLflow, GitHub Actions).

## Quick Start

```bash
dotnet restore src/DataPipelineApi/DataPipelineApi.csproj
dotnet run --project src/DataPipelineApi/DataPipelineApi.csproj
```

## Capabilities

- Batch ingestion: read from MySQL, persist raw JSON to MinIO, optionally run Great Expectations, and trigger the batch Airflow DAG.
- Streaming: produce enriched events to Kafka and trigger the streaming Airflow DAG.
- Governance & ML: register lineage to Atlas and start MLflow runs.
- CI/CD: trigger GitHub Actions workflows for pipeline deployments.
- Observability: structured health checks for every dependency, HTTP request logging, consistent error responses, forwarded header support, HSTS (non-dev), response compression, and request correlation IDs (`X-Request-ID`).

## Configuration

All settings live in `appsettings.json` and can be overridden via environment variables. Important keys:

- `ConnectionStrings.MySql` / `ConnectionStrings.Postgres` and `CommandTimeoutSeconds`
- `Minio.Endpoint`, `AccessKey`, `SecretKey`, `BucketRaw`, `BucketProcessed`
- `Kafka.BootstrapServers`, `Topic`, `ClientId`
- `Airflow.BaseUrl`, `Username`, `Password`, `BatchDagId`, `StreamingDagId`
- `GreatExpectations.CliPath`, `TimeoutSeconds`
- `Atlas.Endpoint`, `Username`, `Password`
- `MLflow.TrackingUri`, `RequestTimeoutSeconds`
- `GitHub.ActionsApi`, `Token`, `UserAgent`

## Container Run

```bash
docker build -f infra/dockerfiles/sample_dotnet_backend.Dockerfile -t data-pipeline-api dotnet-api
docker run -p 8080:80 --env-file .env data-pipeline-api
```

## Project Layout

- `src/DataPipelineApi/Program.cs`: host setup, middleware, dependency wiring.
- `src/DataPipelineApi/Controllers/`: API endpoints for batch, streaming, governance, ML, CI, and monitoring.
- `src/DataPipelineApi/Services/`: integrations with Kafka, Airflow, MinIO, Atlas, MLflow, and GitHub Actions.
- `appsettings.json`: default local configuration.

## Key endpoints

- `POST /api/batch/ingest` – body: `{ "sourceTable": "table", "destinationPrefix": "...", "limit": 100, "triggerAirflow": true, "runGreatExpectations": true }`
- `POST /api/stream/produce` – body: `{ "partition": 0, "payload": { ... } }`
- `POST /api/stream/run` – trigger streaming DAG
- `POST /api/governance/lineage` – Atlas lineage payload
- `POST /api/ml/run` – query: `expId`, `name`
- `POST /api/ci/trigger` – query: `wf`, `branch`
- `GET  /api/monitor/health` or `/health` – dependency health map

Swagger is available at `/swagger` in Development.
