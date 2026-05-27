package dev.cloudflow.common.rag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.common.embed.HashEmbedder;
import java.util.List;
import org.junit.jupiter.api.Test;

class HybridRetrieverTest {

  private final List<Chunk> corpus =
      List.of(
          new Chunk(
              "doc:rollback-inventory#1",
              "doc",
              "to roll back inventory run helm rollback inventory"),
          new Chunk(
              "doc:orders-spike#1",
              "doc",
              "investigate an orders error rate spike and group by signature"),
          new Chunk("log:1", "log", "orders ERROR order failed signature=payment_gateway_timeout"),
          new Chunk("log:2", "log", "inventory INFO stock adjusted available=6"),
          new Chunk("doc:weather#1", "doc", "the weather is sunny in paris today"));

  @Test
  void retrievesRollbackRunbookForRollbackQuestion() {
    HybridRetriever retriever = new HybridRetriever(new HashEmbedder(256), corpus);

    List<Scored> hits = retriever.retrieve("how do I roll back inventory", 3);

    assertThat(hits).extracting(s -> s.chunk().id()).contains("doc:rollback-inventory#1");
  }

  @Test
  void respectsTopK() {
    HybridRetriever retriever = new HybridRetriever(new HashEmbedder(256), corpus);

    List<Scored> hits = retriever.retrieve("orders error spike", 2);

    assertThat(hits).hasSizeLessThanOrEqualTo(2);
  }

  @Test
  void allRetrievedChunksComeFromCorpus() {
    HybridRetriever retriever = new HybridRetriever(new HashEmbedder(256), corpus);

    List<Scored> hits = retriever.retrieve("orders error spike payment", 4);

    assertThat(hits)
        .extracting(s -> s.chunk().id())
        .isSubsetOf(corpus.stream().map(Chunk::id).toList());
  }
}
