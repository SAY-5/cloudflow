package dev.cloudflow.assistant;

import java.util.List;
import java.util.Set;

/**
 * Enforces the assistant's core correctness property: every cited id in an answer must exist in the
 * retrieved candidate set. An answer that cites an id it was not given is a hallucinated citation
 * and is rejected.
 */
public final class CitationGrounder {

  private CitationGrounder() {}

  /** Thrown when an answer cites an id that was not in the retrieved candidate set. */
  public static final class UngroundedCitationException extends RuntimeException {
    public UngroundedCitationException(String message) {
      super(message);
    }
  }

  /**
   * Verifies that all cited ids are members of the retrieved set.
   *
   * @throws UngroundedCitationException if any cited id is absent from {@code retrievedIds}
   */
  public static void verify(List<String> citedIds, Set<String> retrievedIds) {
    for (String id : citedIds) {
      if (!retrievedIds.contains(id)) {
        throw new UngroundedCitationException("answer cited ungrounded id: " + id);
      }
    }
  }

  /** Returns true when every cited id is present in the retrieved set. */
  public static boolean isGrounded(List<String> citedIds, Set<String> retrievedIds) {
    return retrievedIds.containsAll(citedIds);
  }
}
