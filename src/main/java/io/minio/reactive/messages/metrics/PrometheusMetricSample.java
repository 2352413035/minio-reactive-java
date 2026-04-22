package io.minio.reactive.messages.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 单条 Prometheus 指标样本。 */
public final class PrometheusMetricSample {
  private final String name;
  private final Map<String, String> labels;
  private final double value;

  public PrometheusMetricSample(String name, Map<String, String> labels, double value) {
    this.name = name;
    this.labels = Collections.unmodifiableMap(new LinkedHashMap<String, String>(labels));
    this.value = value;
  }

  public String name() {
    return name;
  }

  public Map<String, String> labels() {
    return labels;
  }

  public double value() {
    return value;
  }
}
