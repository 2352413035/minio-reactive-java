package io.minio.reactive;

import reactor.core.publisher.Mono;

/**
 * 监控指标专用客户端。
 *
 * <p>这个客户端按监控指标接口的业务名称提供方法，调用者不需要直接查目录或拼 Map。
 * 如果遇到尚未补充业务模型的特殊场景，可以回退使用 `ReactiveMinioRawClient`。
 */
public final class ReactiveMinioMetricsClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioMetricsClient(ReactiveMinioEndpointExecutor executor) {
    super(executor);
  }

  /** 调用 `METRICS_PROMETHEUS_LEGACY`。 */
  public Mono<String> prometheusLegacy(String bearerToken) {
    return executeToString("METRICS_PROMETHEUS_LEGACY", emptyMap(), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

  /** 调用 `METRICS_V2_CLUSTER`。 */
  public Mono<String> v2Cluster(String bearerToken) {
    return executeToString("METRICS_V2_CLUSTER", emptyMap(), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

  /** 调用 `METRICS_V2_BUCKET`。 */
  public Mono<String> v2Bucket(String bearerToken) {
    return executeToString("METRICS_V2_BUCKET", emptyMap(), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

  /** 调用 `METRICS_V2_NODE`。 */
  public Mono<String> v2Node(String bearerToken) {
    return executeToString("METRICS_V2_NODE", emptyMap(), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

  /** 调用 `METRICS_V2_RESOURCE`。 */
  public Mono<String> v2Resource(String bearerToken) {
    return executeToString("METRICS_V2_RESOURCE", emptyMap(), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

  /** 调用 `METRICS_V3`。 */
  public Mono<String> v3(String pathComps, String bearerToken) {
    return executeToString("METRICS_V3", map("pathComps", pathComps), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

}
