package dev.cloudflow.common.anomaly;

import java.time.Instant;
import java.util.List;

/**
 * A detected error-rate anomaly for one service over a time window.
 *
 * @param service the service whose error-rate spiked
 * @param windowStart inclusive start of the window
 * @param windowEnd exclusive end of the window
 * @param errorRate observed error fraction in the window
 * @param zScore how many EWMA standard deviations above the mean the rate was
 * @param topSignatures the most frequent error signatures seen in the window
 */
public record Anomaly(
    String service,
    Instant windowStart,
    Instant windowEnd,
    double errorRate,
    double zScore,
    List<String> topSignatures) {}
