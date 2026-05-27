import { render, screen } from "@testing-library/react";
import { App } from "../src/App";

describe("App", () => {
  it("renders the platform title", () => {
    render(<App />);
    expect(screen.getByText("CloudFlow Ops")).toBeInTheDocument();
  });
});
