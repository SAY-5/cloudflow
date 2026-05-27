package dev.cloudflow.inventory;

import dev.cloudflow.common.log.StructuredLogger;
import dev.cloudflow.inventory.InventoryDtos.AdjustRequest;
import dev.cloudflow.inventory.InventoryDtos.StockView;
import dev.cloudflow.inventory.InventoryDtos.UpsertStockRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/stock")
public class InventoryController {

  private final StockRepository repository;
  private final StructuredLogger log;

  public InventoryController(StockRepository repository, StructuredLogger log) {
    this.repository = repository;
    this.log = log;
  }

  @GetMapping
  public List<StockView> list() {
    return repository.findAll().stream().map(StockView::from).toList();
  }

  @GetMapping("/{sku}")
  public StockView get(@PathVariable String sku) {
    return repository
        .findById(sku)
        .map(StockView::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sku not found"));
  }

  @PutMapping
  public StockView upsert(@Valid @RequestBody UpsertStockRequest request) {
    StockEntity saved = repository.save(new StockEntity(request.sku(), request.available()));
    log.info(
        "stock set",
        Map.of("sku", saved.getSku(), "available", String.valueOf(saved.getAvailable())));
    return StockView.from(saved);
  }

  @PostMapping("/{sku}/adjust")
  public StockView adjust(@PathVariable String sku, @RequestBody AdjustRequest request) {
    StockEntity entity =
        repository
            .findById(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sku not found"));
    int next = entity.getAvailable() + request.delta();
    if (next < 0) {
      log.error(
          "stock would go negative",
          Map.of(
              "sku", sku, "signature", "negative_stock", "delta", String.valueOf(request.delta())));
      throw new ResponseStatusException(HttpStatus.CONFLICT, "insufficient stock");
    }
    entity.setAvailable(next);
    repository.save(entity);
    log.info("stock adjusted", Map.of("sku", sku, "available", String.valueOf(next)));
    return StockView.from(entity);
  }
}
