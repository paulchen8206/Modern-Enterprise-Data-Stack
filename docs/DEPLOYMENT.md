# 🚢 Advanced Deployment Strategies Guide 🌟

This guide covers advanced deployment strategies for the Modern Data Stack, including Blue/Green, Canary, and progressive delivery patterns.

## <span style="color: #0ea5e9;">Current Implementation Baseline</span>

- CI/CD automation is represented by `.github/workflows/ci.yml` and `.github/workflows/cd.yml`.
- Local Kubernetes validation is implemented under `k8s/kind/` with automation scripts in `ops/deploy-kind.sh` and `ops/kind-smoke.sh`.
- Compose-based local runtime remains available under `infra/compose/docker-compose.yaml` for service-level integration tests before rollout.

## <span style="color: #0ea5e9;">Table of Contents</span>

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Installation](#installation)
5. [Deployment Strategies](#deployment-strategies)
6. [Monitoring & Observability](#monitoring--observability)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

---

## <span style="color: #0ea5e9;">Overview</span>

| Scenario                       | Recommended Strategy   | Why                                           |
| ------------------------------ | ---------------------- | --------------------------------------------- |
| Low-risk routine release       | Blue/Green             | Fast cutover and immediate rollback           |
| High-risk or uncertain release | Canary                 | Progressive traffic shift with analysis gates |
| Mission-critical API           | Canary + auto rollback | Reduces blast radius for regressions          |
| Major platform migration       | Blue/Green + soak test | Parallel validation before full switch        |

### <span style="color: #22c55e;">What Is Included</span>

This project now includes enterprise-grade deployment capabilities:

- **Blue/Green Deployments**: Zero-downtime deployments with instant rollback capability
- **Canary Deployments**: Progressive traffic shifting with automated analysis
- **Automated Analysis**: Prometheus-based metrics analysis during deployments
- **Traffic Management**: Advanced routing with AWS ALB and Nginx Ingress
- **Monitoring**: Real-time deployment metrics and dashboards
- **GitOps Integration**: Argo CD with Argo Rollouts for continuous delivery

### <span style="color: #22c55e;">Key Benefits</span>

- **Zero Downtime**: All deployment strategies ensure no service interruption
- **Risk Mitigation**: Progressive rollouts and automated rollbacks reduce deployment risk
- **Automated Quality Gates**: Analysis templates validate deployments before promotion
- **Visibility**: Comprehensive monitoring and alerting for deployment health
- **Speed**: Faster rollbacks and controlled traffic shifting

---

## <span style="color: #0ea5e9;">Architecture</span>

### <span style="color: #22c55e;">System Architecture Diagram</span>

```mermaid
graph TB
    subgraph TM["Traffic Management"]
        ALB[AWS ALB]
        NI[Nginx Ingress]
        SM[Service Mesh]
    end

    subgraph DE["Deployment Engine"]
        subgraph AR["Argo Rollouts"]
            BG[Blue/Green Strategy]
            CS[Canary Strategy]
        end
    end

    subgraph AM["Analysis & Metrics"]
        PROM[Prometheus]
        GRAF[Grafana]
        ALERT[AlertManager]
    end

    TM --> DE
    DE --> AM
```

### <span style="color: #22c55e;">Local Runtime Configuration Diagrams</span>

#### Pure Docker Compose (All Services in Compose)

```mermaid
graph LR
  Host[Host Machine] --> API[workflow-api container]
  Host --> AF[airflow-webserver]
  Host --> K[kafka]
  Host --> M[minio]
  Host --> PG[postgres]
  Host --> MY[mysql]
  Host --> CDK[conduktor]

  API --> AF
  API --> K
  API --> M
  API --> PG
  API --> MY
```

#### Kind-Only Local Kubernetes

```mermaid
graph LR
  Host[Host Machine] --> Kind[Kind Cluster]

  subgraph Kind
    K8sAPI[workflow-api pod]
    K8sAF[airflow-webserver pod]
    K8sK[kafka pod]
    K8sM[minio pod]
    K8sPG[postgres pod]
    K8sMY[mysql pod]
  end

  K8sAPI --> K8sAF
  K8sAPI --> K8sK
  K8sAPI --> K8sM
  K8sAPI --> K8sPG
  K8sAPI --> K8sMY
```

#### Hybrid (Kind App Stack + Compose Support Services)

```mermaid
graph LR
  Host[Host Machine] --> Kind[Kind Cluster]
  Host --> CPG[postgres-conduktor container]
  Host --> CUI[conduktor container]

  subgraph Kind
    HAPI[workflow-api pod]
    HAF[airflow-webserver pod]
    HK[kafka pod]
    HM[minio pod]
    HPG[postgres pod]
    HMY[mysql pod]
  end

  CUI --> CPG
  CUI --> HK
  HAPI --> HAF
  HAPI --> HK
  HAPI --> HM
  HAPI --> HPG
  HAPI --> HMY
```

#### Java API Runtime Options

```mermaid
graph TD
  A[Java API Host Run\nmake run-java-api-local-safe] --> B[Connect to Compose or Kind endpoints]
  C[Java API Container Run\nmake run-java-api-container] --> D[Compose profile inside workflow-api container]

  B --> E[Best for fast code iteration]
  D --> F[Best for container parity testing]
```

### <span style="color: #22c55e;">Deployment Flow Diagrams</span>

#### Blue/Green Deployment

```mermaid
graph LR
    A[1. Deploy Green<br/>Preview] --> B[2. Run Tests &<br/>Analysis]
    B --> C[3. Manual/Auto<br/>Verification]
    C --> D[4. Switch Traffic<br/>Blue → Green]
    D --> E[5. Keep Blue for<br/>Instant Rollback]
    E --> F[6. Decommission<br/>Blue]
```

#### Canary Deployment

```mermaid
graph TD
    A[1. Deploy Canary] --> B[2. Route 10%<br/>Traffic to Canary]
    B --> C{3. Analysis Pass?}
    C -->|Yes| D[4. Increase to 25%]
    C -->|No| Z[Auto-Rollback]
    D --> E{Analysis Pass?}
    E -->|Yes| F[5. Increase to 50%]
    E -->|No| Z
    F --> G{Analysis Pass?}
    G -->|Yes| H[6. Increase to 75%]
    G -->|No| Z
    H --> I{Analysis Pass?}
    I -->|Yes| J[7. Promote to 100%]
    I -->|No| Z
```

---

## <span style="color: #0ea5e9;">Prerequisites</span>

### <span style="color: #22c55e;">Required Tools</span>

- **kubectl** (v1.24+)
- **helm** (v3.10+)
- **AWS CLI** (v2.0+) - for AWS deployments
- **eksctl** (optional) - for EKS management
- **jq** (optional) - for JSON parsing

### <span style="color: #22c55e;">Infrastructure Requirements</span>

- **Kubernetes**: v1.24+
- **Minimum Nodes**: 3
- **Node Size**: t3.medium or larger
- **Storage**: Support for dynamic PVC provisioning

### <span style="color: #22c55e;">AWS Components (Optional)</span>

- EKS Cluster
- VPC with public/private subnets
- IAM roles for service accounts
- ACM certificate (for HTTPS)

---

## <span style="color: #0ea5e9;">Installation</span>

### <span style="color: #22c55e;">Quick Installation Commands</span>

```bash
# 1. Navigate to ops directory
cd ops

# 2. Make scripts executable
chmod +x *.sh

# 3. Run setup script
./setup.sh

# 4. Verify installation
kubectl get pods -n argo-rollouts
kubectl get pods -n monitoring
```

### <span style="color: #22c55e;">Detailed Installation Steps</span>

#### 1. Install Argo Rollouts

```bash
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f \
  https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml

# Install kubectl plugin
curl -LO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
chmod +x kubectl-argo-rollouts-linux-amd64
sudo mv kubectl-argo-rollouts-linux-amd64 /usr/local/bin/kubectl-argo-rollouts
```

#### 2. Install Prometheus & Grafana

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install kube-prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false
```

#### 3. Install Nginx Ingress (Optional)

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace
```

#### 4. Apply Kubernetes Manifests

```bash
cd k8s

# Apply services
kubectl apply -f services.yaml

# Apply analysis templates
kubectl apply -f analysis-templates.yaml

# Apply ServiceMonitors
kubectl apply -f servicemonitors.yaml

# Apply Ingress
kubectl apply -f ingress.yaml
```

---

## <span style="color: #0ea5e9;">Deployment Strategies</span>

### <span style="color: #22c55e;">Blue/Green Deployment</span>

#### Deploy with Script

```bash
cd ops
./deploy-blue-green.sh airflow v1.0.0
```

#### Manual Deployment

```bash
# Apply rollout manifest
kubectl apply -f k8s/rollout-blue-green.yaml

# Update image
kubectl argo rollouts set image airflow-rollout \
  airflow-webserver=myrepo/airflow-pipeline:v1.0.0

# Check status
kubectl argo rollouts get rollout airflow-rollout --watch

# Promote to production
kubectl argo rollouts promote airflow-rollout

# Rollback if needed
kubectl argo rollouts abort airflow-rollout
kubectl argo rollouts undo airflow-rollout
```

#### Access Preview Environment

```bash
# Port-forward to preview service
kubectl port-forward svc/airflow-webserver-preview 8081:8080

# Test preview
curl http://localhost:8081/health
```

### <span style="color: #22c55e;">Canary Deployment</span>

#### Deploy with Script

```bash
cd ops
./deploy-canary.sh airflow v1.0.0
```

#### Manual Deployment

```bash
# Apply rollout manifest
kubectl apply -f k8s/rollout-canary.yaml

# Update image (triggers canary)
kubectl argo rollouts set image airflow-canary-rollout \
  airflow-webserver=myrepo/airflow-pipeline:v1.0.0

# Monitor progress
kubectl argo rollouts get rollout airflow-canary-rollout --watch

# Manual promotion (if not using auto-promotion)
kubectl argo rollouts promote airflow-canary-rollout

# Abort and rollback
kubectl argo rollouts abort airflow-canary-rollout
```

#### Canary Traffic Weights

The default canary strategy uses progressive traffic shifting:

- **Step 1**: 10% for 2 minutes
- **Step 2**: 25% for 3 minutes
- **Step 3**: 50% for 5 minutes
- **Step 4**: 75% for 3 minutes
- **Step 5**: 100% (full promotion)

Each step includes automated analysis. Deployment auto-aborts on analysis failure.

### <span style="color: #22c55e;">Rollout Management Commands</span>

```bash
# Apply header-based canary
kubectl apply -f k8s/rollout-canary.yaml

# Access canary version with header
curl -H "X-Version: canary" http://your-domain.com

# Regular traffic goes to stable version
curl http://your-domain.com
```

---

## <span style="color: #0ea5e9;">Monitoring & Observability</span>

### <span style="color: #22c55e;">Dashboards and UIs</span>

#### Argo Rollouts Dashboard

```bash
kubectl port-forward -n argo-rollouts svc/argo-rollouts-dashboard 3100:3100
# Open: http://localhost:3100
```

#### Grafana

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-grafana 3000:80
# Open: http://localhost:3000
# Username: admin
# Password: admin
```

#### Prometheus

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-prometheus 9090:9090
# Open: http://localhost:9090
```

### <span style="color: #22c55e;">Prometheus Alerts and Rules</span>

```bash
# The dashboard is located at: monitoring/grafana-deployment-dashboards.json

# Import via UI:
# 1. Login to Grafana
# 2. Click "+" → Import
# 3. Upload the JSON file
# 4. Select Prometheus data source
# 5. Click Import
```

### <span style="color: #22c55e;">Key Metrics to Track</span>

#### Success Rate

```promql
sum(rate(http_requests_total{status=~"2.."}[5m])) /
sum(rate(http_requests_total[5m]))
```

#### Error Rate

```promql
sum(rate(http_requests_total{status=~"5.."}[5m])) /
sum(rate(http_requests_total[5m]))
```

#### P95 Latency

```promql
histogram_quantile(0.95,
  sum(rate(http_request_duration_milliseconds_bucket[5m])) by (le)
)
```

### <span style="color: #22c55e;">Recommended Alert Policies</span>

Alerts are automatically configured for:

- High error rate (>5%)
- High latency (P95 >1000ms)
- Rollout degraded
- Analysis run failed
- Airflow task failures
- Kafka consumer lag
- Spark job failures

Configure Slack notifications in `k8s/argo-rollouts-install.yaml`.

---

## <span style="color: #0ea5e9;">Troubleshooting</span>

### <span style="color: #22c55e;">Rollout Issues</span>

```bash
# Check rollout status
kubectl argo rollouts get rollout <rollout-name>

# Check analysis runs
kubectl get analysisruns

# View analysis details
kubectl describe analysisrun <analysis-run-name>

# Check pod status
kubectl get pods -l app=pipeline

# View pod logs
kubectl logs <pod-name>
```

### <span style="color: #22c55e;">Monitoring Issues</span>

```bash
# Check Prometheus connectivity
kubectl exec -it <rollout-pod> -- curl http://prometheus:9090/-/healthy

# Verify metrics exist
kubectl port-forward svc/prometheus 9090:9090
# Open Prometheus UI and run query

# Check ServiceMonitors
kubectl get servicemonitors

# View analysis template
kubectl get analysistemplate <template-name> -o yaml
```

### <span style="color: #22c55e;">Ingress and Networking Issues</span>

```bash
# Check services
kubectl get svc

# Check ingress
kubectl get ingress
kubectl describe ingress <ingress-name>

# Verify canary service selector
kubectl get svc <canary-service> -o yaml

# Check pod labels
kubectl get pods --show-labels
```

### <span style="color: #22c55e;">Rollback Issues</span>

```bash
# Abort current rollout
kubectl argo rollouts abort <rollout-name>

# Undo to previous version
kubectl argo rollouts undo <rollout-name>

# Undo to specific revision
kubectl argo rollouts undo <rollout-name> --to-revision=2
```

---

## <span style="color: #0ea5e9;">Best Practices</span>

### <span style="color: #22c55e;">Strategy Selection Guide</span>

| Scenario                    | Recommended Strategy       |
| --------------------------- | -------------------------- |
| Critical production service | Blue/Green                 |
| High-traffic service        | Canary                     |
| Testing new features        | Canary with header routing |
| Database migrations         | Blue/Green                 |
| Quick iterations            | Canary                     |
| Stateful applications       | Blue/Green                 |

### <span style="color: #22c55e;">Analysis Configuration Tips</span>

1. **Set appropriate thresholds**: Don't make them too strict or too loose
2. **Use multiple metrics**: Success rate + latency + errors
3. **Set failure limits**: Allow 2-3 failures before aborting
4. **Adjust intervals**: 30s for high-traffic, 60s for low-traffic

### <span style="color: #22c55e;">Canary Rollout Tips</span>

1. **Start with small percentages**: 10% for initial canary
2. **Increase gradually**: 10% → 25% → 50% → 75% → 100%
3. **Allow soak time**: Minimum 2-5 minutes per step
4. **Monitor continuously**: Watch metrics during traffic shifts

### <span style="color: #22c55e;">Rollback Best Practices</span>

1. **Set auto-rollback**: Configure analysis to auto-abort on failures
2. **Keep blue/green environments**: Maintain for quick rollback
3. **Test rollback procedures**: Practice rollbacks regularly
4. **Document rollback playbooks**: Clear steps for emergencies

### <span style="color: #22c55e;">Security and Access Control</span>

1. **Use RBAC**: Limit who can promote deployments
2. **Require approvals**: Manual gates for production
3. **Audit trails**: Track all deployment actions
4. **Network policies**: Isolate preview environments

---

## <span style="color: #0ea5e9;">Advanced Configurations</span>

### <span style="color: #22c55e;">A/B and Experimentation Support</span>

Test multiple versions simultaneously:

```yaml
experiments:
  - name: version-comparison
    templates:
      - name: v1
        specRef: stable
        replicas: 2
      - name: v2
        specRef: canary
        replicas: 2
    duration: 10m
```

### <span style="color: #22c55e;">External Integrations</span>

Beyond Prometheus, support for:

- Datadog
- New Relic
- CloudWatch
- Custom webhooks

### <span style="color: #22c55e;">Notification Channels</span>

Configure in `k8s/argo-rollouts-install.yaml`:

- Slack
- Email
- PagerDuty
- Webhooks

---

## <span style="color: #0ea5e9;">Integration with Existing Infrastructure</span>

### <span style="color: #22c55e;">Terraform Integration</span>

```bash
cd iac

# Initialize
terraform init

# Plan
terraform plan

# Apply
terraform apply

# Outputs
terraform output
```

### <span style="color: #22c55e;">Infrastructure Mapping</span>

- **EKS Cluster**: `eks.tf`
- **Load Balancer Controller**: `load-balancer-controller.tf`
- **Argo Rollouts**: `argo-rollouts.tf`
- **Networking**: `security.tf`

---

## <span style="color: #0ea5e9;">Additional Resources</span>

### <span style="color: #22c55e;">Reference Documentation</span>

- [Argo Rollouts](https://argoproj.github.io/argo-rollouts/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [Prometheus Operator](https://prometheus-operator.dev/)
- [Grafana](https://grafana.com/docs/)

### <span style="color: #22c55e;">Important Project Files</span>

| File                              | Purpose                         |
| --------------------------------- | ------------------------------- |
| `k8s/rollout-blue-green.yaml`     | Blue/Green deployment manifests |
| `k8s/rollout-canary.yaml`         | Canary deployment manifests     |
| `k8s/analysis-templates.yaml`     | Prometheus analysis templates   |
| `k8s/services.yaml`               | Kubernetes services             |
| `k8s/ingress.yaml`                | Ingress configurations          |
| `k8s/servicemonitors.yaml`        | Prometheus ServiceMonitors      |
| `ops/deploy-blue-green.sh`        | Blue/Green deployment script    |
| `ops/deploy-canary.sh`            | Canary deployment script        |
| `ops/setup.sh`                    | Infrastructure setup            |
| `iac/load-balancer-controller.tf` | AWS LB Controller               |
| `iac/argo-rollouts.tf`            | Argo Rollouts infrastructure    |

---

## <span style="color: #0ea5e9;">Conclusion</span>

For issues or questions:

1. Check the troubleshooting section
2. Review Argo Rollouts documentation
3. Check pod logs and events
4. Verify Prometheus metrics

---

**Last Updated**: 2026-03-11
**Version**: 1.0.0
