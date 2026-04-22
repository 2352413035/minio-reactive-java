package io.minio.reactive;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * KMS 专用客户端。
 *
 * <p>这个客户端只提供KMS相关目录接口的命名入口；响应体先以原始文本返回。
 * 如果某个接口需要二进制、流式或只读响应头，可以通过 `rawClient()` 使用底层兜底调用器。
 */
public final class ReactiveMinioKmsClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioKmsClient(ReactiveMinioRawClient rawClient) {
    super(rawClient);
  }

  /** 调用目录接口 `KMS_STATUS`，返回原始文本响应。 */
  public Mono<String> status(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `KMS_METRICS`，返回原始文本响应。 */
  public Mono<String> metrics(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_METRICS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `KMS_APIS`，返回原始文本响应。 */
  public Mono<String> apis(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_APIS", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `KMS_VERSION`，返回原始文本响应。 */
  public Mono<String> version(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_VERSION", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `KMS_KEY_CREATE`，返回原始文本响应。 */
  public Mono<String> keyCreate(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_KEY_CREATE", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `KMS_KEY_LIST`，返回原始文本响应。 */
  public Mono<String> keyList(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_KEY_LIST", pathVariables, queryParameters, headers, body, contentType);
  }

  /** 调用目录接口 `KMS_KEY_STATUS`，返回原始文本响应。 */
  public Mono<String> keyStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executeToString("KMS_KEY_STATUS", pathVariables, queryParameters, headers, body, contentType);
  }

}
