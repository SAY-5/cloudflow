package dev.cloudflow.orders;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** Request and response shapes for the orders API. */
public final class OrderDtos {

  private OrderDtos() {}

  public record CreateOrderRequest(@NotBlank String sku, @Min(1) int quantity) {}

  public record OrderView(Long id, String sku, int quantity, String status, Instant createdAt) {
    public static OrderView from(OrderEntity e) {
      return new OrderView(e.getId(), e.getSku(), e.getQuantity(), e.getStatus(), e.getCreatedAt());
    }
  }
}
