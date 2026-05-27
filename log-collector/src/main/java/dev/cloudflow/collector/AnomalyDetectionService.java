package dev.cloudflow.collector;

import dev.cloudflow.common.anomaly.Anomaly;
import dev.cloudflow.common.anomaly.AnomalyDetector;
import dev.cloudflow.common.log.LogEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the {@link AnomalyDetector} over the stored log chunks and persists any anomalies it finds.
 *
 * <p>Stored log content has the shape {@code "<service> <level> <msg> k=v ..."}, which is enough to
 * reconstruct the level and signature the detector needs; the service and timestamp come from the
 * chunk's own columns.
 */
@Service
public class AnomalyDetectionService {

  private final RagChunkRepository chunkRepository;
  private final AnomalyRepository anomalyRepository;
  private final AnomalyDetector detector;

  public AnomalyDetectionService(
      RagChunkRepository chunkRepository, AnomalyRepository anomalyRepository) {
    this.chunkRepository = chunkRepository;
    this.anomalyRepository = anomalyRepository;
    this.detector = AnomalyDetector.withDefaults();
  }

  @Transactional
  public List<Anomaly> detectAndStore() {
    List<LogEntry> entries = new ArrayList<>();
    for (RagChunkEntity chunk : chunkRepository.findBySource("log")) {
      if (chunk.getTs() == null || chunk.getService() == null) {
        continue;
      }
      entries.add(toLogEntry(chunk));
    }
    List<Anomaly> anomalies = detector.detect(entries);
    for (Anomaly a : anomalies) {
      anomalyRepository.save(
          new AnomalyEntity(
              a.service(),
              a.windowStart(),
              a.windowEnd(),
              a.errorRate(),
              a.zScore(),
              String.join(",", a.topSignatures())));
    }
    return anomalies;
  }

  static LogEntry toLogEntry(RagChunkEntity chunk) {
    String content = chunk.getContent();
    String[] parts = content.split(" ");
    String level = parts.length > 1 ? parts[1] : "INFO";
    String signature = "";
    for (String token : parts) {
      if (token.startsWith("signature=")) {
        signature = token.substring("signature=".length());
        break;
      }
    }
    Map<String, String> fields = signature.isEmpty() ? Map.of() : Map.of("signature", signature);
    return new LogEntry(chunk.getTs(), chunk.getService(), level, "", content, fields);
  }
}
