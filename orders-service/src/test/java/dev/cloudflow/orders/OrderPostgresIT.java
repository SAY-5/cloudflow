package dev.cloudflow.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Exercises the orders persistence layer against a real Postgres via Testcontainers.
 *
 * <p>The container is managed manually rather than with {@code @Testcontainers} so the whole class
 * is skipped (not failed) when no Docker daemon is reachable. CI always has Docker.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=update")
class OrderPostgresIT {

  static PostgreSQLContainer<?> postgres;

  @BeforeAll
  static void startContainer() {
    assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available; skipping Postgres integration test");
    postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("orders");
    postgres.start();
  }

  @AfterAll
  static void stopContainer() {
    if (postgres != null) {
      postgres.stop();
    }
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
    registry.add("spring.datasource.username", () -> postgres.getUsername());
    registry.add("spring.datasource.password", () -> postgres.getPassword());
  }

  @Autowired private OrderRepository repository;

  @Test
  void persistsAndReloadsAcrossRepository() {
    OrderEntity saved =
        repository.save(
            new OrderEntity("PG-SKU", 5, "CREATED", Instant.parse("2026-05-26T14:00:00Z")));
    assertThat(saved.getId()).isNotNull();
    assertThat(repository.findById(saved.getId())).isPresent();
    assertThat(repository.findById(saved.getId()).get().getQuantity()).isEqualTo(5);
  }
}
