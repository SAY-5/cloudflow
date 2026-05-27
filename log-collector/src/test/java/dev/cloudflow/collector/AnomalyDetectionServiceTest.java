package dev.cloudflow.collector;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.common.anomaly.Anomaly;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AnomalyDetectionServiceTest {

  @Autowired private IngestService ingestService;
  @Autowired private AnomalyDetectionService detectionService;
  @Autowired private RagChunkRepository chunkRepository;
  @Autowired private AnomalyRepository anomalyRepository;

  private final Instant base = Instant.parse("2026-05-26T13:50:00Z");

  @BeforeEach
  void reset() {
    anomalyRepository.deleteAll();
    chunkRepository.deleteAll();
  }

  private String line(Instant ts, boolean error) {
    return String.format(
        "{\"ts\":\"%s\",\"service\":\"orders\",\"level\":\"%s\",\"trace_id\":\"t\","
            + "\"msg\":\"%s\",\"fields\":{%s}}",
        ts,
        error ? "ERROR" : "INFO",
        error ? "order failed" : "ok",
        error ? "\"signature\":\"payment_gateway_timeout\"" : "");
  }

  @Test
  void detectsInjectedSpikeWithRightWindowAndSignature() {
    for (int m = 0; m < 10; m++) {
      for (int i = 0; i < 10; i++) {
        ingestService.ingestLogLine(line(base.plus(Duration.ofMinutes(m)), false));
      }
    }
    Instant spikeMinute = base.plus(Duration.ofMinutes(10)); // 14:00
    for (int i = 0; i < 10; i++) {
      ingestService.ingestLogLine(line(spikeMinute, true));
    }

    List<Anomaly> anomalies = detectionService.detectAndStore();

    assertThat(anomalies).hasSize(1);
    Anomaly a = anomalies.get(0);
    assertThat(a.windowStart()).isEqualTo(Instant.parse("2026-05-26T14:00:00Z"));
    assertThat(a.topSignatures()).contains("payment_gateway_timeout");

    AnomalyEntity stored = anomalyRepository.findAll().get(0);
    assertThat(stored.getService()).isEqualTo("orders");
    assertThat(stored.getTopSignatures()).contains("payment_gateway_timeout");
  }

  @Test
  void reconstructsLevelAndSignatureFromStoredContent() {
    RagChunkEntity chunk =
        new RagChunkEntity(
            "log:1", "log", "orders ERROR boom signature=downstream_5xx", "0.0", "orders", base);
    var entry = AnomalyDetectionService.toLogEntry(chunk);
    assertThat(entry.isError()).isTrue();
    assertThat(entry.fields()).containsEntry("signature", "downstream_5xx");
    assertThat(Map.of("signature", "downstream_5xx")).isEqualTo(entry.fields());
  }
}
