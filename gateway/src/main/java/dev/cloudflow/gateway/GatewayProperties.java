package dev.cloudflow.gateway;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Maps {@code cloudflow.services.<name>=<base-url>} into a lookup table. */
@ConfigurationProperties(prefix = "cloudflow")
public class GatewayProperties {

  private Map<String, String> services = new LinkedHashMap<>();

  public Map<String, String> getServices() {
    return services;
  }

  public void setServices(Map<String, String> services) {
    this.services = services;
  }
}
