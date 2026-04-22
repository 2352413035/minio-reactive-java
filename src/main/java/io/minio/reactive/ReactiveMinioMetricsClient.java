package io.minio.reactive;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 监控指标专用客户端。
 *
 * <p>这个客户端只提供监控指标相关目录接口的命名入口；响应体先以原始文本返回。
 * 如果某个接口需要二进制、流式或只读响应头，可以通过 `rawClient()` 使用底层兜底调用器。
 */
public final class ReactiveMinioMetricsClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioMetricsClient(ReactiveMinioRawClient rawClient) {
    super(rawClient);
  }

  /** 调用目录接口 `METRICS_PROMETHEUS_LEGACY`，返回原始文本响应。 */
  public Mono<String> prometheusLegacy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("METRICS_PROMETHEUS_LEGACY", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `METRICS_V2_CLUSTER`，返回原始文本响应。 */
  public Mono<String> v2Cluster(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("METRICS_V2_CLUSTER", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `METRICS_V2_BUCKET`，返回原始文本响应。 */
  public Mono<String> v2Bucket(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("METRICS_V2_BUCKET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `METRICS_V2_NODE`，返回原始文本响应。 */
  public Mono<String> v2Node(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("METRICS_V2_NODE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `METRICS_V2_RESOURCE`，返回原始文本响应。 */
  public Mono<String> v2Resource(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("METRICS_V2_RESOURCE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `METRICS_V3`，返回原始文本响应。 */
  public Mono<String> v3(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("METRICS_V3", pathVariables, queryParameters, headers, body, contentType);
  }

}
