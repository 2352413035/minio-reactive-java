package io.minio.reactive.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 单条 bucket CORS 规则。 */
public final class BucketCorsRule {
  private final List<String> allowedMethods;
  private final List<String> allowedOrigins;
  private final List<String> allowedHeaders;
  private final List<String> exposeHeaders;
  private final int maxAgeSeconds;

  public BucketCorsRule(
      List<String> allowedMethods,
      List<String> allowedOrigins,
      List<String> allowedHeaders,
      List<String> exposeHeaders,
      int maxAgeSeconds) {
    this.allowedMethods = immutableCopy(allowedMethods);
    this.allowedOrigins = immutableCopy(allowedOrigins);
    this.allowedHeaders = immutableCopy(allowedHeaders);
    this.exposeHeaders = immutableCopy(exposeHeaders);
    this.maxAgeSeconds = maxAgeSeconds;
  }

  public static BucketCorsRule of(List<String> allowedMethods, List<String> allowedOrigins) {
    return new BucketCorsRule(
        allowedMethods,
        allowedOrigins,
        Collections.<String>emptyList(),
        Collections.<String>emptyList(),
        0);
  }

  public List<String> allowedMethods() {
    return allowedMethods;
  }

  public List<String> allowedOrigins() {
    return allowedOrigins;
  }

  public List<String> allowedHeaders() {
    return allowedHeaders;
  }

  public List<String> exposeHeaders() {
    return exposeHeaders;
  }

  public int maxAgeSeconds() {
    return maxAgeSeconds;
  }

  private static List<String> immutableCopy(List<String> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<String>();
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        result.add(value.trim());
      }
    }
    return Collections.unmodifiableList(result);
  }
}
