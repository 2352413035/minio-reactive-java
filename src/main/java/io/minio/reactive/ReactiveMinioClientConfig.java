package io.minio.reactive;

import java.net.URI;
import java.util.Objects;

/**
 * Minimal client configuration used by the current prototype.
 *
 * <p>At this stage we only keep the fields needed to talk to a real MinIO server:
 * endpoint, region, and path-style access.
 */
public final class ReactiveMinioClientConfig {
  private final String endpoint;
  private final URI endpointUri;
  private final String region;
  private final boolean pathStyle;

  private ReactiveMinioClientConfig(String endpoint, String region, boolean pathStyle) {
    this.endpoint = normalizeEndpoint(endpoint);
    this.endpointUri = URI.create(this.endpoint);
    this.region = region == null || region.trim().isEmpty() ? "us-east-1" : region;
    this.pathStyle = pathStyle;
  }

  public static ReactiveMinioClientConfig of(String endpoint, String region) {
    return new ReactiveMinioClientConfig(endpoint, region, true);
  }

  public String endpoint() {
    return endpoint;
  }

  public URI endpointUri() {
    return endpointUri;
  }

  public String region() {
    return region;
  }

  public boolean pathStyle() {
    return pathStyle;
  }

  private static String normalizeEndpoint(String endpoint) {
    Objects.requireNonNull(endpoint, "endpoint must not be null");
    String value = endpoint.trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("endpoint must not be empty");
    }
    if (!value.startsWith("http://") && !value.startsWith("https://")) {
      throw new IllegalArgumentException("endpoint must start with http:// or https://");
    }
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }
}
