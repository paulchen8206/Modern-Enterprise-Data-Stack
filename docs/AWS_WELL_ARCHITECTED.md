# AWS Well-Architected Framework Reference

This reference maps AWS Well-Architected best practices to the project design and operating model used in this repository.

Primary sources:

- AWS Well-Architected Framework: https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html
- AWS Well-Architected Tool: https://aws.amazon.com/well-architected-tool/

## Design Mapping By Pillar

### Operational Excellence

- Infrastructure as code under `iac/` supports repeatable provisioning and reviewable change history.
- Standard operations via `Makefile` commands and scripts in `ops/`.
- Deployment automation with progressive delivery patterns (blue/green and canary).

Best-practice focus:

- Keep runbooks current and test key operational playbooks each release cycle.
- Capture incident learnings and feed improvements back into docs and automation.

### Security

- Security controls and cloud resources are declared in Terraform.
- Environment separation and promotion flow reduce uncontrolled production changes.
- Version-controlled manifests and CI checks support secure-by-default delivery.

Best-practice focus:

- Apply least-privilege IAM for all services and automation identities.
- Encrypt data in transit and at rest across storage tiers.
- Manage secrets via a dedicated secrets manager rather than static config.

### Reliability

- Multi-environment rollout flow (`dev`, `qa`, `stg`, `prd`) supports staged verification.
- Argo Rollouts patterns provide safer release and rollback paths.
- Monitoring stack (Prometheus/Grafana) enables health visibility and alerting.

Best-practice focus:

- Define SLOs for ingest, transformation latency, and data freshness.
- Run periodic rollback and failure scenario drills.

### Performance Efficiency

- The platform supports both batch and streaming data paths.
- Spark and Kafka are used for scalable data processing.
- Storage patterns include object storage and analytic table format support.

Best-practice focus:

- Benchmark and tune Spark/Kafka settings per workload profile.
- Right-size resources and use autoscaling where supported.

### Cost Optimization

- Environment overlays in Helm values support right-sized defaults by stage.
- Local-first runtime options (Compose/Kind) help reduce cloud iteration cost.
- Terraform enables consistent tagging and budget guardrail automation.

Best-practice focus:

- Apply mandatory cost allocation tags for all cloud resources.
- Review idle/non-production resources and automate shutdown windows.

### Sustainability

- Repeatable automation reduces rework and unnecessary environment churn.
- Environment-specific sizing supports efficient resource use.

Best-practice focus:

- Track utilization and periodically rebalance compute and storage tiers.
- Prefer efficient managed services when moving workloads to AWS.

## How To Use This In Design Reviews

- Use this page as a checklist during architecture and release readiness reviews.
- Pair with `docs/ARCHITECTURE.md` for system design and `docs/DEPLOYMENT.md` for rollout mechanics.
- Convert identified gaps into tracked backlog items and review quarterly.
