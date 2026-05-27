package dev.cloudflow.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.cloudflow.assistant.CitationGrounder.UngroundedCitationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CitationGrounderTest {

  @Test
  void acceptsCitationsWithinRetrievedSet() {
    CitationGrounder.verify(List.of("log:1", "doc:x#0"), Set.of("log:1", "doc:x#0", "log:2"));
    assertThat(CitationGrounder.isGrounded(List.of("log:1"), Set.of("log:1"))).isTrue();
  }

  @Test
  void rejectsACitationOutsideTheRetrievedSet() {
    assertThatThrownBy(() -> CitationGrounder.verify(List.of("log:99"), Set.of("log:1")))
        .isInstanceOf(UngroundedCitationException.class)
        .hasMessageContaining("log:99");
  }

  @Test
  void emptyCitationsAreTriviallyGrounded() {
    CitationGrounder.verify(List.of(), Set.of("log:1"));
    assertThat(CitationGrounder.isGrounded(List.of(), Set.of())).isTrue();
  }
}
