#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="data-stack-local"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd kubectl
require_cmd curl

echo "Checking deployment rollouts in namespace: $NAMESPACE"
kubectl -n "$NAMESPACE" rollout status deployment/zookeeper --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/kafka --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/mysql --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/postgres --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/minio --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/airflow-webserver --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/airflow-scheduler --timeout=120s
kubectl -n "$NAMESPACE" rollout status deployment/workflow-api --timeout=120s

echo
kubectl -n "$NAMESPACE" get pods,svc

echo
echo "Endpoint checks"
curl -fsS --max-time 10 http://localhost:8081/actuator/health >/dev/null
curl -fsS --max-time 10 http://localhost:8080/health >/dev/null
curl -fsS --max-time 10 http://localhost:9000/minio/health/live >/dev/null

echo "Smoke checks passed."
