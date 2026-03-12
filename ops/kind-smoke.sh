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

check_http_status() {
  local url="$1"
  local expected="$2"
  local code
  code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 10 "$url")
  if [[ " $expected " != *" $code "* ]]; then
    echo "Unexpected HTTP status $code for $url (expected one of: $expected)" >&2
    return 1
  fi
}

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
check_http_status "http://localhost:8081/actuator/health" "200"
# Airflow endpoint may require auth depending on active auth backend; 401/403 still confirms routing is alive.
check_http_status "http://localhost:8080/api/v1/health" "200 401 403"
check_http_status "http://localhost:9000/minio/health/live" "200"

echo "Smoke checks passed."
