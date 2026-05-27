package dev.cloudflow.assistant.bench;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Runs the ingest + assist benchmark and writes a results file under bench/results.
 *
 * <p>Counts default to a CI-friendly size and can be raised for a full run via system properties,
 * e.g. {@code -Dbench.logs=100000 -Dbench.queries=1000}.
 */
class BenchmarkRunnerTest {

  @Test
  void runsBenchmarkAndWritesResults() throws Exception {
    int logs = Integer.getInteger("bench.logs", 20_000);
    int queries = Integer.getInteger("bench.queries", 1_000);

    BenchmarkRunner runner = new BenchmarkRunner(256, 6);
    BenchmarkRunner.Result result = runner.run(logs, queries, 42L);

    assertThat(result.logCount()).isEqualTo(logs);
    assertThat(result.ingestPerSecond()).isPositive();
    assertThat(result.assistLatency().count()).isEqualTo(queries);
    assertThat(result.assistLatency().p99Ms())
        .isGreaterThanOrEqualTo(result.assistLatency().p50Ms());

    Path out = Path.of("bench", "results");
    Files.createDirectories(out);
    String json =
        String.format(
            "{%n"
                + "  \"logCount\": %d,%n"
                + "  \"ingestSeconds\": %.3f,%n"
                + "  \"ingestPerSecond\": %.1f,%n"
                + "  \"assist\": { \"count\": %d, \"p50Ms\": %.4f, \"p95Ms\": %.4f, \"p99Ms\": %.4f, \"meanMs\": %.4f }%n"
                + "}%n",
            result.logCount(),
            result.ingestSeconds(),
            result.ingestPerSecond(),
            result.assistLatency().count(),
            result.assistLatency().p50Ms(),
            result.assistLatency().p95Ms(),
            result.assistLatency().p99Ms(),
            result.assistLatency().meanMs());
    Files.writeString(out.resolve("assist-bench.json"), json);

    System.out.println("[bench] " + json);
  }
}
