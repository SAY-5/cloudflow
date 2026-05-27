package dev.cloudflow.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Request and response shapes for the inventory API. */
public final class InventoryDtos {

  private InventoryDtos() {}

  public record UpsertStockRequest(@NotBlank String sku, @Min(0) int available) {}

  public record AdjustRequest(int delta) {}

  public record StockView(String sku, int available) {
    public static StockView from(StockEntity e) {
      return new StockView(e.getSku(), e.getAvailable());
    }
  }
}
