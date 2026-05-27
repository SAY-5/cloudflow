package dev.cloudflow.assistant.llm;

import java.util.List;

/** Abstracts the language model behind the assistant so CI never calls a real model. */
public interface LlmProvider {

  /**
   * Produces a grounded answer from a question and the retrieved context.
   *
   * @param question the operator's question
   * @param context the retrieved candidate chunks, each carrying a stable id
   * @return an answer plus the ids of the context entries it relied on
   */
  Answer answer(String question, List<GroundingContext> context);

  /** A single piece of retrieved grounding context passed to the model. */
  record GroundingContext(String id, String source, String text) {}

  /** The model's reply: prose plus the ids of the context it cited. */
  record Answer(String text, List<String> citedIds) {}
}
