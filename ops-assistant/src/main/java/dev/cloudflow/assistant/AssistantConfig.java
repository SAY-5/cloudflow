package dev.cloudflow.assistant;

import dev.cloudflow.assistant.llm.ClaudeLlmProvider;
import dev.cloudflow.assistant.llm.FakeLlmProvider;
import dev.cloudflow.assistant.llm.LlmProvider;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.HashEmbedder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfig {

  @Bean
  public Embedder embedder(@Value("${cloudflow.embed.dimension:256}") int dimension) {
    return new HashEmbedder(dimension);
  }

  @Bean
  public LlmProvider llmProvider(
      @Value("${cloudflow.assist.provider:fake}") String provider,
      @Value("${cloudflow.assist.claude.api-key:}") String apiKey,
      @Value("${cloudflow.assist.claude.model:claude-sonnet-4-6}") String model) {
    if ("claude".equalsIgnoreCase(provider)) {
      return new ClaudeLlmProvider(apiKey, model);
    }
    return new FakeLlmProvider();
  }
}
