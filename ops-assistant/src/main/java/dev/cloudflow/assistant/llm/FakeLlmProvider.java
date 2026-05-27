package dev.cloudflow.assistant.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic provider used in CI and local runs: it never calls a network model.
 *
 * <p>It builds a grounded answer purely from the supplied context, citing the ids of the entries it
 * draws from. Because it can only reference ids it was handed, it cannot invent a citation, which
 * is exactly the grounding property the assistant must uphold.
 */
public class FakeLlmProvider implements LlmProvider {

  private final int maxCitations;

  public FakeLlmProvider() {
    this(3);
  }

  public FakeLlmProvider(int maxCitations) {
    this.maxCitations = maxCitations;
  }

  @Override
  public Answer answer(String question, List<GroundingContext> context) {
    if (context == null || context.isEmpty()) {
      return new Answer(
          "I could not find any relevant logs or runbooks for that question.", List.of());
    }
    List<GroundingContext> used =
        context.size() > maxCitations ? context.subList(0, maxCitations) : context;

    StringBuilder sb = new StringBuilder();
    sb.append("Based on the retrieved context, here is what I found for: ")
        .append(question.strip())
        .append('\n');
    List<String> cited = new ArrayList<>();
    for (GroundingContext c : used) {
      sb.append("- [").append(c.id()).append("] ").append(firstLine(c.text())).append('\n');
      cited.add(c.id());
    }
    sb.append("Citations: ").append(String.join(", ", cited));
    return new Answer(sb.toString(), List.copyOf(cited));
  }

  private static String firstLine(String text) {
    int nl = text.indexOf('\n');
    String line = nl < 0 ? text : text.substring(0, nl);
    return line.length() > 160 ? line.substring(0, 160) : line;
  }
}
