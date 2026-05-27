package dev.cloudflow.common.embed;

/** Produces a fixed-dimension dense vector for a piece of text. */
public interface Embedder {

  /** The dimensionality of every vector this embedder returns. */
  int dimension();

  /** Embeds the given text into a unit-normalized dense vector. */
  float[] embed(String text);
}
