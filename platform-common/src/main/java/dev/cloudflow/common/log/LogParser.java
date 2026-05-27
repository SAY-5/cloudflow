package dev.cloudflow.common.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parses the canonical CloudFlow JSON log shape into {@link LogEntry} values. */
public final class LogParser {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private LogParser() {}

  /**
   * Parses one JSON log line.
   *
   * @throws LogParseException if the line is not valid JSON or is missing required fields
   */
  public static LogEntry parse(String line) {
    if (line == null || line.isBlank()) {
      throw new LogParseException("empty log line");
    }
    try {
      JsonNode node = MAPPER.readTree(line);
      String service = text(node, "service");
      String level = text(node, "level");
      String tsRaw = text(node, "ts");
      if (service == null || level == null || tsRaw == null) {
        throw new LogParseException("missing required field (ts/service/level)");
      }
      Instant ts = Instant.parse(tsRaw);
      String traceId = text(node, "trace_id");
      String msg = text(node, "msg");
      Map<String, String> fields = new LinkedHashMap<>();
      JsonNode fieldsNode = node.get("fields");
      if (fieldsNode != null && fieldsNode.isObject()) {
        Iterator<Map.Entry<String, JsonNode>> it = fieldsNode.fields();
        while (it.hasNext()) {
          Map.Entry<String, JsonNode> e = it.next();
          fields.put(e.getKey(), e.getValue().asText());
        }
      }
      return new LogEntry(ts, service, level, traceId, msg == null ? "" : msg, fields);
    } catch (LogParseException e) {
      throw e;
    } catch (Exception e) {
      throw new LogParseException("could not parse log line: " + e.getMessage(), e);
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode v = node.get(field);
    return v == null || v.isNull() ? null : v.asText();
  }
}
