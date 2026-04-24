package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.signer.S3RequestSigner;
import org.springframework.web.reactive.function.client.WebClient;
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

  public static Builder builder() {
    return new Builder();
  }


  /** 拉取集群 Prometheus 指标文本。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeClusterMetrics(
      String bearerToken) {
    return v2Cluster(bearerToken)
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("cluster", text));
  }

  /** 拉取节点 Prometheus 指标文本。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeNodeMetrics(
      String bearerToken) {
    return v2Node(bearerToken)
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("node", text));
  }

  /** 拉取桶维度 Prometheus 指标文本。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeBucketMetrics(
      String bearerToken) {
    return v2Bucket(bearerToken)
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("bucket", text));
  }

  /** 拉取资源维度 Prometheus 指标文本。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeResourceMetrics(
      String bearerToken) {
    return v2Resource(bearerToken)
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("resource", text));
  }

  /** 拉取 metrics v3 指定路径的指标文本。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeV3(
      String pathComps, String bearerToken) {
    String normalizedPath = normalizeMetricsV3Path(pathComps);
    return v3(normalizedPath, bearerToken)
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("v3" + normalizedPath, text));
  }

  /** 拉取旧版 Prometheus 指标文本。 */
  public Mono<io.minio.reactive.messages.metrics.PrometheusMetrics> scrapeLegacyMetrics(
      String bearerToken) {
    return prometheusLegacy(bearerToken)
        .map(text -> new io.minio.reactive.messages.metrics.PrometheusMetrics("legacy", text));
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
    return executeToString("METRICS_V3", map("pathComps", normalizeMetricsV3Path(pathComps)), emptyMap(), bearerHeaders(bearerToken), null, null);
  }

  private static String normalizeMetricsV3Path(String pathComps) {
    if (pathComps == null || pathComps.trim().isEmpty()) {
      return "";
    }
    String value = pathComps.trim();
    return value.startsWith("/") ? value : "/" + value;
  }



  public static final class Builder {
    private String endpoint;
    private String region;
    private ReactiveCredentialsProvider credentialsProvider;
    private WebClient webClient;

    private Builder() {}

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder credentials(String accessKey, String secretKey) {
      this.credentialsProvider = new StaticCredentialsProvider(accessKey, secretKey);
      return this;
    }

    public Builder credentials(String accessKey, String secretKey, String sessionToken) {
      this.credentialsProvider = new StaticCredentialsProvider(accessKey, secretKey, sessionToken);
      return this;
    }

    public Builder credentialsProvider(ReactiveCredentialsProvider credentialsProvider) {
      this.credentialsProvider = credentialsProvider;
      return this;
    }

    public Builder webClient(WebClient webClient) {
      this.webClient = webClient;
      return this;
    }

    public ReactiveMinioMetricsClient build() {
      ReactiveMinioClientConfig config = ReactiveMinioClientConfig.of(endpoint, region);
      WebClient actualWebClient =
          webClient != null ? webClient : WebClient.builder().baseUrl(config.endpoint()).build();
      ReactiveCredentialsProvider actualProvider =
          credentialsProvider != null ? credentialsProvider : ReactiveCredentialsProvider.anonymous();
      ReactiveMinioEndpointExecutor executor =
          new ReactiveMinioEndpointExecutor(
              config,
              actualProvider,
              new ReactiveHttpClient(actualWebClient, config),
              new S3RequestSigner());
      return new ReactiveMinioMetricsClient(executor);
    }
  }
}
