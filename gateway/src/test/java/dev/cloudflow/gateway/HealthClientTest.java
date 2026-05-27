package dev.cloudflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class HealthClientTest {

  private MockWebServer upServer;
  private MockWebServer downServer;

  @BeforeEach
  void setUp() throws IOException {
    upServer = new MockWebServer();
    upServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"status\":\"UP\"}"));
    upServer.start();

    downServer = new MockWebServer();
    downServer.enqueue(new MockResponse().setResponseCode(503).setBody("{\"status\":\"DOWN\"}"));
    downServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    upServer.shutdown();
    downServer.shutdown();
  }

  @Test
  void reportsUpAndDownFromDownstreamHealth() {
    GatewayProperties props = new GatewayProperties();
    props.setServices(
        Map.of(
            "orders", upServer.url("/").toString().replaceAll("/$", ""),
            "inventory", downServer.url("/").toString().replaceAll("/$", "")));

    HealthClient client = new HealthClient(props, WebClient.builder());
    List<ServiceHealth> health = client.checkAll();

    assertThat(health).hasSize(2);
    assertThat(health.stream().filter(ServiceHealth::up).map(ServiceHealth::service))
        .contains("orders");
    assertThat(health.stream().filter(h -> !h.up()).map(ServiceHealth::service))
        .contains("inventory");
    assertThat(HealthAggregator.rollup(health)).isEqualTo(HealthStatus.DEGRADED);
  }
}
