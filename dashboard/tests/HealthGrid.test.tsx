import type { ReactNode } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { HealthGrid } from "../src/components/HealthGrid";

function renderWithClient(ui: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe("HealthGrid", () => {
  it("renders the aggregate status and per-service cards", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          status: "DEGRADED",
          services: [
            { service: "orders", up: true, detail: "UP" },
            { service: "inventory", up: false, detail: "timeout" },
          ],
        }),
      }),
    );

    renderWithClient(<HealthGrid />);

    await waitFor(() => expect(screen.getByText("DEGRADED")).toBeInTheDocument());
    expect(screen.getByText("orders")).toBeInTheDocument();
    expect(screen.getByText("timeout")).toBeInTheDocument();
  });
});
