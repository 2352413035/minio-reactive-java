package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.signer.S3RequestSigner;
import org.springframework.web.reactive.function.client.WebClient;
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

  public static Builder builder() {
    return new Builder();
  }


  /** 获取 KMS 状态，并保留原始 JSON 字段。 */
  public Mono<io.minio.reactive.messages.kms.KmsJsonResult> getStatus() {
    return status().map(io.minio.reactive.messages.kms.KmsJsonResult::parse);
  }

  /** 获取 KMS API 列表或能力信息，并保留原始 JSON 字段。 */
  public Mono<io.minio.reactive.messages.kms.KmsJsonResult> getApis() {
    return apis().map(io.minio.reactive.messages.kms.KmsJsonResult::parse);
  }

  /** 获取 KMS 版本信息，并保留原始 JSON 字段。 */
  public Mono<io.minio.reactive.messages.kms.KmsJsonResult> getVersion() {
    return version().map(io.minio.reactive.messages.kms.KmsJsonResult::parse);
  }

  /** 列出匹配 pattern 的 KMS key。 */
  public Mono<io.minio.reactive.messages.kms.KmsKeyList> listKeys(String pattern) {
    requireText("pattern", pattern);
    return keyList(pattern).map(io.minio.reactive.messages.kms.KmsKeyList::parse);
  }

  /** 创建 KMS key。 */
  public Mono<Void> createKey(String keyId) {
    requireText("keyId", keyId);
    return executeToVoid("KMS_KEY_CREATE", emptyMap(), map("key-id", keyId), emptyMap(), null, null);
  }

  /** 获取 KMS key 状态，并保留原始 JSON 字段。 */
  public Mono<io.minio.reactive.messages.kms.KmsJsonResult> getKeyStatus() {
    return keyStatus().map(io.minio.reactive.messages.kms.KmsJsonResult::parse);
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

    public ReactiveMinioKmsClient build() {
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
      return new ReactiveMinioKmsClient(executor);
    }
  }
}
