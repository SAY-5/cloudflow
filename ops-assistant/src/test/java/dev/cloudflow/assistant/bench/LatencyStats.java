package dev.cloudflow.assistant.bench;

import java.util.Arrays;

/** Percentile summary over a set of latency samples in nanoseconds. */
public record LatencyStats(long count, double p50Ms, double p95Ms, double p99Ms, double meanMs) {

  public static LatencyStats of(long[] nanos) {
    if (nanos.length == 0) {
      return new LatencyStats(0, 0, 0, 0, 0);
    }
    long[] sorted = nanos.clone();
    Arrays.sort(sorted);
    double sum = 0;
    for (long n : sorted) {
      sum += n;
    }
    return new LatencyStats(
        sorted.length,
        toMs(percentile(sorted, 50)),
        toMs(percentile(sorted, 95)),
        toMs(percentile(sorted, 99)),
        toMs(sum / sorted.length));
  }

  private static long percentile(long[] sorted, int p) {
    int idx = (int) Math.ceil((p / 100.0) * sorted.length) - 1;
    idx = Math.max(0, Math.min(sorted.length - 1, idx));
    return sorted[idx];
  }

  private static double toMs(double nanos) {
    return nanos / 1_000_000.0;
  }
}
