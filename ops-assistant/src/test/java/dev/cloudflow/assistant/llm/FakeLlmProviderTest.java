package dev.cloudflow.assistant.llm;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.assistant.llm.LlmProvider.Answer;
import dev.cloudflow.assistant.llm.LlmProvider.GroundingContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeLlmProviderTest {

  private final FakeLlmProvider provider = new FakeLlmProvider();

  @Test
  void onlyCitesIdsItWasGiven() {
    List<GroundingContext> context =
        List.of(
            new GroundingContext("doc:rollback-inventory#1", "doc", "run helm rollback inventory"),
            new GroundingContext("log:1", "log", "inventory ERROR rollout failed"));

    Answer answer = provider.answer("how do I roll back inventory", context);

    assertThat(answer.citedIds()).isSubsetOf(List.of("doc:rollback-inventory#1", "log:1"));
    assertThat(answer.text()).contains("doc:rollback-inventory#1");
  }

  @Test
  void emptyContextYieldsNoCitations() {
    Answer answer = provider.answer("anything", List.of());
    assertThat(answer.citedIds()).isEmpty();
  }
}
