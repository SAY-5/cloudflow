package dev.cloudflow.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.assistant.AssistantService.AssistResult;
import dev.cloudflow.assistant.store.AnomalyEntity;
import dev.cloudflow.assistant.store.AnomalyRepository;
import dev.cloudflow.assistant.store.RagChunkEntity;
import dev.cloudflow.assistant.store.RagChunkRepository;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.EmbeddingCodec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** The clock is pinned to the day the seeded anomaly and logs belong to. */
@SpringBootTest
class AnomalyQuestionServiceTest {

  private static final Instant NOW = Instant.parse("2026-05-26T18:00:00Z");

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  @Autowired private AssistantService service;
  @Autowired private AnomalyRepository anomalyRepository;
  @Autowired private RagChunkRepository chunkRepository;
  @Autowired private Embedder embedder;

  @BeforeEach
  void seed() {
    anomalyRepository.deleteAll();
    chunkRepository.deleteAll();
    anomalyRepository.save(
        new AnomalyEntity(
            "orders",
            Instant.parse("2026-05-26T14:00:00Z"),
            Instant.parse("2026-05-26T14:01:00Z"),
            1.0,
            6.0,
            "payment_gateway_timeout"));
    saveLog(
        "log:1",
        "orders ERROR order failed signature=payment_gateway_timeout",
        "orders",
        Instant.parse("2026-05-26T14:00:30Z"));
  }

  private void saveLog(String id, String content, String svc, Instant ts) {
    chunkRepository.save(
        new RagChunkEntity(
            id, "log", content, EmbeddingCodec.encode(embedder.embed(content)), svc, ts));
  }

  @Test
  void answersWhatAnomaliesHappenedTodayFromTheStore() {
    AssistResult result = service.assist("what anomalies happened today?");

    assertThat(result.answer()).contains("orders").contains("payment_gateway_timeout");
    assertThat(result.citations()).isNotEmpty();
    assertThat(result.citations().get(0).id()).startsWith("anomaly:");
  }

  @Test
  void answersWhyDidOrdersSpikeAt1400WithLogsFromThatWindow() {
    AssistResult result = service.assist("why did orders error-rate spike at 14:00?");

    assertThat(result.citations()).extracting(AssistantService.Citation::id).contains("log:1");
    assertThat(result.answer()).contains("payment_gateway_timeout");
  }
}
