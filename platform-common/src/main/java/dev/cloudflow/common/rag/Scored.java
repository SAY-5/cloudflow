package dev.cloudflow.common.rag;

/** A chunk paired with a relevance score. */
public record Scored(Chunk chunk, double score) {}
