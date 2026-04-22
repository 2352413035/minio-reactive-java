package io.minio.reactive.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个 MinIO HTTP 接口的最小元数据。
 *
 * <p>它只描述“如何构造请求”：接口名、分组、HTTP 方法、路径模板、认证方式、默认 query
 * 和必填 query。它不负责解析具体业务响应，避免管理端等大量协议模型一次性耦合进核心客户端。
 */
public final class MinioApiEndpoint {
  private final String name;
  private final String family;
  private final String method;
  private final String pathTemplate;
  private final boolean authRequired;
  private final String authScheme;
  private final Map<String, String> defaultQueryParameters;
  private final List<String> requiredQueryParameters;

  /** 使用布尔认证标记创建接口；历史兼容入口中 true 表示 SigV4，false 表示无认证。 */
  MinioApiEndpoint(
      String name,
      String family,
      String method,
      String pathTemplate,
      boolean authRequired,
      Map<String, String> defaultQueryParameters,
      List<String> requiredQueryParameters) {
    this.name = name;
    this.family = family;
    this.method = method;
    this.pathTemplate = pathTemplate;
    this.authRequired = authRequired;
    this.authScheme = authRequired ? "sigv4" : "none";
    this.defaultQueryParameters =
        Collections.unmodifiableMap(new LinkedHashMap<String, String>(defaultQueryParameters));
    this.requiredQueryParameters =
        Collections.unmodifiableList(new ArrayList<String>(requiredQueryParameters));
  }


  /** 使用明确认证方案创建接口；用于 metrics 的 bearer、health 的 none 等非 SigV4 场景。 */
  MinioApiEndpoint(
      String name,
      String family,
      String method,
      String pathTemplate,
      String authScheme,
      Map<String, String> defaultQueryParameters,
      List<String> requiredQueryParameters) {
    this.name = name;
    this.family = family;
    this.method = method;
    this.pathTemplate = pathTemplate;
    this.authScheme = authScheme == null ? "none" : authScheme;
    this.authRequired = !"none".equals(this.authScheme);
    this.defaultQueryParameters =
        Collections.unmodifiableMap(new LinkedHashMap<String, String>(defaultQueryParameters));
    this.requiredQueryParameters =
        Collections.unmodifiableList(new ArrayList<String>(requiredQueryParameters));
  }

  public String name() {
    return name;
  }

  public String family() {
    return family;
  }

  public String method() {
    return method;
  }

  public String pathTemplate() {
    return pathTemplate;
  }

  public boolean authRequired() {
    return authRequired;
  }

  public String authScheme() {
    return authScheme;
  }

  /** 只有 SigV4 接口才交给签名器；Bearer 和无认证接口保留调用方提供的认证方式。 */
  public boolean requiresSigV4() {
    return "sigv4".equals(authScheme);
  }

  public Map<String, String> defaultQueryParameters() {
    return defaultQueryParameters;
  }

  public List<String> requiredQueryParameters() {
    return requiredQueryParameters;
  }

  @Override
  public String toString() {
    return method + " " + pathTemplate + " [" + name + "]";
  }
}
