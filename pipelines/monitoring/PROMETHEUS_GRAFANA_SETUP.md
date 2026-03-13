# 📊 Prometheus & Grafana Setup (High-Level) 🌈

## Scope

This guide covers practical monitoring setup for Kafka, Spark, and Airflow using Prometheus + Grafana.

## Current Implementation References

- Compose runtime entrypoint: `infra/compose/docker-compose.yaml`
- Local Kubernetes runtime: `k8s/kind/stack.yaml` with `ops/deploy-kind.sh`
- API and pipeline orchestration references: `java-api/README.md` and `docs/ARCHITECTURE.md`
- CI/CD workflow files: `.github/workflows/ci.yml` and `.github/workflows/cd.yml`
- Branch and environment flow: push `dev` for CI/dev checks, PR to `qa`/`stg`/`prd` for env-specific CI checks and Helm CD deployment.

## Component Procedure

### 1. Bootstrap Observability Stack

1. Start runtime stack with `make up`.
1. Start or verify Prometheus (`:9090`) and Grafana (`:3000`).
1. Confirm Prometheus data source is configured in Grafana.

### 2. Instrument Core Components

1. Kafka: expose broker metrics via JMX exporter.
1. Spark: expose driver/executor metrics via Prometheus sink or JMX.
1. Airflow: export scheduler/webserver/task metrics (StatsD/exporter path).

### 3. Verify and Alert

1. Validate all scrape targets are healthy.
1. Import dashboards and verify key panels populate.
1. Trigger a controlled failure and confirm alert routing.

## Prometheus Configuration

- Create `prometheus.yml` with scrape jobs for:
  - Kafka brokers (`:9092/metrics` or JMX exporter)
  - Spark (JMX or Spark Prometheus sink)
  - Airflow (StatsD exporter or custom metrics plugin)

## Grafana Configuration

- Start Grafana:

  ```bash
  docker run -d -p 3000:3000 --name grafana grafana/grafana
  ```

- Add Prometheus as a data source.
- Import/create dashboards for Kafka throughput and lag, Spark jobs, and Airflow DAG health.

## Compose Snippet (Optional)

```yaml
prometheus:
  image: prom/prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml

grafana:
  image: grafana/grafana
  ports:
    - "3000:3000"
```

## Alerting

- Define alerting rules in `rules.yml`.
- Connect Alertmanager to Slack/email/webhook receivers.

```yaml
alertmanager:
  image: prom/alertmanager
  ports:
    - "9093:9093"
  volumes:
    - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
```

## Operations Checklist

- Confirm targets are up in Prometheus.
- Confirm dashboards refresh in Grafana.
- Verify critical alerts fire during controlled test failures.
- Review dashboard and alert drift during each release cycle.

## Best Practices by Component

Kafka:

- Track produce/consume throughput, lag, and broker availability as baseline SLOs.
- Alert on sustained lag growth, not single-sample spikes.

Spark:

- Track batch duration, micro-batch latency, and failed stages.
- Include executor memory/GC pressure in dashboards.

Airflow:

- Track DAG success rate, task retry counts, and scheduling delay.
- Alert on repeated task failures and scheduler unavailability.

Platform-wide:

- Standardize labels (service, env, component) across all metrics.
- Keep dashboard panels tied to runbooks and owner teams.

## Troubleshooting

- Missing metrics:
  - Verify Prometheus scrape targets and exporters.
- Stale dashboards:
  - Check Grafana data source health and query time ranges.
- Missing alerts:
  - Validate Alertmanager routing configuration and receiver credentials.

## Recommended Operations

- Start stack services with `make up`.
- Verify runtime status with `make ps`.
- Tail core service logs with `make logs`.
- Validate compose configuration with `make validate-compose`.
