package dev.cloudflow.inventory;

import dev.cloudflow.common.log.StructuredLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

  @Bean
  public StructuredLogger structuredLogger() {
    return StructuredLogger.toStdout("inventory");
  }
}
