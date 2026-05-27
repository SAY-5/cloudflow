package dev.cloudflow.common.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownChunkerTest {

  @Test
  void splitsOnHeadingsAndKeepsHeadingWithBody() {
    String md =
        "# Runbook: roll back\n\nintro text\n\n## Steps\n\nrun helm rollback inventory\n\n## Verify\n\ncheck pods";

    List<Chunk> chunks = MarkdownChunker.chunk("rollback-inventory", md);

    assertThat(chunks).hasSize(3);
    assertThat(chunks.get(0).id()).isEqualTo("doc:rollback-inventory#0");
    assertThat(chunks.get(1).text()).contains("## Steps").contains("helm rollback inventory");
    assertThat(chunks).allMatch(c -> c.source().equals("doc"));
  }

  @Test
  void emptyInputYieldsNoChunks() {
    assertThat(MarkdownChunker.chunk("x", "")).isEmpty();
  }
}
