package dev.cloudflow.gateway;

import java.util.List;

/**
 * Rolls per-service health into a single platform status.
 *
 * <p>The rollup is a decision table over how many services are up:
 *
 * <ul>
 *   <li>all services up: {@link HealthStatus#UP}
 *   <li>some up, some down: {@link HealthStatus#DEGRADED}
 *   <li>all services down: {@link HealthStatus#DOWN}
 *   <li>no services reported: {@link HealthStatus#DOWN}
 * </ul>
 */
public final class HealthAggregator {

  private HealthAggregator() {}

  public static HealthStatus rollup(List<ServiceHealth> services) {
    if (services == null || services.isEmpty()) {
      return HealthStatus.DOWN;
    }
    long up = services.stream().filter(ServiceHealth::up).count();
    if (up == services.size()) {
      return HealthStatus.UP;
    }
    if (up == 0) {
      return HealthStatus.DOWN;
    }
    return HealthStatus.DEGRADED;
  }
}
