package dev.cloudflow.common.embed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashEmbedderTest {

  private final HashEmbedder embedder = new HashEmbedder(256);

  @Test
  void isDeterministic() {
    float[] a = embedder.embed("orders error rate spike at 14:00");
    float[] b = embedder.embed("orders error rate spike at 14:00");
    assertThat(a).containsExactly(b, org.assertj.core.data.Offset.offset(0.0f));
  }

  @Test
  void vectorsAreUnitNormalized() {
    float[] v = embedder.embed("inventory rollback runbook helm");
    double norm = Math.sqrt(Vectors.cosine(v, v) * dot(v, v) / Vectors.cosine(v, v));
    assertThat(dot(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
  }

  @Test
  void similarTextScoresHigherThanUnrelated() {
    float[] q = embedder.embed("how do I roll back inventory");
    float[] related = embedder.embed("to roll back inventory run helm rollback");
    float[] unrelated = embedder.embed("the weather is sunny today in paris");
    assertThat(Vectors.cosine(q, related)).isGreaterThan(Vectors.cosine(q, unrelated));
  }

  private static double dot(float[] a, float[] b) {
    double d = 0.0;
    for (int i = 0; i < a.length; i++) {
      d += (double) a[i] * b[i];
    }
    return d;
  }
}
