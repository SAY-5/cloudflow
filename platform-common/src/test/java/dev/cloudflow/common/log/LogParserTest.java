package dev.cloudflow.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LogParserTest {

  @Test
  void parsesCanonicalLine() {
    String line =
        "{\"ts\":\"2026-05-26T14:00:00Z\",\"service\":\"orders\",\"level\":\"ERROR\","
            + "\"trace_id\":\"abc\",\"msg\":\"payment failed\",\"fields\":{\"code\":\"502\"}}";

    LogEntry entry = LogParser.parse(line);

    assertThat(entry.service()).isEqualTo("orders");
    assertThat(entry.level()).isEqualTo("ERROR");
    assertThat(entry.ts()).isEqualTo(Instant.parse("2026-05-26T14:00:00Z"));
    assertThat(entry.traceId()).isEqualTo("abc");
    assertThat(entry.msg()).isEqualTo("payment failed");
    assertThat(entry.fields()).containsEntry("code", "502");
    assertThat(entry.isError()).isTrue();
  }

  @Test
  void roundTripsThroughFormatter() {
    String line =
        "{\"ts\":\"2026-05-26T14:00:00Z\",\"service\":\"inventory\",\"level\":\"INFO\","
            + "\"trace_id\":\"t1\",\"msg\":\"ok\",\"fields\":{}}";

    LogEntry entry = LogParser.parse(line);
    String rendered = JsonLogFormatter.format(entry);
    LogEntry reparsed = LogParser.parse(rendered);

    assertThat(reparsed).isEqualTo(entry);
  }

  @Test
  void rejectsMissingFields() {
    assertThatThrownBy(() -> LogParser.parse("{\"service\":\"orders\"}"))
        .isInstanceOf(LogParseException.class);
  }

  @Test
  void rejectsGarbage() {
    assertThatThrownBy(() -> LogParser.parse("not json")).isInstanceOf(LogParseException.class);
  }
}
