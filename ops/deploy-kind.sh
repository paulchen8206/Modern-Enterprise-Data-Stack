#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="modern-data-stack"
KIND_CONFIG="k8s/kind/cluster-config.yaml"
KIND_STACK="k8s/kind/stack.yaml"
NAMESPACE="data-stack-local"
APP_IMAGE="modern-enterprise-data-stack-workflow-api:kind"
AIRFLOW_IMAGE="modern-enterprise-data-stack-airflow:kind"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd kind
require_cmd kubectl
require_cmd docker

if ! kind get clusters | grep -qx "$CLUSTER_NAME"; then
  echo "Creating kind cluster: $CLUSTER_NAME"
  kind create cluster --config "$KIND_CONFIG"
else
  echo "Kind cluster already exists: $CLUSTER_NAME"
fi

echo "Building workflow-api image for kind..."
docker build -t "$APP_IMAGE" -f infra/dockerfiles/workflow-api.Dockerfile .

echo "Building airflow image for kind..."
docker build -t "$AIRFLOW_IMAGE" -f infra/dockerfiles/airflow.Dockerfile pipelines/airflow

echo "Loading image into kind cluster..."
kind load docker-image "$APP_IMAGE" --name "$CLUSTER_NAME"
kind load docker-image "$AIRFLOW_IMAGE" --name "$CLUSTER_NAME"

echo "Applying Kubernetes manifests..."
kubectl apply -f "$KIND_STACK"

echo "Waiting for deployments to become ready..."
kubectl -n "$NAMESPACE" rollout status deployment/zookeeper --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/kafka --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/mysql --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/postgres --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/minio --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/airflow-webserver --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/airflow-scheduler --timeout=240s
kubectl -n "$NAMESPACE" rollout status deployment/workflow-api --timeout=240s

echo
kubectl -n "$NAMESPACE" get pods -o wide
echo
echo "Local endpoints (through kind extraPortMappings):"
echo "- Airflow UI:     http://localhost:8080"
echo "- Workflow API:  http://localhost:8081/actuator/health"
echo "- MinIO API:      http://localhost:9000"
echo "- MinIO Console:  http://localhost:9001"
echo "- Kafka broker:   localhost:9092"
