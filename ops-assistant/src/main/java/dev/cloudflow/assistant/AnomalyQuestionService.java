package dev.cloudflow.assistant;

import dev.cloudflow.assistant.store.AnomalyEntity;
import dev.cloudflow.assistant.store.AnomalyRepository;
import dev.cloudflow.assistant.store.RagChunkEntity;
import dev.cloudflow.assistant.store.RagChunkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Answers the anomaly-shaped questions the assistant can serve directly from the anomaly store and
 * the windowed logs, separate from the general RAG path.
 */
@Service
public class AnomalyQuestionService {

  private static final Pattern AT_TIME = Pattern.compile("at\\s+(\\d{1,2}):(\\d{2})");
  private static final Pattern SERVICE =
      Pattern.compile("\\b(orders|inventory|gateway|collector|assistant)\\b");

  private final AnomalyRepository anomalyRepository;
  private final RagChunkRepository chunkRepository;
  private final Clock clock;

  public AnomalyQuestionService(
      AnomalyRepository anomalyRepository, RagChunkRepository chunkRepository, Clock clock) {
    this.anomalyRepository = anomalyRepository;
    this.chunkRepository = chunkRepository;
    this.clock = clock;
  }

  public boolean isAnomalyQuestion(String question) {
    String q = question.toLowerCase();
    return q.contains("anomal") || (q.contains("spike") && AT_TIME.matcher(q).find());
  }

  /** "what anomalies happened today?" answered from the anomaly store. */
  public AssistantService.AssistResult anomaliesToday() {
    Instant now = Instant.now(clock);
    LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
    Instant from = today.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = from.plus(Duration.ofDays(1));

    List<AnomalyEntity> found = anomalyRepository.findInRange(from, to);
    if (found.isEmpty()) {
      return new AssistantService.AssistResult("No anomalies were recorded today.", List.of());
    }
    StringBuilder sb = new StringBuilder("Anomalies recorded today:\n");
    List<AssistantService.Citation> citations = new java.util.ArrayList<>();
    for (AnomalyEntity a : found) {
      sb.append("- ")
          .append(a.getService())
          .append(" error-rate ")
          .append(String.format("%.0f%%", a.getErrorRate() * 100))
          .append(" at ")
          .append(a.getWindowStart())
          .append(" (signatures: ")
          .append(a.getTopSignatures())
          .append(")\n");
      citations.add(
          new AssistantService.Citation(
              "anomaly:" + a.getId(), "anomaly", a.getService() + " " + a.getTopSignatures()));
    }
    return new AssistantService.AssistResult(sb.toString().strip(), citations);
  }

  /**
   * "why did X spike at 14:00?" answered by retrieving the logs in that window. Returns empty when
   * the question has no parseable time.
   */
  public Optional<AssistantService.AssistResult> spikeAtTime(String question) {
    Matcher m = AT_TIME.matcher(question.toLowerCase());
    if (!m.find()) {
      return Optional.empty();
    }
    int hour = Integer.parseInt(m.group(1));
    int minute = Integer.parseInt(m.group(2));
    Matcher sm = SERVICE.matcher(question.toLowerCase());
    String service = sm.find() ? sm.group(1) : "orders";

    LocalDate today = Instant.now(clock).atZone(ZoneOffset.UTC).toLocalDate();
    Instant from = today.atTime(LocalTime.of(hour, minute)).toInstant(ZoneOffset.UTC);
    Instant to = from.plus(Duration.ofMinutes(1));

    List<RagChunkEntity> logs = chunkRepository.findLogsInWindow(service, from, to);
    if (logs.isEmpty()) {
      return Optional.of(
          new AssistantService.AssistResult(
              "I found no " + service + " logs in the window around " + from + ".", List.of()));
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Around ")
        .append(from)
        .append(", ")
        .append(service)
        .append(" emitted these log lines:\n");
    List<AssistantService.Citation> citations = new java.util.ArrayList<>();
    for (RagChunkEntity log : logs) {
      sb.append("- [").append(log.getId()).append("] ").append(log.getContent()).append('\n');
      citations.add(new AssistantService.Citation(log.getId(), "log", firstLine(log.getContent())));
    }
    sb.append("Citations: ")
        .append(String.join(", ", citations.stream().map(AssistantService.Citation::id).toList()));
    return Optional.of(new AssistantService.AssistResult(sb.toString().strip(), citations));
  }

  private static String firstLine(String text) {
    int nl = text.indexOf('\n');
    return nl < 0 ? text : text.substring(0, nl);
  }
}
