package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.catalog.MinioApiEndpoint;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 专用客户端共享的目录执行辅助基类。
 *
 * <p>Admin、KMS、STS、Metrics 等客户端都只是围绕某一类目录接口提供更容易发现的方法名，
 * 真正的路径展开、query 合并、认证分派和 HTTP 发送仍统一委托给 `ReactiveMinioRawClient`。
 */
abstract class ReactiveMinioCatalogClientSupport {
  protected final ReactiveMinioRawClient rawClient;

  ReactiveMinioCatalogClientSupport(ReactiveMinioRawClient rawClient) {
    if (rawClient == null) {
      throw new IllegalArgumentException("rawClient must not be null");
    }
    this.rawClient = rawClient;
  }

  /** 返回底层兜底调用器，方便遇到尚未封装的返回形式时直接使用。 */
  public ReactiveMinioRawClient rawClient() {
    return rawClient;
  }

  /** 按目录名称执行接口并返回文本响应。 */
  protected Mono<String> executeToString(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient.executeToString(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 按目录名称执行接口并返回字节响应。 */
  protected Mono<byte[]> executeToBytes(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient.executeToBytes(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 按目录名称执行接口并返回分块字节流。 */
  protected Flux<byte[]> executeToBody(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient.executeToBody(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 按目录名称执行接口并返回响应头。 */
  protected Mono<Map<String, List<String>>> executeToHeaders(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient.executeToHeaders(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 按目录名称执行接口并返回状态码。 */
  protected Mono<Integer> executeToStatus(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient.executeToStatus(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 按目录名称执行无响应体接口。 */
  protected Mono<Void> executeToVoid(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient.executeToVoid(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 查找目录接口，集中处理目录名称拼写错误。 */
  protected MinioApiEndpoint endpoint(String endpointName) {
    return MinioApiCatalog.byName(endpointName);
  }

  /** 供便捷重载复用的空 Map。 */
  protected static Map<String, String> emptyMap() {
    return Collections.<String, String>emptyMap();
  }
}
