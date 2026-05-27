package dev.cloudflow.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/** Dashboard-facing aggregation and proxy API. */
@RestController
@RequestMapping("/api")
public class DashboardController {

  private final HealthClient healthClient;
  private final GatewayProperties properties;
  private final WebClient webClient;

  public DashboardController(
      HealthClient healthClient, GatewayProperties properties, WebClient.Builder builder) {
    this.healthClient = healthClient;
    this.properties = properties;
    this.webClient = builder.build();
  }

  public record PlatformHealth(HealthStatus status, List<ServiceHealth> services) {}

  @GetMapping("/health")
  public PlatformHealth platformHealth() {
    List<ServiceHealth> services = healthClient.checkAll();
    return new PlatformHealth(HealthAggregator.rollup(services), services);
  }

  /** Proxies the dashboard's log query to the log-collector. */
  @GetMapping("/logs")
  public List<?> logs() {
    String base = properties.getServices().getOrDefault("collector", "http://localhost:8083");
    return webClient
        .get()
        .uri(base + "/v1/ingest/logs")
        .retrieve()
        .bodyToMono(List.class)
        .onErrorReturn(List.of())
        .block();
  }

  /** Proxies an assistant question to the ops-assistant. */
  @PostMapping(value = "/assist", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Map<?, ?> assist(@RequestBody Map<String, String> body) {
    String base = properties.getServices().getOrDefault("assistant", "http://localhost:8084");
    return webClient
        .post()
        .uri(base + "/v1/assist")
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class)
        .block();
  }
}
