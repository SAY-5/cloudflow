package dev.cloudflow.common.log;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Emits canonical CloudFlow JSON log lines for a single service.
 *
 * <p>The sink is pluggable so services can write to stdout in production and capture lines in
 * tests. Each emitted line carries a trace id; callers may supply one to correlate a request.
 */
public final class StructuredLogger {

  private final String service;
  private final Clock clock;
  private final Consumer<String> sink;

  public StructuredLogger(String service, Consumer<String> sink) {
    this(service, sink, Clock.systemUTC());
  }

  public StructuredLogger(String service, Consumer<String> sink, Clock clock) {
    this.service = service;
    this.sink = sink;
    this.clock = clock;
  }

  /** A logger that writes JSON lines to stdout. */
  public static StructuredLogger toStdout(String service) {
    return new StructuredLogger(service, System.out::println);
  }

  public void info(String msg, Map<String, String> fields) {
    emit("INFO", newTraceId(), msg, fields);
  }

  public void error(String msg, Map<String, String> fields) {
    emit("ERROR", newTraceId(), msg, fields);
  }

  public void log(String level, String traceId, String msg, Map<String, String> fields) {
    emit(level, traceId, msg, fields);
  }

  private void emit(String level, String traceId, String msg, Map<String, String> fields) {
    LogEntry entry =
        new LogEntry(Instant.now(clock), service, level, traceId, msg == null ? "" : msg, fields);
    sink.accept(JsonLogFormatter.format(entry));
  }

  private static String newTraceId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
