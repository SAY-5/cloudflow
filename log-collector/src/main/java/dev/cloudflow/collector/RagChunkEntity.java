package dev.cloudflow.collector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A stored, embedded chunk shared by the collector (writer) and the assistant (reader).
 *
 * <p>The embedding is persisted as a comma-separated string so the same schema works on both H2
 * (unit tests) and Postgres. The pgvector integration test stores the real {@code vector} column
 * separately; this entity holds the portable copy used by the in-app retriever.
 */
@Entity
@Table(name = "rag_chunk")
public class RagChunkEntity {

  @Id private String id;

  @Column(nullable = false)
  private String source;

  @Lob
  @Column(nullable = false, length = 16384)
  private String content;

  @Lob
  @Column(name = "embedding", nullable = false, length = 16384)
  private String embedding;

  @Column(name = "service")
  private String service;

  @Column(name = "ts")
  private Instant ts;

  protected RagChunkEntity() {}

  public RagChunkEntity(
      String id, String source, String content, String embedding, String service, Instant ts) {
    this.id = id;
    this.source = source;
    this.content = content;
    this.embedding = embedding;
    this.service = service;
    this.ts = ts;
  }

  public String getId() {
    return id;
  }

  public String getSource() {
    return source;
  }

  public String getContent() {
    return content;
  }

  public String getEmbedding() {
    return embedding;
  }

  public String getService() {
    return service;
  }

  public Instant getTs() {
    return ts;
  }
}
