package io.minio.reactive.util;

import io.minio.reactive.messages.metrics.PrometheusMetricSample;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Prometheus 文本格式的轻量解析器。 */
public final class PrometheusTextParser {
  private PrometheusTextParser() {}

  /**
   * 解析 Prometheus 文本中的样本行。
   *
   * <p>当前只解析常见的 `name{label="value"} number` 或 `name number` 形式，`# HELP`、
   * `# TYPE` 等注释行会被跳过。复杂转义场景保留原始文本在 `PrometheusMetrics` 中。
   */
  public static List<PrometheusMetricSample> parseSamples(String text) {
    List<PrometheusMetricSample> samples = new ArrayList<PrometheusMetricSample>();
    if (text == null || text.trim().isEmpty()) {
      return samples;
    }
    String[] lines = text.split("\\r?\\n");
    for (String line : lines) {
      PrometheusMetricSample sample = parseLine(line);
      if (sample != null) {
        samples.add(sample);
      }
    }
    return samples;
  }

  private static PrometheusMetricSample parseLine(String line) {
    String value = line == null ? "" : line.trim();
    if (value.isEmpty() || value.startsWith("#")) {
      return null;
    }
    int space = value.lastIndexOf(' ');
    if (space <= 0 || space == value.length() - 1) {
      return null;
    }
    String metric = value.substring(0, space).trim();
    String number = value.substring(space + 1).trim();
    double parsedValue;
    try {
      parsedValue = Double.parseDouble(number);
    } catch (NumberFormatException ignored) {
      return null;
    }

    int labelStart = metric.indexOf('{');
    if (labelStart < 0) {
      return new PrometheusMetricSample(metric, new LinkedHashMap<String, String>(), parsedValue);
    }
    int labelEnd = metric.lastIndexOf('}');
    if (labelEnd < labelStart) {
      return null;
    }
    String name = metric.substring(0, labelStart);
    String labelText = metric.substring(labelStart + 1, labelEnd);
    return new PrometheusMetricSample(name, parseLabels(labelText), parsedValue);
  }

  private static Map<String, String> parseLabels(String labelText) {
    Map<String, String> labels = new LinkedHashMap<String, String>();
    if (labelText == null || labelText.trim().isEmpty()) {
      return labels;
    }
    String[] pairs = labelText.split(",");
    for (String pair : pairs) {
      int equals = pair.indexOf('=');
      if (equals <= 0) {
        continue;
      }
      String key = pair.substring(0, equals).trim();
      String rawValue = pair.substring(equals + 1).trim();
      if (rawValue.startsWith("\"") && rawValue.endsWith("\"") && rawValue.length() >= 2) {
        rawValue = rawValue.substring(1, rawValue.length() - 1);
      }
      labels.put(key, rawValue);
    }
    return labels;
  }
}
