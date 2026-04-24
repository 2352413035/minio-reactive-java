package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioHealthClient;
import io.minio.reactive.ReactiveMinioMetricsClient;

/**
 * 运维类客户端示例。
 *
 * <p>Health 与 Metrics 是和对象存储平级的专用客户端：Health 适合探针和网关检查，
 * Metrics 适合接入 Prometheus 文本指标。Metrics 是否需要 bearer token 取决于服务端配置。
 */
public final class ReactiveMinioOpsExample {
  private ReactiveMinioOpsExample() {}

  public static void main(String[] args) {
    String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://127.0.0.1:9000");
    String bearerToken = System.getenv("MINIO_METRICS_BEARER_TOKEN");

    ReactiveMinioHealthClient health =
        ReactiveMinioHealthClient.builder().endpoint(endpoint).region("us-east-1").build();
    Boolean live = health.isLive().block();
    Boolean ready = health.isReady().block();
    System.out.println("存活状态 = " + live);
    System.out.println("就绪状态 = " + ready);

    ReactiveMinioMetricsClient metrics =
        ReactiveMinioMetricsClient.builder().endpoint(endpoint).region("us-east-1").build();
    metrics
        .scrapeClusterMetrics(bearerToken)
        .doOnNext(value -> System.out.println("集群指标样本数 = " + value.samples().size()))
        .onErrorResume(
            error -> {
              System.err.println("指标接口未开放或 bearer token 不正确：" + error.getMessage());
              return reactor.core.publisher.Mono.empty();
            })
        .block();
  }
}
