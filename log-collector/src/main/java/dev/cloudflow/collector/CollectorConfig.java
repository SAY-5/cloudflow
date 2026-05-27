package dev.cloudflow.collector;

import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.HashEmbedder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollectorConfig {

  @Bean
  public Embedder embedder(@Value("${cloudflow.embed.dimension:256}") int dimension) {
    return new HashEmbedder(dimension);
  }
}
