package dev.cloudflow.assistant.remediation;

import dev.cloudflow.assistant.store.AnomalyEntity;
import dev.cloudflow.assistant.store.AnomalyRepository;
import dev.cloudflow.assistant.store.RagChunkEntity;
import dev.cloudflow.assistant.store.RagChunkRepository;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.rag.Chunk;
import dev.cloudflow.common.rag.HybridRetriever;
import dev.cloudflow.common.rag.Scored;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Proposes a structured remediation when asked how to fix something: ordered steps, the
 * kubectl/helm commands taken from the matching runbook, a confidence, and an explicit
 * verify-before-running disclaimer. Destructive commands are blocked unless the runbook contains
 * them.
 */
@Service
public class RemediationService {

  private static final String DISCLAIMER =
      "Verify before running: confirm you are on the right cluster and namespace; "
          + "these commands are suggestions, not executed by CloudFlow.";

  private final RagChunkRepository chunkRepository;
  private final AnomalyRepository anomalyRepository;
  private final Embedder embedder;
  private final int topK;

  public RemediationService(
      RagChunkRepository chunkRepository,
      AnomalyRepository anomalyRepository,
      Embedder embedder,
      @Value("${cloudflow.assist.top-k:6}") int topK) {
    this.chunkRepository = chunkRepository;
    this.anomalyRepository = anomalyRepository;
    this.embedder = embedder;
    this.topK = topK;
  }

  public record Citation(String id, String source, String snippet) {}

  public record Remediation(
      String summary,
      List<String> steps,
      List<String> commands,
      List<String> blockedCommands,
      double confidence,
      String disclaimer,
      List<Citation> citations) {}

  public boolean isFixQuestion(String question) {
    String q = question.toLowerCase(Locale.ROOT);
    return q.contains("how do i fix")
        || q.contains("how to fix")
        || q.contains("how do i roll back")
        || q.contains("how do i rollback")
        || q.contains("remediat");
  }

  public Optional<Remediation> suggest(String question) {
    List<RagChunkEntity> docs = chunkRepository.findBySource("doc");
    if (docs.isEmpty()) {
      return Optional.empty();
    }
    List<Chunk> corpus = new ArrayList<>(docs.size());
    for (RagChunkEntity d : docs) {
      corpus.add(new Chunk(d.getId(), d.getSource(), d.getContent()));
    }
    HybridRetriever retriever = new HybridRetriever(embedder, corpus);
    List<Scored> hits = retriever.retrieve(question, topK);
    if (hits.isEmpty()) {
      return Optional.empty();
    }

    // The runbook slug of the top hit (id form doc:<slug>#<n>) anchors the suggestion.
    String slug = slugOf(hits.get(0).chunk().id());
    List<RagChunkEntity> runbookChunks =
        docs.stream().filter(d -> slugOf(d.getId()).equals(slug)).toList();

    StringBuilder runbookText = new StringBuilder();
    List<Citation> citations = new ArrayList<>();
    for (RagChunkEntity c : runbookChunks) {
      runbookText.append(c.getContent()).append('\n');
      citations.add(new Citation(c.getId(), "doc", firstLine(c.getContent())));
    }

    List<String> candidateCommands = CommandExtractor.extract(runbookText.toString());
    List<String> allowed = CommandGuard.allowed(candidateCommands, runbookText.toString());
    List<String> blocked = new ArrayList<>(candidateCommands);
    blocked.removeAll(allowed);

    List<String> steps = stepsFrom(runbookText.toString());

    // Recent anomaly context sharpens or softens confidence.
    List<AnomalyEntity> recent = anomalyRepository.findAll();
    double confidence = confidence(allowed.size(), steps.size(), !recent.isEmpty());
    String summary =
        "Suggested remediation from runbook '"
            + slug
            + "'"
            + (recent.isEmpty() ? "." : " (recent anomalies are recorded).");

    return Optional.of(
        new Remediation(summary, steps, allowed, blocked, confidence, DISCLAIMER, citations));
  }

  private static List<String> stepsFrom(String runbookText) {
    List<String> steps = new ArrayList<>();
    for (String raw : runbookText.split("\n")) {
      String line = raw.strip();
      if (line.matches("^\\d+\\.\\s+.*")) {
        steps.add(line);
      }
    }
    return steps;
  }

  private static double confidence(int commandCount, int stepCount, boolean hasAnomalies) {
    double base = 0.4;
    if (commandCount > 0) {
      base += 0.3;
    }
    if (stepCount > 0) {
      base += 0.2;
    }
    if (hasAnomalies) {
      base += 0.1;
    }
    return Math.min(1.0, base);
  }

  private static String slugOf(String id) {
    // doc:<slug>#<n>
    int colon = id.indexOf(':');
    int hash = id.indexOf('#');
    if (colon < 0 || hash < 0 || hash <= colon) {
      return id;
    }
    return id.substring(colon + 1, hash);
  }

  private static String firstLine(String text) {
    int nl = text.indexOf('\n');
    return nl < 0 ? text : text.substring(0, nl);
  }
}
