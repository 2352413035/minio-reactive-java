package io.minio.reactive.messages;

/** 健康检查结果。 */
public final class HealthCheckResult {
  private final String name;
  private final int statusCode;

  public HealthCheckResult(String name, int statusCode) {
    this.name = name;
    this.statusCode = statusCode;
  }

  public String name() {
    return name;
  }

  public int statusCode() {
    return statusCode;
  }

  public boolean isHealthy() {
    return statusCode >= 200 && statusCode < 300;
  }
}
