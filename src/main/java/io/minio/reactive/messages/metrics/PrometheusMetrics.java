package io.minio.reactive.messages.metrics;

import io.minio.reactive.util.PrometheusTextParser;
import java.util.List;

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

  /** 解析出 Prometheus 文本中的普通样本行；复杂格式仍可通过 text() 读取原文。 */
  public List<PrometheusMetricSample> samples() {
    return PrometheusTextParser.parseSamples(text);
  }
}
