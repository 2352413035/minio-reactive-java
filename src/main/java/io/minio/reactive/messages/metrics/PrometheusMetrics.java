package io.minio.reactive.messages.metrics;

/** Prometheus 文本指标响应。 */
public final class PrometheusMetrics {
  private final String scope;
  private final String text;

  public PrometheusMetrics(String scope, String text) {
    this.scope = scope;
    this.text = text == null ? "" : text;
  }

  public String scope() {
    return scope;
  }

  public String text() {
    return text;
  }

  public boolean isEmpty() {
    return text.trim().isEmpty();
  }
}
