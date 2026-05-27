package dev.cloudflow.assistant.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A recorded error-rate anomaly, shared between the collector (writer) and assistant (reader). */
@Entity
@Table(name = "anomaly")
public class AnomalyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String service;

  @Column(name = "window_start", nullable = false)
  private Instant windowStart;

  @Column(name = "window_end", nullable = false)
  private Instant windowEnd;

  @Column(name = "error_rate", nullable = false)
  private double errorRate;

  @Column(name = "z_score", nullable = false)
  private double zScore;

  @Column(name = "top_signatures", nullable = false)
  private String topSignatures;

  protected AnomalyEntity() {}

  public AnomalyEntity(
      String service,
      Instant windowStart,
      Instant windowEnd,
      double errorRate,
      double zScore,
      String topSignatures) {
    this.service = service;
    this.windowStart = windowStart;
    this.windowEnd = windowEnd;
    this.errorRate = errorRate;
    this.zScore = zScore;
    this.topSignatures = topSignatures;
  }

  public Long getId() {
    return id;
  }

  public String getService() {
    return service;
  }

  public Instant getWindowStart() {
    return windowStart;
  }

  public Instant getWindowEnd() {
    return windowEnd;
  }

  public double getErrorRate() {
    return errorRate;
  }

  public double getZScore() {
    return zScore;
  }

  public String getTopSignatures() {
    return topSignatures;
  }
}
