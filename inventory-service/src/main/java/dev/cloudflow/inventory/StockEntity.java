package dev.cloudflow.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock")
public class StockEntity {

  @Id private String sku;

  @Column(nullable = false)
  private int available;

  protected StockEntity() {}

  public StockEntity(String sku, int available) {
    this.sku = sku;
    this.available = available;
  }

  public String getSku() {
    return sku;
  }

  public int getAvailable() {
    return available;
  }

  public void setAvailable(int available) {
    this.available = available;
  }
}
