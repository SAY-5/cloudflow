package dev.cloudflow.common.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a markdown runbook into section chunks keyed by a slug and an ordinal.
 *
 * <p>A new chunk starts at each heading line. The heading text is kept with its body so a citation
 * carries enough context to be useful on its own.
 */
public final class MarkdownChunker {

  private MarkdownChunker() {}

  public static List<Chunk> chunk(String slug, String markdown) {
    List<Chunk> chunks = new ArrayList<>();
    if (markdown == null || markdown.isBlank()) {
      return chunks;
    }
    StringBuilder current = new StringBuilder();
    int ordinal = 0;
    for (String line : markdown.split("\n", -1)) {
      boolean isHeading = line.startsWith("#");
      if (isHeading && current.toString().isBlank()) {
        current.append(line).append('\n');
        continue;
      }
      if (isHeading) {
        chunks.add(new Chunk("doc:" + slug + "#" + ordinal, "doc", current.toString().strip()));
        ordinal++;
        current.setLength(0);
      }
      current.append(line).append('\n');
    }
    if (!current.toString().isBlank()) {
      chunks.add(new Chunk("doc:" + slug + "#" + ordinal, "doc", current.toString().strip()));
    }
    return chunks;
  }
}
