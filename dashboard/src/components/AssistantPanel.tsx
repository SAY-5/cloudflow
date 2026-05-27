import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { askAssistant } from "../api";

export function AssistantPanel() {
  const [question, setQuestion] = useState("");
  const mutation = useMutation({ mutationFn: askAssistant });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (question.trim()) {
      mutation.mutate(question.trim());
    }
  }

  return (
    <section aria-labelledby="assist-heading" className="assistant">
      <h2 id="assist-heading">Ask the assistant</h2>
      <form onSubmit={onSubmit}>
        <label htmlFor="assist-input">Operational question</label>
        <input
          id="assist-input"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="why did orders error-rate spike at 14:00?"
        />
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Thinking..." : "Ask"}
        </button>
      </form>

      {mutation.isError && <p role="alert">The assistant could not answer.</p>}

      {mutation.data && (
        <div className="answer">
          <pre>{mutation.data.answer}</pre>
          {mutation.data.citations.length > 0 && (
            <>
              <h3>Citations</h3>
              <ul>
                {mutation.data.citations.map((c) => (
                  <li key={c.id}>
                    <code>{c.id}</code> ({c.source}): {c.snippet}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </section>
  );
}
