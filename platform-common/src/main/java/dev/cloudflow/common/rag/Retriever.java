package dev.cloudflow.common.rag;

import java.util.List;

/** Retrieves the top-K most relevant chunks for a question. */
public interface Retriever {

  List<Scored> retrieve(String question, int topK);
}
