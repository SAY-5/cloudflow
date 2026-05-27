package dev.cloudflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HealthAggregatorTest {

  @Test
  void allUpIsUp() {
    assertThat(
            HealthAggregator.rollup(
                List.of(ServiceHealth.up("orders"), ServiceHealth.up("inventory"))))
        .isEqualTo(HealthStatus.UP);
  }

  @Test
  void someDownIsDegraded() {
    assertThat(
            HealthAggregator.rollup(
                List.of(ServiceHealth.up("orders"), ServiceHealth.down("inventory", "timeout"))))
        .isEqualTo(HealthStatus.DEGRADED);
  }

  @Test
  void allDownIsDown() {
    assertThat(
            HealthAggregator.rollup(
                List.of(ServiceHealth.down("orders", "x"), ServiceHealth.down("inventory", "y"))))
        .isEqualTo(HealthStatus.DOWN);
  }

  @Test
  void emptyIsDown() {
    assertThat(HealthAggregator.rollup(List.of())).isEqualTo(HealthStatus.DOWN);
  }
}
