SHELL := /bin/bash

PYTHON ?= $(shell [ -x .venv/bin/python ] && echo .venv/bin/python || echo python3)
COMPOSE_MAIN ?= docker-compose --project-directory . -f infra/compose/docker-compose.yaml
COMPOSE_CI ?= docker-compose --project-directory . -f infra/compose/docker-compose.ci.yaml
TERRAFORM ?= terraform
PRETTIER ?= prettier
WIKI_PORT ?= 8000
KIND ?= kind
KUBECTL ?= kubectl
KIND_CLUSTER ?= modern-data-stack
KIND_NAMESPACE ?= data-stack-local
HYBRID_SUPPORT_SERVICES ?= postgres-conduktor conduktor
PYTHON_SRC ?= pipelines/airflow pipelines/bi_dashboards pipelines/governance pipelines/great_expectations pipelines/kafka pipelines/ml pipelines/monitoring pipelines/spark pipelines/storage devtools/serve_wiki.py
TEXT_FILE_TYPES ?= \( -name '*.md' -o -name '*.yaml' -o -name '*.yml' -o -name '*.json' -o -name '*.js' -o -name '*.css' -o -name '*.html' \)
YAML_FILE_TYPES ?= \( -name '*.yml' -o -name '*.yaml' \)
COMMON_EXCLUDES ?= ! -path './.venv/*' ! -path './.git/*' ! -path './java-api/target/*'

.PHONY: help up down restart ps logs clean \
	format format-python format-text terraform-fmt \
	validate validate-compose validate-shell validate-python validate-json validate-yaml validate-notebook validate-format validate-terraform \
	terraform-init terraform-validate \
	run-kafka-producer run-streaming-job run-batch-job run-iceberg-demo prepare-demo-data \
	run-java-api-local run-java-api-compose run-java-api-local-safe \
	build-java-api-container run-java-api-container stop-java-api-container logs-java-api-container \
	run-wiki \
	kind-up kind-deploy kind-status kind-smoke kind-down \
	hybrid-up hybrid-status hybrid-down

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*##"; print "Available targets:"} /^[a-zA-Z0-9_.-]+:.*##/ {printf "  %-26s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

up: ## Start full platform with Docker Compose
	$(COMPOSE_MAIN) up --build -d

down: ## Stop platform containers
	$(COMPOSE_MAIN) down

restart: ## Restart platform containers
	$(COMPOSE_MAIN) restart

ps: ## List platform container status
	$(COMPOSE_MAIN) ps

logs: ## Tail logs for core services
	$(COMPOSE_MAIN) logs --tail=200 airflow-webserver spark kafka postgres mysql

clean: ## Stop containers and remove volumes
	$(COMPOSE_MAIN) down -v

format: format-python format-text terraform-fmt ## Run all formatters

format-python: ## Format Python source files with Black
	$(PYTHON) -m black $(PYTHON_SRC)

format-text: ## Format markdown/yaml/json/js/css/html with Prettier
	find . -type f $(TEXT_FILE_TYPES) \
		$(COMMON_EXCLUDES) \
		-print0 | xargs -0 $(PRETTIER) --write

terraform-fmt: ## Format Terraform files
	$(TERRAFORM) -chdir=iac fmt -recursive

validate: validate-compose validate-shell validate-python validate-json validate-yaml validate-notebook validate-format validate-terraform ## Run full project validation

validate-compose: ## Validate docker-compose configuration files
	$(COMPOSE_MAIN) config >/dev/null
	$(COMPOSE_CI) config >/dev/null

validate-shell: ## Validate shell script syntax
	find . -type f -name '*.sh' $(COMMON_EXCLUDES) -print0 | xargs -0 -n1 bash -n

validate-python: ## Validate Python syntax
	$(PYTHON) -m compileall $(PYTHON_SRC)

validate-json: ## Validate JSON files
	@find . -type f -name '*.json' \
		$(COMMON_EXCLUDES) \
		-print0 | xargs -0 -n1 $(PYTHON) -m json.tool >/dev/null
	@echo "JSON validation passed."

validate-yaml: ## Validate YAML files
	@find . -type f $(YAML_FILE_TYPES) \
		$(COMMON_EXCLUDES) \
		-print0 | xargs -0 -n1 $(PYTHON) -c "import sys,yaml; list(yaml.safe_load_all(open(sys.argv[1], encoding='utf-8')))"
	@echo "YAML validation passed."

validate-notebook: ## Validate notebook schema
	@$(PYTHON) -c "import nbformat; nbformat.read(open('notebooks/modern-data-stack.ipynb','r',encoding='utf-8'), as_version=4); print('Notebook schema validation passed.')"

validate-format: ## Check markdown/yaml/json/js/css/html formatting
	find . -type f $(TEXT_FILE_TYPES) \
		$(COMMON_EXCLUDES) \
		-print0 | xargs -0 $(PRETTIER) --check

validate-terraform: ## Validate Terraform formatting and configuration
	$(TERRAFORM) -chdir=iac fmt -check -recursive
	$(TERRAFORM) -chdir=iac init -backend=false -input=false >/dev/null
	$(TERRAFORM) -chdir=iac validate

terraform-init: ## Initialize Terraform providers/modules
	$(TERRAFORM) -chdir=iac init -backend=false -input=false

terraform-validate: ## Validate Terraform only
	$(TERRAFORM) -chdir=iac init -backend=false -input=false >/dev/null
	$(TERRAFORM) -chdir=iac validate

run-java-api-local: ## Run Java API from repo root using local profile
	mvn -f java-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local -DskipTests

run-java-api-compose: ## Run Java API from repo root using compose profile
	mvn -f java-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=compose -DskipTests

run-java-api-local-safe: ## Stop process on 8081, then run Java API local profile
	@if lsof -ti tcp:8081 >/dev/null 2>&1; then \
		echo "Stopping process on port 8081..."; \
		kill $$(lsof -ti tcp:8081); \
		sleep 1; \
	fi
	mvn -f java-api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local -DskipTests

build-java-api-container: ## Build Java API container image
	$(COMPOSE_MAIN) build workflow-api

run-java-api-container: ## Run Java API as Docker Compose service on port 8081
	@for pid in $$(lsof -ti tcp:8081 2>/dev/null); do \
		cmd=$$(ps -p $$pid -o comm= | tr -d ' '); \
		if [ "$$cmd" = "java" ]; then \
			echo "Stopping Java process on port 8081 (pid=$$pid)..."; \
			kill $$pid; \
			sleep 1; \
		fi; \
	done
	$(COMPOSE_MAIN) up -d --force-recreate workflow-api

stop-java-api-container: ## Stop Java API Docker Compose service
	$(COMPOSE_MAIN) stop workflow-api

logs-java-api-container: ## Tail Java API container logs
	$(COMPOSE_MAIN) logs --tail=200 -f workflow-api

run-wiki: ## Run local wiki server (override port: make run-wiki WIKI_PORT=3000)
	$(PYTHON) devtools/serve_wiki.py $(WIKI_PORT)

kind-up: ## Create local kind cluster for Kubernetes deployment
	$(KIND) create cluster --name $(KIND_CLUSTER) --config k8s/kind/cluster-config.yaml

kind-deploy: ## Deploy local stack into kind (build/load/apply/wait)
	./ops/deploy-kind.sh

kind-status: ## Show status of pods/services in local kind namespace
	$(KUBECTL) -n $(KIND_NAMESPACE) get pods,svc

kind-smoke: ## Run local smoke checks for kind deployment
	./ops/kind-smoke.sh

kind-down: ## Delete local kind cluster
	$(KIND) delete cluster --name $(KIND_CLUSTER)

hybrid-up: ## Start compose support services and deploy local kind stack
	$(COMPOSE_MAIN) up -d --no-deps $(HYBRID_SUPPORT_SERVICES)
	./ops/deploy-kind.sh

hybrid-status: ## Show compose support services and kind namespace status
	$(COMPOSE_MAIN) ps $(HYBRID_SUPPORT_SERVICES)
	$(KUBECTL) -n $(KIND_NAMESPACE) get pods,svc

hybrid-down: ## Stop hybrid compose support services and delete local kind cluster
	$(COMPOSE_MAIN) stop $(HYBRID_SUPPORT_SERVICES)
	$(COMPOSE_MAIN) rm -f $(HYBRID_SUPPORT_SERVICES)
	$(KIND) delete cluster --name $(KIND_CLUSTER)

run-kafka-producer: ## Run Kafka producer in compose stack
	$(COMPOSE_MAIN) exec -e KAFKA_TOPIC=events spark python3 /opt/kafka_jobs/producer.py

run-streaming-job: ## Run Spark streaming job in compose stack
	$(COMPOSE_MAIN) exec -T spark /opt/spark/bin/spark-submit --master local[2] /opt/spark_jobs/spark_streaming_job.py

run-batch-job: prepare-demo-data ## Run Spark batch job in compose stack
	$(COMPOSE_MAIN) exec -T spark /opt/spark/bin/spark-submit --master local[2] /opt/spark_jobs/spark_batch_job.py
	@echo "Batch job finished successfully. Output CSV: /tmp/transformed_orders.csv"

prepare-demo-data: ## Ensure MinIO buckets and sample orders CSV exist for Spark batch demo
	$(COMPOSE_MAIN) exec -T spark python3 -c "import boto3; s3=boto3.client('s3', endpoint_url='http://minio:9000', aws_access_key_id='minio', aws_secret_access_key='minio123'); data='order_id,customer_id,amount\\n1,1001,120.5\\n2,1002,75.0\\n3,1001,30.0\\n4,1003,200.0\\n'; exec(\"for b in ('raw-data','processed-data'):\\n    try:\\n        s3.head_bucket(Bucket=b)\\n    except Exception:\\n        s3.create_bucket(Bucket=b)\"); s3.put_object(Bucket='raw-data', Key='orders/orders.csv', Body=data.encode('utf-8')); print('Seeded MinIO demo data at s3://raw-data/orders/orders.csv')"

run-iceberg-demo: prepare-demo-data ## Run Spark batch job with Iceberg table write enabled
	$(COMPOSE_MAIN) exec -T \
		-e ENABLE_ICEBERG=true \
		-e ICEBERG_CATALOG=local \
		-e ICEBERG_NAMESPACE=analytics \
		-e ICEBERG_TABLE=orders \
		-e ICEBERG_WAREHOUSE=file:///tmp/iceberg_warehouse \
		spark /opt/spark/bin/spark-submit --master local[2] /opt/spark_jobs/spark_batch_job.py
	@echo "Iceberg demo finished successfully. Table: local.analytics.orders (warehouse=file:///tmp/iceberg_warehouse)"
