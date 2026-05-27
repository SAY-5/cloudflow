package dev.cloudflow.common.rag;

import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.Vectors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * In-memory hybrid retriever: ranks chunks by vector cosine and by keyword overlap, then fuses the
 * two rankings with RRF.
 *
 * <p>It is backed by an explicit chunk list so tests and benchmarks can run without a database. The
 * production path loads the same chunks from Postgres + pgvector and reuses {@link Rrf} on the two
 * SQL-side rankings.
 */
public final class HybridRetriever implements Retriever {

  private final Embedder embedder;
  private final List<Chunk> chunks;
  private final float[][] vectors;

  public HybridRetriever(Embedder embedder, List<Chunk> chunks) {
    this.embedder = embedder;
    this.chunks = List.copyOf(chunks);
    this.vectors = new float[this.chunks.size()][];
    for (int i = 0; i < this.chunks.size(); i++) {
      this.vectors[i] = embedder.embed(this.chunks.get(i).text());
    }
  }

  @Override
  public List<Scored> retrieve(String question, int topK) {
    if (topK <= 0) {
      throw new IllegalArgumentException("topK must be positive");
    }
    List<Chunk> vectorRanked = rankByVector(question, topK * 2);
    List<Chunk> keywordRanked = rankByKeyword(question, topK * 2);
    List<Scored> fused = Rrf.fuse(vectorRanked, keywordRanked);
    return fused.size() > topK ? fused.subList(0, topK) : fused;
  }

  private List<Chunk> rankByVector(String question, int limit) {
    float[] q = embedder.embed(question);
    record IndexScore(int idx, double score) {}
    List<IndexScore> scored = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
      scored.add(new IndexScore(i, Vectors.cosine(q, vectors[i])));
    }
    scored.sort(Comparator.comparingDouble(IndexScore::score).reversed());
    List<Chunk> out = new ArrayList<>();
    for (int i = 0; i < Math.min(limit, scored.size()); i++) {
      out.add(chunks.get(scored.get(i).idx()));
    }
    return out;
  }

  private List<Chunk> rankByKeyword(String question, int limit) {
    Set<String> qTokens = tokens(question);
    record IndexScore(int idx, int score) {}
    List<IndexScore> scored = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
      Set<String> cTokens = tokens(chunks.get(i).text());
      int overlap = 0;
      for (String t : qTokens) {
        if (cTokens.contains(t)) {
          overlap++;
        }
      }
      if (overlap > 0) {
        scored.add(new IndexScore(i, overlap));
      }
    }
    scored.sort(
        Comparator.comparingInt(IndexScore::score)
            .reversed()
            .thenComparing(is -> chunks.get(is.idx()).id()));
    List<Chunk> out = new ArrayList<>();
    for (int i = 0; i < Math.min(limit, scored.size()); i++) {
      out.add(chunks.get(scored.get(i).idx()));
    }
    return out;
  }

  private static Set<String> tokens(String text) {
    Set<String> set = new HashSet<>();
    for (String t : text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+")) {
      if (t.length() > 1) {
        set.add(t);
      }
    }
    return set;
  }
}
