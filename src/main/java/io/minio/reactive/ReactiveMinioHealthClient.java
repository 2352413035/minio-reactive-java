package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.signer.S3RequestSigner;
import org.springframework.web.reactive.function.client.WebClient;
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

  public static Builder builder() {
    return new Builder();
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

    public ReactiveMinioHealthClient build() {
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
      return new ReactiveMinioHealthClient(executor);
    }
  }
}
