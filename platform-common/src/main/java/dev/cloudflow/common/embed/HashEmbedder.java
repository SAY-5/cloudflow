package dev.cloudflow.common.embed;

import java.util.Locale;

/**
 * Deterministic, dependency-free embedder used everywhere in CloudFlow.
 *
 * <p>Tokens are lower-cased, hashed into one of {@code dimension} buckets via the FNV-1a hash, and
 * accumulated with a signed weight. The resulting vector is L2-normalized so cosine similarity is a
 * dot product. Being deterministic, it lets CI exercise the full embed-store-retrieve pipeline
 * without a real model.
 */
public final class HashEmbedder implements Embedder {

  private static final int DEFAULT_DIM = 256;
  private static final long FNV_OFFSET = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x100000001b3L;

  private final int dim;

  public HashEmbedder() {
    this(DEFAULT_DIM);
  }

  public HashEmbedder(int dim) {
    if (dim <= 0) {
      throw new IllegalArgumentException("dimension must be positive");
    }
    this.dim = dim;
  }

  @Override
  public int dimension() {
    return dim;
  }

  @Override
  public float[] embed(String text) {
    float[] vec = new float[dim];
    if (text == null || text.isBlank()) {
      vec[0] = 1.0f;
      return vec;
    }
    for (String token : tokenize(text)) {
      long h = hash(token);
      int bucket = (int) Math.floorMod(h, dim);
      // Sign derived from a distinct bit so collisions can cancel rather than always add.
      float sign = ((h >>> 33) & 1L) == 0L ? 1.0f : -1.0f;
      vec[bucket] += sign;
    }
    normalize(vec);
    return vec;
  }

  private static String[] tokenize(String text) {
    return text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+");
  }

  private static long hash(String token) {
    long h = FNV_OFFSET;
    for (int i = 0; i < token.length(); i++) {
      h ^= token.charAt(i);
      h *= FNV_PRIME;
    }
    return h;
  }

  private static void normalize(float[] vec) {
    double sum = 0.0;
    for (float v : vec) {
      sum += (double) v * v;
    }
    if (sum == 0.0) {
      vec[0] = 1.0f;
      return;
    }
    float norm = (float) Math.sqrt(sum);
    for (int i = 0; i < vec.length; i++) {
      vec[i] /= norm;
    }
  }
}
