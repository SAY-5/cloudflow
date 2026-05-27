# Benchmarks

CloudFlow ships a deterministic benchmark for the two hot paths the platform
owns: log ingest (embed + encode per line) and assist (retrieve + ground). LLM
time is excluded because the benchmark uses the `FakeLlmProvider`, so the numbers
reflect retrieval and citation grounding rather than model latency.

## Running

```
make bench           # 20k logs, 1000 queries by default; writes bench/results/assist-bench.json
make bench-regress   # 5k logs, 500 queries against the 30% regression gate
```

A full-scale run:

```
./mvnw -pl ops-assistant -am test -Dtest=BenchmarkRunnerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dbench.logs=100000 -Dbench.queries=1000
```

## Reference numbers (100k logs, 1000 queries, dim 256, topK 6)

| Metric                | Value        |
| --------------------- | ------------ |
| Ingest throughput     | ~154,000 lines/s |
| Assist latency P50    | ~114 ms      |
| Assist latency P95    | ~137 ms      |
| Assist latency P99    | ~172 ms      |

Assist latency is dominated by the in-memory hybrid retriever scanning the full
corpus per query; the production path pushes that scan into pgvector. The
in-app numbers give a stable, model-free regression signal.

## Regression gate

`make bench-regress` fails when assist P95 exceeds the baseline by more than 30%
or ingest throughput drops below the baseline by more than 30%. Baselines live in
`BenchRegressionTest` and are intentionally generous so the gate flags real
regressions rather than runner jitter.
