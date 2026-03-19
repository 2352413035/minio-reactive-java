package io.minio.reactive;

import java.net.URI;
import java.util.Objects;

/**
 * 当前原型项目使用的最小客户端配置。
 *
 * <p>目前只保留已经验证必需的几个字段：
 *
 * <ul>
 *   <li>endpoint：MinIO 服务地址
 *   <li>region：签名时使用的区域
 *   <li>pathStyle：当前是否使用路径风格访问
 * </ul>
 *
 * <p>官方 `minio-java` 在这层还会处理更多事情，例如 region 发现、超时、域名风格判断等。
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
