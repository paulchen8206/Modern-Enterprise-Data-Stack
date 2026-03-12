# ⚡ Modern Data Stack Quick Start - Advanced Deployments 🎯

This guide provides fast commands for common advanced deployment operations.

## <span style="color: #0ea5e9;">Current Project Layout Notes</span>

- CI and CD workflows are split into `.github/workflows/ci.yml` and `.github/workflows/cd.yml`.
- Local Kubernetes assets are under `k8s/kind/` and operations scripts are under `ops/`.
- Docker Compose remains the default integration runtime via `infra/compose/docker-compose.yaml`.
- For full topology and component map, see `README.md` and `docs/ARCHITECTURE.md`.

## <span style="color: #0ea5e9;">Quick Command Matrix</span>

| Goal                      | Command                                                     |
| ------------------------- | ----------------------------------------------------------- |
| Start stack               | `make up`                                                   |
| Start hybrid local runtime | `make hybrid-up`                                           |
| Run Blue/Green automation | `./ops/deploy-blue-green.sh airflow v1.0.0`                 |
| Run Canary automation     | `./ops/deploy-canary.sh airflow v1.0.0`                     |
| Watch rollout             | `kubectl argo rollouts get rollout airflow-rollout --watch` |
| Promote rollout           | `kubectl argo rollouts promote airflow-rollout`             |
| Abort rollout             | `kubectl argo rollouts abort airflow-rollout`               |

## <span style="color: #0ea5e9;">Local Kubernetes (Kind) Quick Path</span>

Use this when you want to run Docker-built containers in a local Kubernetes cluster.

```bash
make kind-deploy
make kind-status
make kind-smoke
```

Use this when you want Kind plus non-conflicting Docker Compose support services (`postgres-conduktor`, `conduktor`):

```bash
make hybrid-up
make hybrid-status
```

Default host endpoints via Kind port mappings:

- Workflow API: `http://localhost:8081`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- Kafka broker: `localhost:9092`

Cleanup:

```bash
make kind-down
make hybrid-down
```

Implementation details: `k8s/kind/README.md`

## <span style="color: #0ea5e9;">Component Quick Procedures</span>

### <span style="color: #22c55e;">Orchestration (Airflow)</span>

1. Start stack: `make up`
2. Open UI: `http://localhost:8080`
3. Enable and trigger `batch_ingestion_dag` once manually
4. Confirm task logs and downstream writes before enabling schedule

Best practices:

- Keep first production run manual and observable.
- Set explicit retries and task timeouts.

### <span style="color: #22c55e;">Streaming (Kafka + Spark)</span>

1. Start producer: `make run-kafka-producer`
2. Start consumer pipeline: `make run-streaming-job`
3. Validate throughput and lag in monitoring dashboards

Best practices:

- Use stable partitioning keys.
- Make write path idempotent for retries.

### <span style="color: #22c55e;">Storage and Quality</span>

1. Verify `raw-data` and `processed-data` buckets in MinIO
2. Verify transformed records in PostgreSQL
3. Review Great Expectations validation outputs

Best practices:

- Retain immutable raw records for replay.
- Treat critical expectation failures as release blockers.

### <span style="color: #22c55e;">Governance and ML</span>

1. Register lineage payloads after successful processing
2. Create MLflow runs for each experiment change

Best practices:

- Attach dataset/version metadata to lineage and experiments.
- Keep feature definitions and model metadata versioned.

## <span style="color: #0ea5e9;">Prerequisites</span>

### <span style="color: #22c55e;">Install Required CLI Tools</span>

```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# Install helm
curl https://raw.githubusercontent.com/helm/helm/main/ops/get-helm-3 | bash

# Install AWS CLI (for AWS deployments)
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

### <span style="color: #22c55e;">Run Setup Script</span>

```bash
cd ops
chmod +x *.sh
./setup-advanced-deployments.sh
```

### <span style="color: #22c55e;">Provision Infrastructure (Optional)</span>

```bash
cd iac
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
terraform init
terraform plan
terraform apply
```

Make-based alternative:

```bash
make terraform-init
make terraform-validate
```

---

## <span style="color: #0ea5e9;">Blue/Green Deployment Quick Start</span>

### <span style="color: #22c55e;">Deploy Blue/Green Rollout</span>

```bash
# Deploy new version to preview
./ops/deploy-blue-green.sh airflow v1.0.0

# Inside the script, you can:
# - View preview endpoint
# - Test preview environment
# - Compare Blue vs Green metrics
# - Promote to production
# - Rollback to Blue
```

### <span style="color: #22c55e;">Manage Blue/Green Manually</span>

```bash
# Apply Blue/Green rollout manifest
kubectl apply -f k8s/rollout-blue-green.yaml

# Watch rollout status
kubectl argo rollouts get rollout airflow-rollout --watch

# Access preview service for validation
kubectl get svc airflow-preview-service

# Promote preview to stable
kubectl argo rollouts promote airflow-rollout

# Undo if needed
kubectl argo rollouts undo airflow-rollout
```

---

## <span style="color: #0ea5e9;">Canary Deployment Quick Start</span>

### <span style="color: #22c55e;">Deploy Canary Rollout</span>

```bash
# List all rollouts
kubectl argo rollouts list

# Get rollout status
kubectl argo rollouts get rollout airflow-rollout

# Watch rollout progress
kubectl argo rollouts get rollout airflow-rollout --watch

# Promote rollout
kubectl argo rollouts promote airflow-rollout

# Abort rollout
kubectl argo rollouts abort airflow-rollout

# Rollback to previous version
kubectl argo rollouts undo airflow-rollout

# Set new image
kubectl argo rollouts set image airflow-rollout \
  airflow-webserver=myrepo/airflow-pipeline:v2.0.0
```

### <span style="color: #22c55e;">Manage Canary Manually</span>

```bash
# Apply rollout manifests
kubectl apply -f k8s/rollout-blue-green.yaml
kubectl apply -f k8s/rollout-canary.yaml

# Get rollouts
kubectl get rollouts

# Describe rollout
kubectl describe rollout airflow-rollout

# Get analysis runs
kubectl get analysisruns

# View analysis details
kubectl describe analysisrun <analysis-run-name>

# Get pods
kubectl get pods -l app=pipeline
```

---

## <span style="color: #0ea5e9;">Monitoring Rollouts</span>

### <span style="color: #22c55e;">Check Rollout Status</span>

```bash
# Argo Rollouts Dashboard
kubectl port-forward -n argo-rollouts svc/argo-rollouts-dashboard 3100:3100
# Open: http://localhost:3100

# Grafana
kubectl port-forward -n monitoring svc/kube-prometheus-grafana 3000:80
# Open: http://localhost:3000 (admin/admin)

# Prometheus
kubectl port-forward -n monitoring svc/kube-prometheus-prometheus 9090:9090
# Open: http://localhost:9090
```

### <span style="color: #22c55e;">Inspect Metrics and Dashboards</span>

```bash
# Check success rate
kubectl exec -n monitoring prometheus-0 -- \
  wget -qO- 'http://localhost:9090/api/v1/query?query=service:http_requests:success_rate'

# Check error rate
kubectl exec -n monitoring prometheus-0 -- \
  wget -qO- 'http://localhost:9090/api/v1/query?query=service:http_requests:error_rate'

# Check latency
kubectl exec -n monitoring prometheus-0 -- \
  wget -qO- 'http://localhost:9090/api/v1/query?query=service:http_request_duration:p95'
```

---

## <span style="color: #0ea5e9;">Rollback Procedures</span>

### <span style="color: #22c55e;">Roll Back Blue/Green Deployment</span>

```bash
kubectl argo rollouts get rollout <rollout-name>
kubectl describe rollout <rollout-name>
kubectl get events --sort-by=.metadata.creationTimestamp
```

### <span style="color: #22c55e;">Abort Canary Deployment</span>

```bash
kubectl get analysisruns
kubectl describe analysisrun <analysis-run-name>
kubectl logs -n argo-rollouts deployment/argo-rollouts
```

### <span style="color: #22c55e;">Undo to Previous Revision</span>

```bash
kubectl get svc
kubectl describe svc <service-name>
kubectl get endpoints <service-name>
```

### <span style="color: #22c55e;">Emergency Full Rollback</span>

```bash
kubectl get pods -l app=pipeline
kubectl describe pod <pod-name>
kubectl logs <pod-name>
kubectl logs <pod-name> --previous  # Previous container logs
```

### <span style="color: #22c55e;">Verify Post-Rollback Health</span>

```bash
# Method 1: Using Argo Rollouts
kubectl argo rollouts abort <rollout-name>
kubectl argo rollouts undo <rollout-name>

# Method 2: Using kubectl
kubectl rollout undo rollout/<rollout-name>

# Method 3: Revert to specific revision
kubectl argo rollouts undo <rollout-name> --to-revision=2
```

---

## <span style="color: #0ea5e9;">Advanced Testing and Analysis</span>

### <span style="color: #22c55e;">Run Automated Analysis</span>

```bash
# Apply canary rollout with auto-promotion
kubectl apply -f k8s/rollout-canary.yaml

# Update image (deployment starts automatically)
kubectl argo rollouts set image airflow-canary-rollout \
  airflow-webserver=myrepo/airflow-pipeline:v2.0.0

# Watch progress (auto-promotes if analysis passes)
kubectl argo rollouts get rollout airflow-canary-rollout --watch
```

### <span style="color: #22c55e;">Execute Load Testing</span>

```bash
# Deploy blue/green with manual promotion
kubectl apply -f k8s/rollout-blue-green.yaml

# Update to new version
kubectl argo rollouts set image airflow-rollout \
  airflow-webserver=myrepo/airflow-pipeline:v2.0.0

# Test preview environment
kubectl port-forward svc/airflow-webserver-preview 8081:8080

# After testing, promote manually
kubectl argo rollouts promote airflow-rollout
```

### <span style="color: #22c55e;">Execute Chaos Testing</span>

```bash
# Apply header-based canary
kubectl apply -f k8s/rollout-canary.yaml

# Internal users get canary version
curl -H "X-Version: canary" http://your-service.com

# Regular users get stable version
curl http://your-service.com
```

### <span style="color: #22c55e;">Capture Performance Benchmarks</span>

```bash
# Immediate abort
kubectl argo rollouts abort airflow-rollout

# Rollback to previous stable version
kubectl argo rollouts undo airflow-rollout

# Verify rollback
kubectl argo rollouts get rollout airflow-rollout
```

---

## <span style="color: #0ea5e9;">Configuration Reference</span>

### <span style="color: #22c55e;">Key Configuration Files</span>

| File                          | Purpose                           |
| ----------------------------- | --------------------------------- |
| `k8s/rollout-blue-green.yaml` | Blue/Green rollout definitions    |
| `k8s/rollout-canary.yaml`     | Canary rollout definitions        |
| `k8s/analysis-templates.yaml` | Prometheus metrics analysis       |
| `k8s/services.yaml`           | Kubernetes services               |
| `k8s/ingress.yaml`            | Traffic routing rules             |
| `ops/deploy-blue-green.sh`    | Interactive blue/green deployment |
| `ops/deploy-canary.sh`        | Interactive canary deployment     |
| `ops/setup-advanced-deployments.sh` | Infrastructure setup       |

### <span style="color: #22c55e;">Tune Analysis Thresholds</span>

Edit `k8s/analysis-templates.yaml`:

```yaml
# Example: Change success rate threshold
metrics:
  - name: success-rate
    successCondition: result >= 0.99 # Change from 0.95 to 0.99
    failureLimit: 2 # Reduce from 3 to 2
```

### <span style="color: #22c55e;">Adjust Canary Weights</span>

Edit `k8s/rollout-canary.yaml`:

```yaml
steps:
  - setWeight: 5 # Start with 5% instead of 10%
  - pause: { duration: 1m }
  - setWeight: 15 # Then 15%
  - pause: { duration: 2m }
  # ... customize as needed
```

---

## <span style="color: #0ea5e9;">Troubleshooting</span>

### <span style="color: #22c55e;">Rollout Stuck Issues</span>

```bash
# Check Argo Rollouts
kubectl get pods -n argo-rollouts

# Check Prometheus
kubectl get pods -n monitoring

# Check Ingress Controller
kubectl get pods -n ingress-nginx

# Verify all components
kubectl get all -n argo-rollouts
kubectl get all -n monitoring
```

### <span style="color: #22c55e;">Analysis Failure Debugging</span>

```bash
# Test Prometheus
kubectl exec -n monitoring prometheus-0 -- wget -qO- http://localhost:9090/-/healthy

# Test Grafana
kubectl exec -n monitoring deployment/kube-prometheus-grafana -- wget -qO- http://localhost:3000/api/health

# Test Argo Rollouts
kubectl exec -n argo-rollouts deployment/argo-rollouts -- wget -qO- http://localhost:8080/healthz
```

---

## <span style="color: #0ea5e9;">Best Practices</span>

### <span style="color: #22c55e;">Traffic Strategy Guidance</span>

For high-traffic services:

- Start with smaller weights (5%)
- Use shorter soak times (1-2 minutes)
- More granular steps (5%, 10%, 20%, 30%, 50%, 100%)

For critical services:

- Larger initial weight (20%)
- Longer soak times (5-10 minutes)
- Fewer steps (20%, 50%, 100%)

### <span style="color: #22c55e;">Reliability and Availability Settings</span>

```yaml
# Fast-moving services
interval: 15s
count: 4        # 1 minute total

# Stable services
interval: 60s
count: 10       # 10 minutes total
```

---

## <span style="color: #0ea5e9;">Next Steps</span>

1. Review [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed documentation
2. Customize analysis templates for your metrics
3. Configure Slack notifications
4. Set up custom dashboards in Grafana
5. Practice rollback procedures
6. Configure automated testing in CI/CD

---

## <span style="color: #0ea5e9;">Resources</span>

- **Argo Rollouts Docs**: https://argoproj.github.io/argo-rollouts/
- **Prometheus Docs**: https://prometheus.io/docs/
- **Grafana Docs**: https://grafana.com/docs/
- **Kubectl Cheatsheet**: https://kubernetes.io/docs/reference/kubectl/cheatsheet/

---

**Last Updated**: 2026-03-11
