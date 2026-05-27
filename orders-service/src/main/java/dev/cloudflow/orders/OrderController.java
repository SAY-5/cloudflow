package dev.cloudflow.orders;

import dev.cloudflow.common.log.StructuredLogger;
import dev.cloudflow.orders.OrderDtos.CreateOrderRequest;
import dev.cloudflow.orders.OrderDtos.OrderView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderRepository repository;
  private final StructuredLogger log;

  public OrderController(OrderRepository repository, StructuredLogger log) {
    this.repository = repository;
    this.log = log;
  }

  @GetMapping
  public List<OrderView> list() {
    return repository.findAll().stream().map(OrderView::from).toList();
  }

  @GetMapping("/{id}")
  public OrderView get(@PathVariable Long id) {
    return repository
        .findById(id)
        .map(OrderView::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderView create(@Valid @RequestBody CreateOrderRequest request) {
    OrderEntity saved =
        repository.save(
            new OrderEntity(request.sku(), request.quantity(), "CREATED", Instant.now()));
    log.info(
        "order created",
        Map.of(
            "order_id", String.valueOf(saved.getId()),
            "sku", saved.getSku(),
            "quantity", String.valueOf(saved.getQuantity())));
    return OrderView.from(saved);
  }

  @PostMapping("/{id}/fail")
  public ResponseEntity<OrderView> fail(@PathVariable Long id) {
    OrderEntity entity =
        repository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    entity.setStatus("FAILED");
    repository.save(entity);
    log.error(
        "order failed",
        Map.of("order_id", String.valueOf(id), "signature", "payment_gateway_timeout"));
    return ResponseEntity.ok(OrderView.from(entity));
  }
}
