package dev.cloudflow.assistant.llm;

import static org.assertj.core.api.Assertions.assertThat;

import dev.cloudflow.assistant.llm.LlmProvider.Answer;
import dev.cloudflow.assistant.llm.LlmProvider.GroundingContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class FakeLlmProviderPropertyTest {

  private final FakeLlmProvider provider = new FakeLlmProvider();

  @Provide
  Arbitrary<List<GroundingContext>> contexts() {
    Arbitrary<GroundingContext> one =
        Arbitraries.integers()
            .between(0, 100)
            .map(i -> new GroundingContext("log:" + i, "log", "content " + i));
    return one.list().ofMaxSize(10).uniqueElements(GroundingContext::id);
  }

  @Property(tries = 400)
  void neverCitesAnIdItWasNotGiven(
      @ForAll("contexts") List<GroundingContext> context,
      @ForAll @net.jqwik.api.constraints.StringLength(max = 30) String question) {
    Set<String> givenIds = new HashSet<>();
    context.forEach(c -> givenIds.add(c.id()));

    Answer answer = provider.answer(question, context);

    assertThat(answer.citedIds()).allMatch(givenIds::contains);
  }
}
