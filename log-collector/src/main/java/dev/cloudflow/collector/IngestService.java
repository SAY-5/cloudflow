package dev.cloudflow.collector;

import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.EmbeddingCodec;
import dev.cloudflow.common.log.LogEntry;
import dev.cloudflow.common.log.LogParser;
import dev.cloudflow.common.rag.Chunk;
import dev.cloudflow.common.rag.MarkdownChunker;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Embeds and stores structured log lines and runbook markdown into the shared RAG store. */
@Service
public class IngestService {

  private final RagChunkRepository repository;
  private final Embedder embedder;
  private final AtomicLong logSeq = new AtomicLong();

  public IngestService(RagChunkRepository repository, Embedder embedder) {
    this.repository = repository;
    this.embedder = embedder;
  }

  /** Ingests one raw JSON log line. Returns the stored chunk id. */
  @Transactional
  public String ingestLogLine(String rawJson) {
    LogEntry entry = LogParser.parse(rawJson);
    long seq = logSeq.incrementAndGet();
    String id = "log:" + seq;
    String text = renderLog(entry);
    String embedding = EmbeddingCodec.encode(embedder.embed(text));
    repository.save(new RagChunkEntity(id, "log", text, embedding, entry.service(), entry.ts()));
    return id;
  }

  /** Ingests a batch of raw JSON log lines. Returns how many were stored. */
  @Transactional
  public int ingestLogLines(List<String> rawJsonLines) {
    int count = 0;
    for (String line : rawJsonLines) {
      ingestLogLine(line);
      count++;
    }
    return count;
  }

  /** Chunks and ingests a runbook markdown document under the given slug. */
  @Transactional
  public int ingestRunbook(String slug, String markdown) {
    List<Chunk> chunks = MarkdownChunker.chunk(slug, markdown);
    for (Chunk chunk : chunks) {
      String embedding = EmbeddingCodec.encode(embedder.embed(chunk.text()));
      repository.save(new RagChunkEntity(chunk.id(), "doc", chunk.text(), embedding, null, null));
    }
    return chunks.size();
  }

  static String renderLog(LogEntry entry) {
    StringBuilder sb = new StringBuilder();
    sb.append(entry.service()).append(' ').append(entry.level()).append(' ').append(entry.msg());
    entry.fields().forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
    return sb.toString();
  }
}
