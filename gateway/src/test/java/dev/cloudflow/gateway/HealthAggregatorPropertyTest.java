package dev.cloudflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

class HealthAggregatorPropertyTest {

  @Property(tries = 500)
  void rollupMatchesTheDecisionTable(@ForAll @Size(min = 1, max = 8) List<Boolean> ups) {
    List<ServiceHealth> services = new ArrayList<>();
    for (int i = 0; i < ups.size(); i++) {
      services.add(ups.get(i) ? ServiceHealth.up("s" + i) : ServiceHealth.down("s" + i, "x"));
    }
    long upCount = ups.stream().filter(b -> b).count();

    HealthStatus status = HealthAggregator.rollup(services);

    if (upCount == ups.size()) {
      assertThat(status).isEqualTo(HealthStatus.UP);
    } else if (upCount == 0) {
      assertThat(status).isEqualTo(HealthStatus.DOWN);
    } else {
      assertThat(status).isEqualTo(HealthStatus.DEGRADED);
    }
  }

  @Property(tries = 200)
  void anyNonEmptyAllUpListIsUp(@ForAll @IntRange(min = 1, max = 10) int n) {
    List<ServiceHealth> services = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      services.add(ServiceHealth.up("s" + i));
    }
    assertThat(HealthAggregator.rollup(services)).isEqualTo(HealthStatus.UP);
  }
}
