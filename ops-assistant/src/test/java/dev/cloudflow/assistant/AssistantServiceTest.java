package dev.cloudflow.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.assistant.AssistantService.AssistResult;
import dev.cloudflow.assistant.store.RagChunkEntity;
import dev.cloudflow.assistant.store.RagChunkRepository;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.EmbeddingCodec;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AssistantServiceTest {

  @Autowired private AssistantService service;
  @Autowired private RagChunkRepository repository;
  @Autowired private Embedder embedder;

  @BeforeEach
  void seed() {
    repository.deleteAll();
    save(
        "doc:rollback-inventory#1",
        "doc",
        "To roll back inventory run helm rollback inventory",
        null,
        null);
    save(
        "doc:orders-spike#1",
        "doc",
        "Investigate an orders error rate spike and group by signature",
        null,
        null);
    save(
        "log:1",
        "log",
        "orders ERROR order failed signature=payment_gateway_timeout",
        "orders",
        Instant.parse("2026-05-26T14:00:00Z"));
    save(
        "log:2",
        "log",
        "inventory INFO stock adjusted available=6",
        "inventory",
        Instant.parse("2026-05-26T14:01:00Z"));
    save("doc:weather#1", "doc", "The weather is sunny in paris today", null, null);
  }

  private void save(String id, String source, String content, String svc, Instant ts) {
    repository.save(
        new RagChunkEntity(
            id, source, content, EmbeddingCodec.encode(embedder.embed(content)), svc, ts));
  }

  @Test
  void answersRollbackQuestionWithGroundedCitations() {
    AssistResult result = service.assist("how do I roll back inventory");

    assertThat(result.citations()).isNotEmpty();
    Set<String> citedIds = new HashSet<>();
    result.citations().forEach(c -> citedIds.add(c.id()));
    assertThat(citedIds).contains("doc:rollback-inventory#1");
  }

  @Test
  void everyCitationExistsInTheStore() {
    AssistResult result = service.assist("why did orders error rate spike");

    Set<String> storeIds = new HashSet<>();
    repository.findAll().forEach(c -> storeIds.add(c.getId()));
    assertThat(result.citations()).allMatch(c -> storeIds.contains(c.id()));
  }

  @Test
  void handlesQuestionWithNoMatchesGracefully() {
    repository.deleteAll();
    AssistResult result = service.assist("anything at all");
    assertThat(result.citations()).isEmpty();
  }
}
