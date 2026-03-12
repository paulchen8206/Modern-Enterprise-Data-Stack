# Local Kubernetes with Kind

This directory contains manifests and cluster configuration for running a local Kubernetes stack on Kind.

## What It Deploys

- Zookeeper
- Kafka
- MySQL
- PostgreSQL
- MinIO (API + Console)
- Airflow (Webserver + Scheduler)
- Workflow API (Spring profile: `compose`)

Namespace: `data-stack-local`

## One-Command Deployment

From repository root:

```bash
make kind-deploy
```

This command:

1. Creates a Kind cluster (if missing).
2. Builds `workflow-api` Docker image.
3. Loads the image into Kind.
4. Applies `k8s/kind/stack.yaml`.
5. Waits for deployments to become ready.

## Endpoints

Using host port mappings from `k8s/kind/cluster-config.yaml`:

- Airflow UI: `http://localhost:8080`
- Workflow API: `http://localhost:8081`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- Kafka broker: `localhost:9092`

## Useful Commands

```bash
make kind-status
make kind-smoke
make kind-down
```

## Notes

- This local stack is optimized for development smoke testing.
- It is intentionally separate from Docker Compose workflow.
- Existing cloud-focused manifests in `k8s/` are still valid for EKS/Argo paths.
