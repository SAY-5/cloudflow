package dev.cloudflow.common.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class RrfPropertyTest {

  @Provide
  Arbitrary<List<Chunk>> chunkLists() {
    Arbitrary<String> ids = Arbitraries.integers().between(0, 40).map(i -> "log:" + i);
    return ids.list()
        .ofMaxSize(15)
        .uniqueElements()
        .map(list -> list.stream().map(this::chunk).toList());
  }

  private Chunk chunk(String id) {
    return new Chunk(id, "log", "text " + id);
  }

  @Property(tries = 300)
  void fusionOnlyContainsInputIds(
      @ForAll("chunkLists") List<Chunk> vector, @ForAll("chunkLists") List<Chunk> keyword) {
    Set<String> inputIds = new HashSet<>();
    vector.forEach(c -> inputIds.add(c.id()));
    keyword.forEach(c -> inputIds.add(c.id()));

    List<Scored> fused = Rrf.fuse(vector, keyword);

    assertThat(fused).extracting(s -> s.chunk().id()).allMatch(inputIds::contains);
  }

  @Property(tries = 300)
  void fusionIsSortedByDescendingScore(
      @ForAll("chunkLists") List<Chunk> vector, @ForAll("chunkLists") List<Chunk> keyword) {
    List<Scored> fused = Rrf.fuse(vector, keyword);
    for (int i = 1; i < fused.size(); i++) {
      assertThat(fused.get(i - 1).score()).isGreaterThanOrEqualTo(fused.get(i).score());
    }
  }

  @Property(tries = 300)
  void fusionContainsTheUnionWithoutDuplicates(
      @ForAll("chunkLists") List<Chunk> vector, @ForAll("chunkLists") List<Chunk> keyword) {
    Set<String> union = new HashSet<>();
    vector.forEach(c -> union.add(c.id()));
    keyword.forEach(c -> union.add(c.id()));

    List<Scored> fused = Rrf.fuse(vector, keyword);

    Set<String> fusedIds = new HashSet<>();
    fused.forEach(s -> fusedIds.add(s.chunk().id()));
    assertThat(fusedIds).isEqualTo(union);
    assertThat(fused).hasSize(union.size());
  }
}
