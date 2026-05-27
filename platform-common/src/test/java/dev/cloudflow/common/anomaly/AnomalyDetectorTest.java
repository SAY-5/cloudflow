package dev.cloudflow.common.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.common.log.LogEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnomalyDetectorTest {

  private final Instant base = Instant.parse("2026-05-26T13:50:00Z");

  private LogEntry log(int minute, boolean error) {
    return new LogEntry(
        base.plus(Duration.ofMinutes(minute)),
        "orders",
        error ? "ERROR" : "INFO",
        "t",
        error ? "order failed" : "ok",
        error ? Map.of("signature", "payment_gateway_timeout") : Map.of());
  }

  @Test
  void firesOnAnInjectedErrorSpikeWithTheRightWindowAndSignature() {
    List<LogEntry> entries = new ArrayList<>();
    // Ten calm minutes: all INFO.
    for (int m = 0; m < 10; m++) {
      for (int i = 0; i < 10; i++) {
        entries.add(log(m, false));
      }
    }
    // Minute 10 (14:00): a spike of errors.
    for (int i = 0; i < 10; i++) {
      entries.add(log(10, true));
    }

    List<Anomaly> anomalies = AnomalyDetector.withDefaults().detect(entries);

    assertThat(anomalies).hasSize(1);
    Anomaly a = anomalies.get(0);
    assertThat(a.service()).isEqualTo("orders");
    assertThat(a.windowStart()).isEqualTo(Instant.parse("2026-05-26T14:00:00Z"));
    assertThat(a.errorRate()).isEqualTo(1.0);
    assertThat(a.topSignatures()).contains("payment_gateway_timeout");
  }

  @Test
  void stableErrorRateDoesNotFire() {
    List<LogEntry> entries = new ArrayList<>();
    for (int m = 0; m < 12; m++) {
      for (int i = 0; i < 10; i++) {
        entries.add(log(m, i == 0));
      }
    }
    assertThat(AnomalyDetector.withDefaults().detect(entries)).isEmpty();
  }
}
