package dev.cloudflow.common.log;

import java.time.Instant;
import java.util.Map;

/**
 * A single structured log line emitted by a CloudFlow service.
 *
 * <p>The canonical JSON shape is {@code {ts, service, level, trace_id, msg, fields}}.
 */
public record LogEntry(
    Instant ts,
    String service,
    String level,
    String traceId,
    String msg,
    Map<String, String> fields) {

  public LogEntry {
    if (service == null || service.isBlank()) {
      throw new IllegalArgumentException("service is required");
    }
    if (level == null || level.isBlank()) {
      throw new IllegalArgumentException("level is required");
    }
    if (ts == null) {
      throw new IllegalArgumentException("ts is required");
    }
    fields = fields == null ? Map.of() : Map.copyOf(fields);
  }

  public boolean isError() {
    return "ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level);
  }
}
