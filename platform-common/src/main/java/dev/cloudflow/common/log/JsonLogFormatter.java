package dev.cloudflow.common.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders a {@link LogEntry} back to the canonical one-line JSON shape. */
public final class JsonLogFormatter {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private JsonLogFormatter() {}

  public static String format(LogEntry entry) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("ts", entry.ts().toString());
    root.put("service", entry.service());
    root.put("level", entry.level());
    root.put("trace_id", entry.traceId());
    root.put("msg", entry.msg());
    root.put("fields", entry.fields());
    try {
      return MAPPER.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("could not serialize log entry", e);
    }
  }
}
