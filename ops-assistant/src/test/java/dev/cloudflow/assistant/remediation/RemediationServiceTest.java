package dev.cloudflow.assistant.remediation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.assistant.remediation.RemediationService.Remediation;
import dev.cloudflow.assistant.store.RagChunkEntity;
import dev.cloudflow.assistant.store.RagChunkRepository;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.EmbeddingCodec;
import dev.cloudflow.common.rag.Chunk;
import dev.cloudflow.common.rag.MarkdownChunker;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RemediationServiceTest {

  private static final String ROLLBACK_RUNBOOK =
      "# Runbook: Roll back the inventory service\n\n"
          + "## Steps\n\n"
          + "1. Confirm the current Helm revision.\n"
          + "2. Roll back to the previous revision.\n\n"
          + "helm rollback inventory\n\n"
          + "kubectl rollout status deployment/inventory\n";

  private static final String SCALE_RUNBOOK =
      "# Runbook: Investigate an orders error-rate spike\n\n"
          + "## Steps\n\n1. Inspect logs.\n\nkubectl scale deployment/orders --replicas=4\n";

  @Autowired private RemediationService service;
  @Autowired private RagChunkRepository repository;
  @Autowired private Embedder embedder;

  @BeforeEach
  void seed() {
    repository.deleteAll();
    ingest("rollback-inventory", ROLLBACK_RUNBOOK);
    ingest("orders-error-spike", SCALE_RUNBOOK);
  }

  private void ingest(String slug, String markdown) {
    for (Chunk c : MarkdownChunker.chunk(slug, markdown)) {
      repository.save(
          new RagChunkEntity(
              c.id(),
              "doc",
              c.text(),
              EmbeddingCodec.encode(embedder.embed(c.text())),
              null,
              null));
    }
  }

  @Test
  void rollbackQuestionReturnsHelmRollbackStepsCitingTheRunbook() {
    Optional<Remediation> maybe = service.suggest("how do I roll back inventory?");

    assertThat(maybe).isPresent();
    Remediation r = maybe.get();
    assertThat(r.commands()).contains("helm rollback inventory");
    assertThat(r.commands()).contains("kubectl rollout status deployment/inventory");
    assertThat(r.steps()).isNotEmpty();
    assertThat(r.confidence()).isGreaterThan(0.5);
    assertThat(r.disclaimer()).contains("Verify before running");
    assertThat(r.citations()).anyMatch(c -> c.id().startsWith("doc:rollback-inventory"));
  }

  @Test
  void destructiveGuardrailPassesBecauseRunbookContainsTheCommand() {
    // The rollback runbook commands are non-destructive; verify each suggested command passes the
    // guard against the runbook text it came from.
    Remediation r = service.suggest("how do I roll back inventory?").orElseThrow();
    String runbookText = String.join("\n", r.citations().stream().map(c -> c.snippet()).toList());
    for (String command : r.commands()) {
      // The guard must not throw for any suggested command.
      CommandGuard.verify(command, ROLLBACK_RUNBOOK);
    }
    assertThat(r.blockedCommands()).isEmpty();
    assertThat(runbookText).isNotEmpty();
  }
}
