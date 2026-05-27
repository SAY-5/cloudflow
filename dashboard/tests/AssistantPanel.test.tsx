import { render, screen, waitFor } from "@testing-library/react";
import userEventDefault from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { AssistantPanel } from "../src/components/AssistantPanel";

function renderWithClient(ui: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe("AssistantPanel", () => {
  it("submits a question and shows the grounded answer with citations", async () => {
    const user = userEventDefault.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          answer: "Run helm rollback inventory.",
          citations: [
            { id: "doc:rollback-inventory#1", source: "doc", snippet: "helm rollback inventory" },
          ],
        }),
      }),
    );

    renderWithClient(<AssistantPanel />);

    await user.type(screen.getByLabelText("Operational question"), "how do I roll back inventory");
    await user.click(screen.getByRole("button", { name: "Ask" }));

    await waitFor(() =>
      expect(screen.getByText("Run helm rollback inventory.")).toBeInTheDocument(),
    );
    expect(screen.getByText("doc:rollback-inventory#1")).toBeInTheDocument();
  });
});
