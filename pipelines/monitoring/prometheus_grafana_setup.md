# 📊 Prometheus & Grafana Setup (High-Level) 🌈

## <span style="color: #0ea5e9;">Scope</span>

This guide covers practical monitoring setup for Kafka, Spark, and Airflow using Prometheus + Grafana.

## <span style="color: #0ea5e9;">Prometheus Configuration</span>

- Create `prometheus.yml` with scrape jobs for:
  - Kafka brokers (`:9092/metrics` or JMX exporter)
  - Spark (JMX or Spark Prometheus sink)
  - Airflow (StatsD exporter or custom metrics plugin)

## <span style="color: #0ea5e9;">Grafana Configuration</span>

- Start Grafana:

  ```bash
  docker run -d -p 3000:3000 --name grafana grafana/grafana
  ```

- Add Prometheus as a data source.
- Import/create dashboards for Kafka throughput and lag, Spark jobs, and Airflow DAG health.

## <span style="color: #0ea5e9;">Compose Snippet (Optional)</span>

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

## <span style="color: #0ea5e9;">Alerting</span>

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

## <span style="color: #0ea5e9;">Operations Checklist</span>

- Confirm targets are up in Prometheus.
- Confirm dashboards refresh in Grafana.
- Verify critical alerts fire during controlled test failures.
- Review dashboard and alert drift during each release cycle.

## <span style="color: #0ea5e9;">Troubleshooting</span>

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
