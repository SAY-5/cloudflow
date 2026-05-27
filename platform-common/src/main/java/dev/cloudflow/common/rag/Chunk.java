package dev.cloudflow.common.rag;

/**
 * A retrievable unit of grounding context: either a log line or a runbook section.
 *
 * @param id stable identifier, e.g. {@code log:42} or {@code doc:rollback-inventory#2}
 * @param source one of {@code log} or {@code doc}
 * @param text the content used for embedding and shown as a citation
 */
public record Chunk(String id, String source, String text) {

  public Chunk {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id is required");
    }
    if (!"log".equals(source) && !"doc".equals(source)) {
      throw new IllegalArgumentException("source must be 'log' or 'doc', was: " + source);
    }
  }
}
