package dev.cloudflow.common.embed;

/** Small helpers for dense float vectors. */
public final class Vectors {

  private Vectors() {}

  /** Cosine similarity. Assumes both vectors are the same length. */
  public static double cosine(float[] a, float[] b) {
    if (a.length != b.length) {
      throw new IllegalArgumentException("dimension mismatch: " + a.length + " vs " + b.length);
    }
    double dot = 0.0;
    double na = 0.0;
    double nb = 0.0;
    for (int i = 0; i < a.length; i++) {
      dot += (double) a[i] * b[i];
      na += (double) a[i] * a[i];
      nb += (double) b[i] * b[i];
    }
    if (na == 0.0 || nb == 0.0) {
      return 0.0;
    }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
  }

  /** Renders a vector as the pgvector literal form {@code [a,b,c]}. */
  public static String toPgVector(float[] v) {
    StringBuilder sb = new StringBuilder(v.length * 8);
    sb.append('[');
    for (int i = 0; i < v.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(v[i]);
    }
    sb.append(']');
    return sb.toString();
  }
}
