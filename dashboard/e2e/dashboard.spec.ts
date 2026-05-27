import { test, expect } from "@playwright/test";

// Runs against the docker-compose stack: the dashboard is served by nginx and
// proxies /api to the gateway, which fans out to the services and the assistant.
test.describe("CloudFlow ops dashboard", () => {
  test("shows platform health and the service grid", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "CloudFlow Ops" })).toBeVisible();
    await expect(page.getByRole("heading", { name: /Platform health/ })).toBeVisible();
    await expect(page.getByText("orders")).toBeVisible();
    await expect(page.getByText("inventory")).toBeVisible();
  });

  test("answers an operational question with grounded citations", async ({ page }) => {
    await page.goto("/");
    await page
      .getByLabel("Operational question")
      .fill("how do I roll back inventory");
    await page.getByRole("button", { name: "Ask" }).click();

    await expect(page.getByRole("heading", { name: "Citations" })).toBeVisible();
    // The answer text and each citation id both mention the runbook, so scope to a
    // citation <code> element and take the first match.
    await expect(page.locator("code", { hasText: /doc:rollback-inventory/ }).first()).toBeVisible();
  });
});
