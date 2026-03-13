# 🏗️ Infrastructure Containers Layout 🟠

This folder centralizes container definitions for the Modern Data Stack.

## <span style="color: #0ea5e9;">Directory Layout</span>

- `compose/`
  - `docker-compose.yaml`: canonical local and development stack
  - `docker-compose.ci.yaml`: CI-oriented compose stack
- `dockerfiles/`
  - `airflow.Dockerfile`: Airflow image definition
  - `sample_dotnet_backend.Dockerfile`: sample .NET backend image definition
  - `spark.Dockerfile`: Spark image definition

## <span style="color: #0ea5e9;">Current Runtime Topology</span>

- Main local stack is defined in `compose/docker-compose.yaml`.
- The stack uses two Postgres services:
  - `postgres` on `5432` for project data.
  - `postgres-conduktor` on `5433` for Conduktor metadata.
- CI-focused stack overlays are defined in `compose/docker-compose.ci.yaml`.
- Branch and environment flow is standardized: push `dev` for CI/dev checks, PR to `qa`/`stg`/`prd` for env-specific CI checks and Helm CD deployment.

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

## <span style="color: #0ea5e9;">Quick Verification</span>

- Validate compose files:

  ```bash
  make validate-compose
  ```

- Check service state after startup:

  ```bash
  make ps
  ```

- Tail core logs for triage:

  ```bash
  make logs
  ```
