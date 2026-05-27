package dev.cloudflow.assistant;

import dev.cloudflow.assistant.llm.LlmProvider;
import dev.cloudflow.assistant.llm.LlmProvider.Answer;
import dev.cloudflow.assistant.llm.LlmProvider.GroundingContext;
import dev.cloudflow.assistant.store.RagChunkEntity;
import dev.cloudflow.assistant.store.RagChunkRepository;
import dev.cloudflow.common.embed.Embedder;
import dev.cloudflow.common.rag.Chunk;
import dev.cloudflow.common.rag.HybridRetriever;
import dev.cloudflow.common.rag.Scored;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Answers operational questions by retrieving grounding context, asking the LLM provider, and
 * verifying every cited id is grounded in the retrieved set.
 */
@Service
public class AssistantService {

  private final RagChunkRepository repository;
  private final Embedder embedder;
  private final LlmProvider llmProvider;
  private final int topK;

  public AssistantService(
      RagChunkRepository repository,
      Embedder embedder,
      LlmProvider llmProvider,
      @Value("${cloudflow.assist.top-k:6}") int topK) {
    this.repository = repository;
    this.embedder = embedder;
    this.llmProvider = llmProvider;
    this.topK = topK;
  }

  public record AssistResult(String answer, List<Citation> citations) {}

  public record Citation(String id, String source, String snippet) {}

  public AssistResult assist(String question) {
    List<RagChunkEntity> all = repository.findAll();
    List<Chunk> corpus = new ArrayList<>(all.size());
    for (RagChunkEntity e : all) {
      corpus.add(new Chunk(e.getId(), e.getSource(), e.getContent()));
    }

    HybridRetriever retriever = new HybridRetriever(embedder, corpus);
    List<Scored> retrieved = corpus.isEmpty() ? List.of() : retriever.retrieve(question, topK);

    Set<String> retrievedIds = new LinkedHashSet<>();
    List<GroundingContext> context = new ArrayList<>();
    for (Scored s : retrieved) {
      retrievedIds.add(s.chunk().id());
      context.add(new GroundingContext(s.chunk().id(), s.chunk().source(), s.chunk().text()));
    }

    Answer answer = llmProvider.answer(question, context);

    // Load-bearing invariant: a cited id the retriever never returned is a hallucination.
    CitationGrounder.verify(answer.citedIds(), retrievedIds);

    List<Citation> citations = new ArrayList<>();
    for (String id : answer.citedIds()) {
      retrieved.stream()
          .filter(s -> s.chunk().id().equals(id))
          .findFirst()
          .ifPresent(
              s -> citations.add(new Citation(id, s.chunk().source(), snippet(s.chunk().text()))));
    }
    return new AssistResult(answer.text(), citations);
  }

  private static String snippet(String text) {
    String firstLine = text.indexOf('\n') < 0 ? text : text.substring(0, text.indexOf('\n'));
    return firstLine.length() > 200 ? firstLine.substring(0, 200) : firstLine;
  }
}
