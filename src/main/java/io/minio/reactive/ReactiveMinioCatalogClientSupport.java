package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.catalog.MinioApiEndpoint;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 专用客户端共享的目录执行辅助基类。
 *
 * <p>它直接依赖底层执行器，不依赖 `ReactiveMinioRawClient`。这样 Admin、KMS、STS、Metrics、
 * Health 等专用客户端与 raw client 是并列关系，而不是“用 raw 再包一层”。
 */
abstract class ReactiveMinioCatalogClientSupport {
  private final ReactiveMinioEndpointExecutor executor;

  ReactiveMinioCatalogClientSupport(ReactiveMinioEndpointExecutor executor) {
    if (executor == null) {
      throw new IllegalArgumentException("executor must not be null");
    }
    this.executor = executor;
  }

  /** 按目录名称执行接口并返回文本响应。 */
  protected Mono<String> executeToString(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToString(
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
    return executor.executeToBytes(
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
    return executor.executeToBody(
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
    return executor.executeToHeaders(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 按目录名称执行接口并返回任意状态码，不把非 2xx 转成异常。 */
  protected Mono<Integer> executeToStatusAllowAll(
      String endpointName,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToStatusAllowAll(
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
    return executor.executeToStatus(
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
    return executor.executeToVoid(
        endpoint(endpointName), pathVariables, queryParameters, headers, body, contentType);
  }

  /** 查找目录接口，集中处理目录名称拼写错误。 */
  protected MinioApiEndpoint endpoint(String endpointName) {
    return MinioApiCatalog.byName(endpointName);
  }

  /** 供便捷方法复用的空 Map。 */
  protected static Map<String, String> emptyMap() {
    return Collections.<String, String>emptyMap();
  }

  /** 生成只有一个键值对的 Map，用于强类型方法内部组装路径变量或 query。 */
  protected static Map<String, String> map(String key, String value) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    result.put(key, value);
    return result;
  }

  /** 根据可变键值对生成 Map，调用方必须成对传入 key 和 value。 */
  protected static Map<String, String> map(String... keyValues) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (int i = 0; i < keyValues.length; i += 2) {
      result.put(keyValues[i], keyValues[i + 1]);
    }
    return result;
  }

  /** 校验用户输入的必要文本参数，提前给出清晰错误。 */
  protected static void requireText(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " 不能为空");
    }
  }

  /** 为 bearer 认证接口组装 Authorization 头；token 为空时返回空头，兼容公开 metrics 配置。 */
  protected static Map<String, String> bearerHeaders(String bearerToken) {
    if (bearerToken == null || bearerToken.trim().isEmpty()) {
      return emptyMap();
    }
    return map("Authorization", bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken);
  }
}
