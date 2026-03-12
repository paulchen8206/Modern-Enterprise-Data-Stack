#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/infra/compose/docker-compose.yaml"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Please install it first."
  exit 1
fi

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose --project-directory "$REPO_ROOT" -f "$COMPOSE_FILE")
elif docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose --project-directory "$REPO_ROOT" -f "$COMPOSE_FILE")
else
  echo "Docker Compose is required. Install docker-compose or enable 'docker compose'."
  exit 1
fi

echo "Pulling Docker images..."
images=(
  confluentinc/cp-zookeeper:7.3.2
  confluentinc/cp-kafka:7.3.2
  mysql:8.0
  postgres:14
  minio/minio:latest
)

for image in "${images[@]}"; do
  docker pull "$image"
done

echo "Starting the data pipeline..."
"${COMPOSE_CMD[@]}" up -d --build

echo "Checking container status..."
"${COMPOSE_CMD[@]}" ps

echo "Data pipeline setup completed successfully."
