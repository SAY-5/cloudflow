package dev.cloudflow.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.cloudflow.common.embed.EmbeddingCodec;
import dev.cloudflow.common.embed.HashEmbedder;
import dev.cloudflow.common.embed.Vectors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Validates that the embeddings work against a real pgvector column: store two vectors and confirm
 * the database's own cosine-distance ordering agrees with the in-app cosine. Skips when no Docker.
 */
class PgVectorIT {

  static PostgreSQLContainer<?> postgres;

  @BeforeAll
  static void start() {
    assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available; skipping pgvector integration test");
    postgres =
        new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
    postgres.start();
  }

  @AfterAll
  static void stop() {
    if (postgres != null) {
      postgres.stop();
    }
  }

  @Test
  void pgvectorCosineOrderingMatchesInAppCosine() throws Exception {
    HashEmbedder embedder = new HashEmbedder(256);
    float[] query = embedder.embed("how do I roll back inventory");
    float[] related = embedder.embed("to roll back inventory run helm rollback inventory");
    float[] unrelated = embedder.embed("the weather is sunny in paris today");

    // In-app cosine: related should beat unrelated.
    assertThat(Vectors.cosine(query, related)).isGreaterThan(Vectors.cosine(query, unrelated));

    try (Connection conn =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement st = conn.createStatement()) {
      st.execute("CREATE EXTENSION IF NOT EXISTS vector");
      st.execute("CREATE TABLE emb (id text primary key, v vector(256))");
      insert(conn, "related", related);
      insert(conn, "unrelated", unrelated);

      try (ResultSet rs =
          st.executeQuery(
              "SELECT id FROM emb ORDER BY v <=> '"
                  + Vectors.toPgVector(query)
                  + "' ASC LIMIT 1")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("id")).isEqualTo("related");
      }
    }
  }

  private static void insert(Connection conn, String id, float[] vec) throws Exception {
    try (var ps = conn.prepareStatement("INSERT INTO emb (id, v) VALUES (?, ?::vector)")) {
      ps.setString(1, id);
      ps.setString(2, Vectors.toPgVector(vec));
      ps.executeUpdate();
    }
    // touch codec so it is covered by this module too
    EmbeddingCodec.decode(EmbeddingCodec.encode(vec));
  }
}
