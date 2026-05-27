package dev.cloudflow.gateway;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard-facing aggregation API. */
@RestController
@RequestMapping("/api")
public class DashboardController {

  private final HealthClient healthClient;

  public DashboardController(HealthClient healthClient) {
    this.healthClient = healthClient;
  }

  public record PlatformHealth(HealthStatus status, List<ServiceHealth> services) {}

  @GetMapping("/health")
  public PlatformHealth platformHealth() {
    List<ServiceHealth> services = healthClient.checkAll();
    return new PlatformHealth(HealthAggregator.rollup(services), services);
  }
}
