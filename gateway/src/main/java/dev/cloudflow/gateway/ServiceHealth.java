package dev.cloudflow.gateway;

/** Health of a single downstream service as seen by the gateway. */
public record ServiceHealth(String service, boolean up, String detail) {

  public static ServiceHealth up(String service) {
    return new ServiceHealth(service, true, "UP");
  }

  public static ServiceHealth down(String service, String detail) {
    return new ServiceHealth(service, false, detail);
  }
}
