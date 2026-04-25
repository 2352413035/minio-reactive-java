package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.signer.S3RequestSigner;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MinIO 公开 HTTP 接口目录的兜底原始调用器。
 *
 * <p>它保留 Map 形式的灵活入参，专门用于 SDK 尚未封装的新接口、特殊接口或排障场景。
 * 常规业务代码应优先使用 `ReactiveMinioClient` 以及 Admin、KMS、STS、Metrics、Health 等专用客户端。
 */
public final class ReactiveMinioRawClient {
  private final ReactiveMinioEndpointExecutor executor;

  ReactiveMinioRawClient(
      ReactiveMinioClientConfig config,
      ReactiveCredentialsProvider credentialsProvider,
      ReactiveHttpClient httpClient,
      S3RequestSigner signer) {
    this(new ReactiveMinioEndpointExecutor(config, credentialsProvider, httpClient, signer));
  }

  ReactiveMinioRawClient(ReactiveMinioEndpointExecutor executor) {
    this.executor = executor;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** 暴露请求构造结果，便于测试和排查目录条目展开后的实际请求。 */
  public S3Request requestFor(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.requestFor(endpoint, pathVariables, queryParameters, headers, body, contentType);
  }

  /** 执行不带额外参数的接口，并只返回 HTTP 状态码。 */
  public Mono<Integer> executeToStatus(MinioApiEndpoint endpoint) {
    return executeToStatus(
        endpoint,
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        null,
        null);
  }

  /** 执行接口并只关心状态码，适合 HEAD、健康检查或只需确认成功的操作。 */
  public Mono<Integer> executeToStatus(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToStatus(
        endpoint, pathVariables, queryParameters, headers, body, contentType);
  }

  /** 执行不带额外参数的接口，并把响应体整体读取为字符串。 */
  public Mono<String> executeToString(MinioApiEndpoint endpoint) {
    return executeToString(
        endpoint,
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        Collections.<String, String>emptyMap(),
        null,
        null);
  }

  /** 执行接口并把响应体整体读取为字符串，适合 XML、JSON、文本类管理接口。 */
  public Mono<String> executeToString(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToString(
        endpoint, pathVariables, queryParameters, headers, body, contentType);
  }

  /** 执行接口并把响应体整体读取为字节数组，适合导出、下载等小到中等体积响应。 */
  public Mono<byte[]> executeToBytes(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToBytes(
        endpoint, pathVariables, queryParameters, headers, body, contentType);
  }

  /** 执行接口并以分块字节流返回响应体，适合下载、日志、trace 等流式场景。 */
  public Flux<byte[]> executeToBody(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToBody(
        endpoint, pathVariables, queryParameters, headers, body, contentType);
  }

  /** 执行接口并返回响应头，适合 HEAD 或只关心元数据的操作。 */
  public Mono<Map<String, List<String>>> executeToHeaders(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToHeaders(
        endpoint, pathVariables, queryParameters, headers, body, contentType);
  }

  /** 执行接口并释放响应体，适合删除、设置配置等无返回体操作。 */
  public Mono<Void> executeToVoid(
      MinioApiEndpoint endpoint,
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return executor.executeToVoid(endpoint, pathVariables, queryParameters, headers, body, contentType);
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

    /** 使用 minio-java 风格 Provider 配置凭证，并桥接到响应式 provider。 */
    public Builder credentialsProvider(io.minio.reactive.credentials.Provider provider) {
      this.credentialsProvider = ReactiveCredentialsProvider.from(provider);
      return this;
    }

    public Builder webClient(WebClient webClient) {
      this.webClient = webClient;
      return this;
    }

    public ReactiveMinioRawClient build() {
      ReactiveMinioClientConfig config = ReactiveMinioClientConfig.of(endpoint, region);
      WebClient actualWebClient =
          webClient != null ? webClient : WebClient.builder().baseUrl(config.endpoint()).build();
      ReactiveCredentialsProvider actualProvider =
          credentialsProvider != null ? credentialsProvider : ReactiveCredentialsProvider.anonymous();
      return new ReactiveMinioRawClient(
          config, actualProvider, new ReactiveHttpClient(actualWebClient, config), new S3RequestSigner());
    }
  }
}
