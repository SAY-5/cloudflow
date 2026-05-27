package dev.cloudflow.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.cloudflow.assistant.CitationGrounder.UngroundedCitationException;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class CitationGrounderPropertyTest {

  @Provide
  Arbitrary<List<String>> idLists() {
    return Arbitraries.integers().between(0, 50).map(i -> "log:" + i).list().ofMaxSize(12);
  }

  @Property(tries = 500)
  void acceptsExactlyWhenCitedIsSubsetOfRetrieved(
      @ForAll("idLists") List<String> retrieved, @ForAll("idLists") List<String> cited) {
    Set<String> retrievedSet = Set.copyOf(retrieved);
    boolean subset = retrievedSet.containsAll(cited);

    if (subset) {
      CitationGrounder.verify(cited, retrievedSet);
      assertThat(CitationGrounder.isGrounded(cited, retrievedSet)).isTrue();
    } else {
      assertThatThrownBy(() -> CitationGrounder.verify(cited, retrievedSet))
          .isInstanceOf(UngroundedCitationException.class);
      assertThat(CitationGrounder.isGrounded(cited, retrievedSet)).isFalse();
    }
  }

  @Property(tries = 500)
  void groundedIsConsistentWithVerify(
      @ForAll("idLists") List<String> retrieved, @ForAll("idLists") List<String> cited) {
    Set<String> retrievedSet = Set.copyOf(retrieved);
    boolean grounded = CitationGrounder.isGrounded(cited, retrievedSet);
    boolean threw;
    try {
      CitationGrounder.verify(cited, retrievedSet);
      threw = false;
    } catch (UngroundedCitationException e) {
      threw = true;
    }
    assertThat(grounded).isEqualTo(!threw);
  }
}
