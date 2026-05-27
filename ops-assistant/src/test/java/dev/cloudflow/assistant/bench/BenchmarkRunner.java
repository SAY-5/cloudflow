package dev.cloudflow.assistant.bench;

import dev.cloudflow.assistant.CitationGrounder;
import dev.cloudflow.assistant.llm.FakeLlmProvider;
import dev.cloudflow.assistant.llm.LlmProvider;
import dev.cloudflow.assistant.llm.LlmProvider.Answer;
import dev.cloudflow.assistant.llm.LlmProvider.GroundingContext;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.embed.EmbeddingCodec;
import dev.cloudflow.common.embed.HashEmbedder;
import dev.cloudflow.common.rag.Chunk;
import dev.cloudflow.common.rag.HybridRetriever;
import dev.cloudflow.common.rag.Scored;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Drives the ingest and assist paths to measure throughput and latency.
 *
 * <p>LLM time is excluded: the assist loop uses {@link FakeLlmProvider}, so what is measured is
 * retrieval plus citation grounding, which is the part of the pipeline CloudFlow owns.
 */
public final class BenchmarkRunner {

  private static final String[] SERVICES = {"orders", "inventory", "gateway"};
  private static final String[] LEVELS = {"INFO", "INFO", "INFO", "WARN", "ERROR"};
  private static final String[] SIGNATURES = {
    "payment_gateway_timeout", "negative_stock", "downstream_5xx", "slow_query", "ok"
  };

  private final Embedder embedder;
  private final LlmProvider provider;
  private final int topK;

  public BenchmarkRunner(int dimension, int topK) {
    this.embedder = new HashEmbedder(dimension);
    this.provider = new FakeLlmProvider();
    this.topK = topK;
  }

  public record Result(
      int logCount, double ingestSeconds, double ingestPerSecond, LatencyStats assistLatency) {}

  public Result run(int logCount, int queryCount, long seed) {
    Random rnd = new Random(seed);

    List<Chunk> corpus = new ArrayList<>(logCount);
    long ingestStart = System.nanoTime();
    for (int i = 0; i < logCount; i++) {
      String text = syntheticLog(rnd, i);
      // Embed + encode is the work the collector does per line on ingest.
      String encoded = EmbeddingCodec.encode(embedder.embed(text));
      if (encoded.isEmpty()) {
        throw new IllegalStateException("empty embedding");
      }
      corpus.add(new Chunk("log:" + i, "log", text));
    }
    double ingestSeconds = (System.nanoTime() - ingestStart) / 1_000_000_000.0;

    HybridRetriever retriever = new HybridRetriever(embedder, corpus);
    String[] questions = {
      "why did orders error rate spike",
      "how do I roll back inventory",
      "what caused the downstream 5xx errors",
      "negative stock on which sku",
      "slow query in gateway"
    };

    long[] samples = new long[queryCount];
    for (int q = 0; q < queryCount; q++) {
      String question = questions[q % questions.length];
      long start = System.nanoTime();
      List<Scored> retrieved = retriever.retrieve(question, topK);
      List<GroundingContext> context = new ArrayList<>(retrieved.size());
      java.util.Set<String> ids = new java.util.HashSet<>();
      for (Scored s : retrieved) {
        context.add(new GroundingContext(s.chunk().id(), s.chunk().source(), s.chunk().text()));
        ids.add(s.chunk().id());
      }
      Answer answer = provider.answer(question, context);
      CitationGrounder.verify(answer.citedIds(), ids);
      samples[q] = System.nanoTime() - start;
    }

    return new Result(
        logCount,
        ingestSeconds,
        logCount / Math.max(ingestSeconds, 1e-9),
        LatencyStats.of(samples));
  }

  private static String syntheticLog(Random rnd, int i) {
    String svc = SERVICES[rnd.nextInt(SERVICES.length)];
    String level = LEVELS[rnd.nextInt(LEVELS.length)];
    String sig = SIGNATURES[rnd.nextInt(SIGNATURES.length)];
    return svc
        + " "
        + level
        + " request "
        + i
        + " signature="
        + sig
        + " latency_ms="
        + rnd.nextInt(500);
  }
}
