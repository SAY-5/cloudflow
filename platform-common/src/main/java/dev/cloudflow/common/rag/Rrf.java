package dev.cloudflow.common.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion of two ranked candidate lists.
 *
 * <p>For a chunk appearing at rank {@code r} (0-based) in a list, it contributes {@code 1 / (k + r
 * + 1)} to its fused score. Scores from both lists are summed, then chunks are sorted by descending
 * fused score with the chunk id as a deterministic tie-break.
 */
public final class Rrf {

  public static final int DEFAULT_K = 60;

  private Rrf() {}

  public static List<Scored> fuse(List<Chunk> vectorRanked, List<Chunk> keywordRanked) {
    return fuse(vectorRanked, keywordRanked, DEFAULT_K);
  }

  public static List<Scored> fuse(List<Chunk> vectorRanked, List<Chunk> keywordRanked, int k) {
    if (k <= 0) {
      throw new IllegalArgumentException("k must be positive");
    }
    Map<String, Double> scores = new LinkedHashMap<>();
    Map<String, Chunk> byId = new LinkedHashMap<>();
    accumulate(scores, byId, vectorRanked, k);
    accumulate(scores, byId, keywordRanked, k);

    List<Scored> fused = new ArrayList<>();
    for (Map.Entry<String, Double> e : scores.entrySet()) {
      fused.add(new Scored(byId.get(e.getKey()), e.getValue()));
    }
    fused.sort(
        (a, b) -> {
          int byScore = Double.compare(b.score(), a.score());
          return byScore != 0 ? byScore : a.chunk().id().compareTo(b.chunk().id());
        });
    return fused;
  }

  private static void accumulate(
      Map<String, Double> scores, Map<String, Chunk> byId, List<Chunk> ranked, int k) {
    if (ranked == null) {
      return;
    }
    for (int rank = 0; rank < ranked.size(); rank++) {
      Chunk c = ranked.get(rank);
      byId.putIfAbsent(c.id(), c);
      scores.merge(c.id(), 1.0 / (k + rank + 1), Double::sum);
    }
  }
}
