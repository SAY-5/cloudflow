package dev.cloudflow.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class LogParserPropertyTest {

  @Provide
  Arbitrary<LogEntry> logEntries() {
    Arbitrary<String> service = Arbitraries.of("orders", "inventory", "gateway");
    Arbitrary<String> level = Arbitraries.of("INFO", "WARN", "ERROR");
    Arbitrary<String> trace = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8);
    Arbitrary<String> msg = Arbitraries.strings().alpha().ofMaxLength(40);
    Arbitrary<Long> epoch = Arbitraries.longs().between(0L, 4_000_000_000L);
    return Combinators.combine(service, level, trace, msg, epoch)
        .as(
            (s, l, t, m, e) ->
                new LogEntry(Instant.ofEpochSecond(e), s, l, t, m, Map.of("k", "v")));
  }

  @Property(tries = 400)
  void formatThenParseRoundTrips(@ForAll("logEntries") LogEntry entry) {
    String rendered = JsonLogFormatter.format(entry);
    LogEntry reparsed = LogParser.parse(rendered);
    assertThat(reparsed).isEqualTo(entry);
  }

  @Property(tries = 400)
  void errorLevelImpliesIsError(@ForAll("logEntries") LogEntry entry) {
    assertThat(entry.isError()).isEqualTo("ERROR".equalsIgnoreCase(entry.level()));
  }
}
