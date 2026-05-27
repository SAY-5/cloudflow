# CloudFlow

Hybrid-cloud microservice platform with an AI ops-assistant.

CloudFlow runs a small fleet of Java 21 + Spring Boot services behind a gateway,
ships their structured logs into Postgres + pgvector, and exposes an LLM-powered
ops-assistant that answers operational questions ("why did orders error-rate
spike at 14:00?", "how do I roll back inventory?") grounded in the platform's own
logs and runbooks. A React + TypeScript dashboard drives it, and a Helm chart
deploys the whole thing to Kubernetes.

## Components

| Module            | What it does                                                          |
| ----------------- | --------------------------------------------------------------------- |
| `platform-common` | Shared structured-log model, JSON log parser, deterministic embedder. |
| `orders-service`  | Orders CRUD, Postgres-backed, emits structured JSON logs.             |
| `inventory-service` | Stock levels, Postgres-backed, emits structured JSON logs.          |
| `gateway`         | Routes to services and aggregates `/actuator/health` for the dashboard. |
| `log-collector`   | Ingests structured logs + runbook docs into Postgres + pgvector.      |
| `ops-assistant`   | RAG endpoint that answers ops questions with grounded citations.      |
| `dashboard`       | React + TS console: health grid, log viewer, "Ask the assistant".     |
| `charts/cloudflow`| Helm chart deploying every service + Postgres to Kubernetes.          |

## The ops-assistant

`POST /v1/assist {question}` retrieves the most relevant log lines and runbook
chunks (hybrid vector + keyword), builds a grounded prompt, and returns an answer
that cites the specific log line and doc-section ids it used. The core
correctness property, enforced by tests, is that every cited id must exist in the
retrieved candidate set: the assistant cannot cite a source it did not retrieve.

In CI the assistant uses a `FakeLLMProvider` so no real model is ever called; a
`ClaudeProvider` stub stands in for live use.

## Embeddings

Embeddings use a deterministic `HashEmbedder` (FNV-1a hashing into a fixed number
of buckets, L2-normalized). Determinism lets CI exercise the full
embed-store-retrieve pipeline without a model, and keeps retrieval reproducible.

## Build and test

Requires Java 21, Maven (wrapper included), Node, Helm, and kubeconform.

```
make verify          # spotless check + full Java build and tests
make dashboard-test  # lint + typecheck + unit test the dashboard
make helm-validate   # helm lint + helm template | kubeconform -strict
make bench           # ingest + assist latency benchmark
```

Postgres-backed integration tests use Testcontainers with the
`pgvector/pgvector` image, so a Docker daemon is required for those.

## License

MIT. See [LICENSE](LICENSE).
