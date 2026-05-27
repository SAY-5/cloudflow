package dev.cloudflow.assistant.bench;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Bench regression gate at 30%.
 *
 * <p>Runs a small, deterministic benchmark and fails if assist P95 latency exceeds the checked-in
 * baseline by more than the allowed margin, or if ingest throughput drops below the baseline by
 * more than the margin. The baseline is intentionally generous so the gate flags real regressions,
 * not runner jitter.
 */
class BenchRegressionTest {

  // Conservative baselines for the gate size (5k logs, 500 queries) on a CI runner.
  private static final double BASELINE_ASSIST_P95_MS = 8.0;
  private static final double BASELINE_INGEST_PER_SEC = 20_000.0;
  private static final double MARGIN = 0.30;

  @Test
  void assistLatencyAndIngestThroughputDoNotRegress() {
    BenchmarkRunner runner = new BenchmarkRunner(256, 6);
    BenchmarkRunner.Result result = runner.run(5_000, 500, 7L);

    double p95Ceiling = BASELINE_ASSIST_P95_MS * (1 + MARGIN);
    double ingestFloor = BASELINE_INGEST_PER_SEC * (1 - MARGIN);

    System.out.printf(
        "[bench-regress] assist p95=%.4fms (ceiling %.2fms) ingest=%.0f/s (floor %.0f/s)%n",
        result.assistLatency().p95Ms(), p95Ceiling, result.ingestPerSecond(), ingestFloor);

    assertThat(result.assistLatency().p95Ms())
        .as("assist P95 latency regressed beyond %.0f%%", MARGIN * 100)
        .isLessThanOrEqualTo(p95Ceiling);
    assertThat(result.ingestPerSecond())
        .as("ingest throughput regressed beyond %.0f%%", MARGIN * 100)
        .isGreaterThanOrEqualTo(ingestFloor);
  }
}
