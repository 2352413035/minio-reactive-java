package io.minio.reactive;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 健康检查专用客户端。
 *
 * <p>这个客户端只提供健康检查相关目录接口的命名入口；响应体先以原始文本返回。
 * 如果某个接口需要二进制、流式或只读响应头，可以通过 `rawClient()` 使用底层兜底调用器。
 */
public final class ReactiveMinioHealthClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioHealthClient(ReactiveMinioRawClient rawClient) {
    super(rawClient);
  }

  /** 调用目录接口 `HEALTH_CLUSTER_GET`，返回原始文本响应。 */
  public Mono<String> clusterGet(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_CLUSTER_GET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_CLUSTER_HEAD`，返回原始文本响应。 */
  public Mono<String> clusterHead(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_CLUSTER_HEAD", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_CLUSTER_READ_GET`，返回原始文本响应。 */
  public Mono<String> clusterReadGet(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_CLUSTER_READ_GET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_CLUSTER_READ_HEAD`，返回原始文本响应。 */
  public Mono<String> clusterReadHead(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_CLUSTER_READ_HEAD", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_LIVE_GET`，返回原始文本响应。 */
  public Mono<String> liveGet(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_LIVE_GET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_LIVE_HEAD`，返回原始文本响应。 */
  public Mono<String> liveHead(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_LIVE_HEAD", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_READY_GET`，返回原始文本响应。 */
  public Mono<String> readyGet(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_READY_GET", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `HEALTH_READY_HEAD`，返回原始文本响应。 */
  public Mono<String> readyHead(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("HEALTH_READY_HEAD", pathVariables, queryParameters, headers, body, contentType);
  }

}
