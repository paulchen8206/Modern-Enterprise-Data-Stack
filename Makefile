SHELL := /bin/bash

PYTHON ?= $(shell [ -x .venv/bin/python ] && echo .venv/bin/python || echo python3)
COMPOSE_MAIN ?= docker-compose --project-directory . -f infra/compose/docker-compose.yaml
COMPOSE_CI ?= docker-compose --project-directory . -f infra/compose/docker-compose.ci.yaml
TERRAFORM ?= terraform
PRETTIER ?= prettier

.PHONY: help up down restart ps logs clean \
	format format-python format-text terraform-fmt \
	validate validate-compose validate-shell validate-python validate-json validate-yaml validate-notebook validate-format validate-terraform \
	terraform-init terraform-validate \
	run-kafka-producer run-streaming-job run-batch-job run-iceberg-demo

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
	$(PYTHON) -m black pipelines/airflow pipelines/bi_dashboards pipelines/governance pipelines/great_expectations pipelines/kafka pipelines/ml pipelines/monitoring pipelines/spark pipelines/storage devtools/serve_wiki.py

format-text: ## Format markdown/yaml/json/js/css/html with Prettier
	find . -type f \( -name '*.md' -o -name '*.yaml' -o -name '*.yml' -o -name '*.json' -o -name '*.js' -o -name '*.css' -o -name '*.html' \) \
		! -path './.venv/*' \
		! -path './.git/*' \
		! -path './dotnet-api/src/DataPipelineApi/bin/*' \
		! -path './dotnet-api/src/DataPipelineApi/obj/*' \
		! -path './dotnet-api/nupkg/*' \
		-print0 | xargs -0 $(PRETTIER) --write

terraform-fmt: ## Format Terraform files
	$(TERRAFORM) -chdir=iac fmt -recursive

validate: validate-compose validate-shell validate-python validate-json validate-yaml validate-notebook validate-format validate-terraform ## Run full project validation

validate-compose: ## Validate docker-compose configuration files
	$(COMPOSE_MAIN) config >/dev/null
	$(COMPOSE_CI) config >/dev/null

validate-shell: ## Validate shell script syntax
	find . -type f -name '*.sh' ! -path './.venv/*' -print0 | xargs -0 -n1 bash -n

validate-python: ## Validate Python syntax
	$(PYTHON) -m compileall pipelines/airflow pipelines/bi_dashboards pipelines/governance pipelines/great_expectations pipelines/kafka pipelines/ml pipelines/monitoring pipelines/spark pipelines/storage devtools/serve_wiki.py

validate-json: ## Validate JSON files
	@find . -type f -name '*.json' \
		! -path './.venv/*' \
		! -path './.git/*' \
		! -path './dotnet-api/src/DataPipelineApi/bin/*' \
		! -path './dotnet-api/src/DataPipelineApi/obj/*' \
		-print0 | xargs -0 -n1 $(PYTHON) -m json.tool >/dev/null
	@echo "JSON validation passed."

validate-yaml: ## Validate YAML files
	@find . -type f \( -name '*.yml' -o -name '*.yaml' \) \
		! -path './.venv/*' \
		! -path './.git/*' \
		! -path './dotnet-api/src/DataPipelineApi/bin/*' \
		! -path './dotnet-api/src/DataPipelineApi/obj/*' \
		! -path './dotnet-api/nupkg/*' \
		-print0 | xargs -0 -n1 $(PYTHON) -c "import sys,yaml; list(yaml.safe_load_all(open(sys.argv[1], encoding='utf-8')))"
	@echo "YAML validation passed."

validate-notebook: ## Validate notebook schema
	@$(PYTHON) -c "import nbformat; nbformat.read(open('notebooks/modern-data-stack.ipynb','r',encoding='utf-8'), as_version=4); print('Notebook schema validation passed.')"

validate-format: ## Check markdown/yaml/json/js/css/html formatting
	find . -type f \( -name '*.md' -o -name '*.yaml' -o -name '*.yml' -o -name '*.json' -o -name '*.js' -o -name '*.css' -o -name '*.html' \) \
		! -path './.venv/*' \
		! -path './.git/*' \
		! -path './dotnet-api/src/DataPipelineApi/bin/*' \
		! -path './dotnet-api/src/DataPipelineApi/obj/*' \
		! -path './dotnet-api/nupkg/*' \
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

run-kafka-producer: ## Run Kafka producer in compose stack
	$(COMPOSE_MAIN) exec kafka python /opt/spark_jobs/../kafka/producer.py

run-streaming-job: ## Run Spark streaming job in compose stack
	$(COMPOSE_MAIN) exec spark /opt/spark/bin/spark-submit --master local[2] /opt/spark_jobs/spark_streaming_job.py

run-batch-job: ## Run Spark batch job in compose stack
	$(COMPOSE_MAIN) exec spark /opt/spark/bin/spark-submit --master local[2] /opt/spark_jobs/spark_batch_job.py

run-iceberg-demo: ## Run Spark batch job with Iceberg table write enabled
	$(COMPOSE_MAIN) exec \
		-e ENABLE_ICEBERG=true \
		-e ICEBERG_CATALOG=local \
		-e ICEBERG_NAMESPACE=analytics \
		-e ICEBERG_TABLE=orders \
		-e ICEBERG_WAREHOUSE=file:///tmp/iceberg_warehouse \
		spark /opt/spark/bin/spark-submit --master local[2] /opt/spark_jobs/spark_batch_job.py
