# 🏗️ Infrastructure Containers Layout 🟠

This folder centralizes container definitions for the Modern Data Stack.

## <span style="color: #0ea5e9;">Directory Layout</span>

- `compose/`
  - `docker-compose.yaml`: canonical local and development stack
  - `docker-compose.ci.yaml`: CI-oriented compose stack
- `dockerfiles/`
  - `airflow.Dockerfile`: Airflow image definition
  - `spark.Dockerfile`: Spark image definition
  - `sample_dotnet_backend.Dockerfile`: .NET API sample image definition

## <span style="color: #0ea5e9;">Operational Workflow</span>

Use Make targets as the primary interface for day-to-day operations:

```bash
make up
make ps
make logs
make down
make clean
make validate-compose
```

Direct compose equivalent:

```bash
docker-compose --project-directory . -f infra/compose/docker-compose.yaml up --build -d
```

## <span style="color: #0ea5e9;">Build and Path Conventions</span>

- Build contexts are defined in `infra/compose/docker-compose.yaml` and point to canonical source locations under `pipelines/`.
- Dockerfiles are intentionally separated from service code to keep image definitions centralized.
- For local troubleshooting, run `make ps` and `make logs` before restarting services.
