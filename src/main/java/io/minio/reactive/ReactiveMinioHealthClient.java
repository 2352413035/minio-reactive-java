package io.minio.reactive;

import reactor.core.publisher.Mono;

/**
 * 健康检查专用客户端。
 *
 * <p>这个客户端按健康检查接口的业务名称提供方法，调用者不需要直接查目录或拼 Map。
 * 如果遇到尚未补充业务模型的特殊场景，可以回退使用 `ReactiveMinioRawClient`。
 */
public final class ReactiveMinioHealthClient extends ReactiveMinioCatalogClientSupport {
  ReactiveMinioHealthClient(ReactiveMinioEndpointExecutor executor) {
    super(executor);
  }

  /** 调用 `HEALTH_CLUSTER_GET`。 */
  public Mono<Integer> clusterGet() {
    return executeToStatus("HEALTH_CLUSTER_GET", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_CLUSTER_HEAD`。 */
  public Mono<Integer> clusterHead() {
    return executeToStatus("HEALTH_CLUSTER_HEAD", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_CLUSTER_READ_GET`。 */
  public Mono<Integer> clusterReadGet() {
    return executeToStatus("HEALTH_CLUSTER_READ_GET", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_CLUSTER_READ_HEAD`。 */
  public Mono<Integer> clusterReadHead() {
    return executeToStatus("HEALTH_CLUSTER_READ_HEAD", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_LIVE_GET`。 */
  public Mono<Integer> liveGet() {
    return executeToStatus("HEALTH_LIVE_GET", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_LIVE_HEAD`。 */
  public Mono<Integer> liveHead() {
    return executeToStatus("HEALTH_LIVE_HEAD", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_READY_GET`。 */
  public Mono<Integer> readyGet() {
    return executeToStatus("HEALTH_READY_GET", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `HEALTH_READY_HEAD`。 */
  public Mono<Integer> readyHead() {
    return executeToStatus("HEALTH_READY_HEAD", emptyMap(), emptyMap(), emptyMap(), null, null);
  }

}
