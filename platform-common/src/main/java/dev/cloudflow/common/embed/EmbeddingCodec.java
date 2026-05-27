package dev.cloudflow.common.embed;

/** Serializes dense float vectors to and from a compact comma-separated string form. */
public final class EmbeddingCodec {

  private EmbeddingCodec() {}

  public static String encode(float[] vec) {
    StringBuilder sb = new StringBuilder(vec.length * 8);
    for (int i = 0; i < vec.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(vec[i]);
    }
    return sb.toString();
  }

  public static float[] decode(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return new float[0];
    }
    String[] parts = encoded.split(",");
    float[] vec = new float[parts.length];
    for (int i = 0; i < parts.length; i++) {
      vec[i] = Float.parseFloat(parts[i]);
    }
    return vec;
  }
}
