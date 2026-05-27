package dev.cloudflow.common.anomaly;

import dev.cloudflow.common.log.LogEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-service error-rate anomaly detector over a log stream.
 *
 * <p>Logs are bucketed into fixed windows. For each window the error fraction is computed and fed
 * through an exponentially-weighted moving average and variance. A window whose error-rate exceeds
 * the EWMA mean by more than {@code zThreshold} standard deviations (after a warmup) is flagged as
 * an anomaly, carrying its window bounds and the top error signatures seen in it.
 */
public final class AnomalyDetector {

  private final Duration window;
  private final double alpha;
  private final double zThreshold;
  private final int warmupWindows;
  private final double minAbsoluteJump;

  public AnomalyDetector(Duration window, double alpha, double zThreshold, int warmupWindows) {
    this(window, alpha, zThreshold, warmupWindows, 0.5);
  }

  public AnomalyDetector(
      Duration window, double alpha, double zThreshold, int warmupWindows, double minAbsoluteJump) {
    if (alpha <= 0 || alpha >= 1) {
      throw new IllegalArgumentException("alpha must be in (0,1)");
    }
    this.window = window;
    this.alpha = alpha;
    this.zThreshold = zThreshold;
    this.warmupWindows = warmupWindows;
    this.minAbsoluteJump = minAbsoluteJump;
  }

  /** Sensible defaults: 1-minute windows, alpha 0.3, z-threshold 3, 3-window warmup. */
  public static AnomalyDetector withDefaults() {
    return new AnomalyDetector(Duration.ofMinutes(1), 0.3, 3.0, 3, 0.5);
  }

  /**
   * Scans the given entries (any order) and returns the anomalies found, grouped per service and
   * ordered by window start.
   */
  public List<Anomaly> detect(List<LogEntry> entries) {
    Map<String, List<LogEntry>> byService = new LinkedHashMap<>();
    for (LogEntry e : entries) {
      byService.computeIfAbsent(e.service(), k -> new ArrayList<>()).add(e);
    }
    List<Anomaly> anomalies = new ArrayList<>();
    for (Map.Entry<String, List<LogEntry>> svc : byService.entrySet()) {
      anomalies.addAll(detectForService(svc.getKey(), svc.getValue()));
    }
    anomalies.sort(Comparator.comparing(Anomaly::windowStart).thenComparing(Anomaly::service));
    return anomalies;
  }

  private List<Anomaly> detectForService(String service, List<LogEntry> entries) {
    entries.sort(Comparator.comparing(LogEntry::ts));
    long windowMillis = window.toMillis();

    Map<Long, List<LogEntry>> buckets = new LinkedHashMap<>();
    for (LogEntry e : entries) {
      long bucket = e.ts().toEpochMilli() / windowMillis;
      buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(e);
    }

    double ewmaMean = 0;
    double ewmaVar = 0;
    int seen = 0;
    List<Anomaly> result = new ArrayList<>();

    for (Map.Entry<Long, List<LogEntry>> bucket : buckets.entrySet()) {
      List<LogEntry> windowLogs = bucket.getValue();
      long errors = windowLogs.stream().filter(LogEntry::isError).count();
      double rate = (double) errors / windowLogs.size();

      double std = Math.sqrt(ewmaVar);
      boolean hasSpread = std > 1e-9;
      double z = hasSpread ? (rate - ewmaMean) / std : 0.0;

      // Fire on a z-score breach when there is spread, or on a large absolute jump above a
      // flat baseline where the variance is effectively zero (z would be undefined).
      boolean zBreach = hasSpread && z >= zThreshold && rate > ewmaMean;
      boolean flatJump = !hasSpread && (rate - ewmaMean) >= minAbsoluteJump;

      if (seen >= warmupWindows && (zBreach || flatJump)) {
        Instant start = Instant.ofEpochMilli(bucket.getKey() * windowMillis);
        double reportedZ = hasSpread ? z : Double.POSITIVE_INFINITY;
        result.add(
            new Anomaly(
                service, start, start.plus(window), rate, reportedZ, topSignatures(windowLogs)));
      }

      // Update EWMA mean and variance after scoring so the current window is judged
      // against history, not itself.
      double delta = rate - ewmaMean;
      ewmaMean += alpha * delta;
      ewmaVar = (1 - alpha) * (ewmaVar + alpha * delta * delta);
      seen++;
    }
    return result;
  }

  private static List<String> topSignatures(List<LogEntry> windowLogs) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (LogEntry e : windowLogs) {
      if (e.isError()) {
        String sig = e.fields().getOrDefault("signature", e.msg());
        counts.merge(sig, 1, Integer::sum);
      }
    }
    return counts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(3)
        .map(Map.Entry::getKey)
        .toList();
  }
}
