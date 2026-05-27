package dev.cloudflow.collector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IngestServiceTest {

  @Autowired private IngestService ingestService;
  @Autowired private RagChunkRepository repository;

  @Test
  void ingestsLogLineAndStoresEmbeddedChunk() {
    String line =
        "{\"ts\":\"2026-05-26T14:00:00Z\",\"service\":\"orders\",\"level\":\"ERROR\","
            + "\"trace_id\":\"t1\",\"msg\":\"order failed\",\"fields\":{\"signature\":\"payment_gateway_timeout\"}}";

    String id = ingestService.ingestLogLine(line);

    RagChunkEntity stored = repository.findById(id).orElseThrow();
    assertThat(stored.getSource()).isEqualTo("log");
    assertThat(stored.getService()).isEqualTo("orders");
    assertThat(stored.getContent()).contains("payment_gateway_timeout");
    assertThat(stored.getEmbedding()).isNotBlank();
  }

  @Test
  void ingestsRunbookAsMultipleChunks() {
    String md = "# Title\n\nintro\n\n## Steps\n\nhelm rollback inventory\n";

    int chunks = ingestService.ingestRunbook("rollback-inventory", md);

    assertThat(chunks).isGreaterThanOrEqualTo(2);
    assertThat(repository.findBySource("doc")).isNotEmpty();
  }
}
