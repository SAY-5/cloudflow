JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null || echo /opt/homebrew/opt/openjdk@21)
MVN = JAVA_HOME=$(JAVA_HOME) ./mvnw
HELM ?= helm
KUBECONFORM ?= kubeconform
CHART = charts/cloudflow

.PHONY: help build test fmt fmt-check verify dashboard-install dashboard-build dashboard-test \
        helm-lint helm-template helm-validate bench bench-regress images clean

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  %-18s %s\n", $$1, $$2}'

build: ## Compile all Java modules
	$(MVN) -B -DskipTests package

test: ## Run all Java tests
	$(MVN) -B verify

fmt: ## Apply spotless formatting
	$(MVN) -B spotless:apply

fmt-check: ## Verify spotless formatting
	$(MVN) -B spotless:check

verify: fmt-check test ## Format check + full verify

dashboard-install: ## Install dashboard deps
	cd dashboard && npm ci

dashboard-build: ## Build the dashboard
	cd dashboard && npm run build

dashboard-test: ## Lint + typecheck + unit test the dashboard
	cd dashboard && npm run lint && npm run typecheck && npm run test

helm-lint: ## Lint the Helm chart
	$(HELM) lint $(CHART)

helm-template: ## Render the Helm chart
	$(HELM) template cloudflow $(CHART)

helm-validate: helm-lint ## Lint chart and validate rendered manifests with kubeconform
	$(HELM) template cloudflow $(CHART) | $(KUBECONFORM) -strict -summary \
		-schema-location default \
		-schema-location 'https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json'

bench: ## Run the ingest + assist latency benchmark
	$(MVN) -B -pl ops-assistant -am test -Dtest=BenchmarkRunnerTest \
		-Dsurefire.failIfNoSpecifiedTests=false

bench-regress: ## Run the benchmark with the regression gate
	$(MVN) -B -pl ops-assistant -am test -Dtest=BenchRegressionTest \
		-Dsurefire.failIfNoSpecifiedTests=false

images: ## Build all service container images
	docker build -t cloudflow/orders:dev -f orders-service/Dockerfile .
	docker build -t cloudflow/inventory:dev -f inventory-service/Dockerfile .
	docker build -t cloudflow/gateway:dev -f gateway/Dockerfile .
	docker build -t cloudflow/log-collector:dev -f log-collector/Dockerfile .
	docker build -t cloudflow/ops-assistant:dev -f ops-assistant/Dockerfile .
	docker build -t cloudflow/dashboard:dev -f dashboard/Dockerfile dashboard

clean: ## Clean build outputs
	$(MVN) -B clean
	rm -rf dashboard/dist dashboard/node_modules
