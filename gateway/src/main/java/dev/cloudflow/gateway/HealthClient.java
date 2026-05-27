package dev.cloudflow.gateway;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Calls {@code /actuator/health} on each downstream service and reports per-service status. */
@Component
public class HealthClient {

  private final GatewayProperties properties;
  private final WebClient webClient;
  private final Duration timeout;

  @Autowired
  public HealthClient(GatewayProperties properties, WebClient.Builder builder) {
    this(properties, builder, Duration.ofSeconds(2));
  }

  HealthClient(GatewayProperties properties, WebClient.Builder builder, Duration timeout) {
    this.properties = properties;
    this.webClient = builder.build();
    this.timeout = timeout;
  }

  public List<ServiceHealth> checkAll() {
    return Flux.fromIterable(properties.getServices().entrySet())
        .flatMap(e -> check(e.getKey(), e.getValue()))
        .collectList()
        .block();
  }

  private Mono<ServiceHealth> check(String name, String baseUrl) {
    return webClient
        .get()
        .uri(baseUrl + "/actuator/health")
        .retrieve()
        .bodyToMono(Map.class)
        .timeout(timeout)
        .map(body -> "UP".equals(String.valueOf(body.get("status"))))
        .map(up -> up ? ServiceHealth.up(name) : ServiceHealth.down(name, "status not UP"))
        .onErrorResume(err -> Mono.just(ServiceHealth.down(name, err.getClass().getSimpleName())));
  }
}
