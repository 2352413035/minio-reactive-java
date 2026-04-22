package io.minio.reactive;

import reactor.core.publisher.Mono;

/**
 * KMS 专用客户端。
 *
 * <p>这个客户端按KMS接口的业务名称提供方法，调用者不需要直接查目录或拼 Map。
 * 如果遇到尚未补充业务模型的特殊场景，可以回退使用 `ReactiveMinioRawClient`。
 */
public final class ReactiveMinioKmsClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioKmsClient(ReactiveMinioEndpointExecutor executor) {
    super(executor);
  }

  /** 调用 `KMS_STATUS`。 */
  public Mono<String> status() {
    return executeToString("KMS_STATUS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `KMS_METRICS`。 */
  public Mono<String> metrics() {
    return executeToString("KMS_METRICS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `KMS_APIS`。 */
  public Mono<String> apis() {
    return executeToString("KMS_APIS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `KMS_VERSION`。 */
  public Mono<String> version() {
    return executeToString("KMS_VERSION", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `KMS_KEY_CREATE`。 */
  public Mono<String> keyCreate(String keyId, byte[] body, String contentType) {
    return executeToString("KMS_KEY_CREATE", emptyMap(), map("key-id", keyId), emptyMap(), body, contentType);
  }

  /** 调用 `KMS_KEY_CREATE`，不携带请求体。 */
  public Mono<String> keyCreate(String keyId) {
    return keyCreate(keyId, null, null);
  }

  /** 调用 `KMS_KEY_LIST`。 */
  public Mono<String> keyList(String pattern) {
    return executeToString("KMS_KEY_LIST", emptyMap(), map("pattern", pattern), emptyMap(), null, null);
  }

  /** 调用 `KMS_KEY_STATUS`。 */
  public Mono<String> keyStatus() {
    return executeToString("KMS_KEY_STATUS", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

}
