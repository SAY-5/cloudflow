package dev.cloudflow.assistant.llm;

import java.util.List;

/**
 * Live provider stub for Anthropic's Claude.
 *
 * <p>This is the seam where a real API call would go. It is never selected in CI: the assistant
 * defaults to {@link FakeLlmProvider} unless {@code cloudflow.assist.provider=claude} and an API
 * key are configured. The grounding prompt built here is the same one the fake provider is given,
 * so the citation invariant holds for either provider.
 */
public class ClaudeLlmProvider implements LlmProvider {

  private final String apiKey;
  private final String model;

  public ClaudeLlmProvider(String apiKey, String model) {
    this.apiKey = apiKey;
    this.model = model;
  }

  @Override
  public Answer answer(String question, List<GroundingContext> context) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "ClaudeLlmProvider requires cloudflow.assist.claude.api-key; "
              + "use the fake provider in CI");
    }
    // A real implementation would POST the grounded prompt to the Messages API using
    // model=" + model + " and parse the cited ids out of the response. Left as a stub so CI stays
    // hermetic and no network call is ever made.
    throw new UnsupportedOperationException(
        "live Claude calls are not enabled in this build (model=" + model + ")");
  }

  public static String buildGroundedPrompt(String question, List<GroundingContext> context) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are CloudFlow's ops-assistant. Answer the operator's question using ONLY the\n")
        .append("context below. Cite the [id] of every entry you use. Do not invent citations.\n\n")
        .append("Question: ")
        .append(question)
        .append("\n\nContext:\n");
    for (GroundingContext c : context) {
      sb.append("[")
          .append(c.id())
          .append("] (")
          .append(c.source())
          .append(") ")
          .append(c.text())
          .append('\n');
    }
    return sb.toString();
  }
}
