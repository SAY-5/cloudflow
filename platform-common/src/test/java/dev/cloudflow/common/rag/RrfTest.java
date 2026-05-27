package dev.cloudflow.common.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RrfTest {

  private static Chunk c(String id) {
    return new Chunk(id, "log", "text for " + id);
  }

  @Test
  void itemRankedHighInBothListsWinsFusion() {
    List<Chunk> vector = List.of(c("log:1"), c("log:2"), c("log:3"));
    List<Chunk> keyword = List.of(c("log:1"), c("log:4"), c("log:5"));

    List<Scored> fused = Rrf.fuse(vector, keyword);

    assertThat(fused.get(0).chunk().id()).isEqualTo("log:1");
  }

  @Test
  void unionsBothLists() {
    List<Chunk> vector = List.of(c("log:1"));
    List<Chunk> keyword = List.of(c("log:2"));

    List<Scored> fused = Rrf.fuse(vector, keyword);

    assertThat(fused).extracting(s -> s.chunk().id()).containsExactlyInAnyOrder("log:1", "log:2");
  }

  @Test
  void scoresAreDescending() {
    List<Chunk> vector = List.of(c("log:1"), c("log:2"), c("log:3"));
    List<Chunk> keyword = List.of(c("log:3"), c("log:2"), c("log:1"));

    List<Scored> fused = Rrf.fuse(vector, keyword);

    for (int i = 1; i < fused.size(); i++) {
      assertThat(fused.get(i - 1).score()).isGreaterThanOrEqualTo(fused.get(i).score());
    }
  }
}
