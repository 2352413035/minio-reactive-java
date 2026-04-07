package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.signer.S3RequestSigner;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 响应式 MinIO 客户端入口。
 *
 * <p>这个类本身尽量保持轻量。每个公开方法基本都遵循同一条调用链：
 *
 * <ol>
 *   <li>根据业务动作构造 {@link S3Request}
 *   <li>获取凭证
 *   <li>使用 SigV4 对请求签名
 *   <li>把签名后的请求交给 HTTP 层发送
 * </ol>
 *
 * <p>这样拆分后，客户端层只负责“编排”，不会把协议细节、签名逻辑、HTTP 细节混在一起。
 */
public final class ReactiveMinioClient {
  private final ReactiveMinioClientConfig config;
  private final ReactiveCredentialsProvider credentialsProvider;
  private final ReactiveHttpClient httpClient;
  private final S3RequestSigner signer;

  private ReactiveMinioClient(
      ReactiveMinioClientConfig config,
      ReactiveCredentialsProvider credentialsProvider,
      ReactiveHttpClient httpClient,
      S3RequestSigner signer) {
    this.config = config;
    this.credentialsProvider = credentialsProvider;
    this.httpClient = httpClient;
    this.signer = signer;
  }

  public static Builder builder() {
    return new Builder();
  }

 public Mono<String> getBucketLocation(String bucket){
     S3Request request = S3Request.builder().method(HttpMethod.GET).bucket(bucket).queryParameter("location",null).build();
     return sign(request)
             .flatMap(httpClient::exchangeToByteArray).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
 }


  public Mono<Boolean> bucketExists(String bucket) {
    // HEAD Bucket 只关心桶是否存在，不需要响应体。
    S3Request request =
        S3Request.builder().method(HttpMethod.HEAD).bucket(bucket).region(config.region()).build();

    return sign(request)
        .flatMap(httpClient::exchangeToStatus)
        .map(status -> status >= 200 && status < 300)
        // 对 bucketExists 来说，404 是正常业务结果，不应该作为异常继续抛出。
        .onErrorResume(
            ReactiveS3Exception.class,
            ex -> ex.statusCode() == 404 ? Mono.just(false) : Mono.error(ex));
  }

  public Mono<Void> makeBucket(String bucket) {
    // 真实 MinIO 调试中已经验证：空的 PUT Bucket 请求可能触发服务端解析
    // LocationConstraint 时出现 EOF，因此这里显式发送 XML。
    String body =
        "<CreateBucketConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
            + "<LocationConstraint>"
            + config.region()
            + "</LocationConstraint>"
            + "</CreateBucketConfiguration>";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

    S3Request request =
        S3Request.builder()
            .method(HttpMethod.PUT)
            .bucket(bucket)
            .region(config.region())
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();

    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<Void> removeBucket(String bucket) {
    S3Request request =
        S3Request.builder().method(HttpMethod.DELETE).bucket(bucket).region(config.region()).build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Flux<byte[]> getObject(String bucket, String object) {
    // 这是流式下载接口，后续做大文件下载时会主要扩展这一条路径。
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.GET)
            .bucket(bucket)
            .object(object)
            .region(config.region())
            .build();

    return sign(request).flatMapMany(httpClient::exchangeToBody);
  }

  public Mono<byte[]> getObjectAsBytes(String bucket, String object) {
    // 这是便捷接口，会把整个对象一次性读入内存，适合示例和小文件。
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.GET)
            .bucket(bucket)
            .object(object)
            .region(config.region())
            .build();

    return sign(request).flatMap(httpClient::exchangeToByteArray);
  }

  public Mono<String> getObjectAsString(String bucket, String object, Charset charset) {
    return getObjectAsBytes(bucket, object).map(bytes -> new String(bytes, charset));
  }

  public Mono<String> getObjectAsString(String bucket, String object) {
    return getObjectAsString(bucket, object, StandardCharsets.UTF_8);
  }

  public Mono<Void> putObject(String bucket, String object, byte[] content, String contentType) {
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.PUT)
            .bucket(bucket)
            .object(object)
            .region(config.region())
            .contentType(contentType)
            .header("Content-Length", Integer.toString(content.length))
            .body(content)
            .build();

    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<Void> putObject(String bucket, String object, String content, String contentType) {
    return putObject(bucket, object, content.getBytes(StandardCharsets.UTF_8), contentType);
  }

  public Mono<Void> removeObject(String bucket, String object) {
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.DELETE)
            .bucket(bucket)
            .object(object)
            .region(config.region())
            .build();

    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<Map<String, List<String>>> statObject(String bucket, String object) {
    // HEAD Object 适合取元数据，不下载对象内容。
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.HEAD)
            .bucket(bucket)
            .object(object)
            .region(config.region())
            .build();

    return sign(request).flatMap(httpClient::exchangeToHeaders);
  }

  private Mono<S3Request> sign(S3Request request) {
    // 凭证提供者保持响应式接口，这样后续对接临时凭证刷新时不需要推翻现有 API。
    return credentialsProvider
        .getCredentials()
        .defaultIfEmpty(ReactiveCredentials.anonymous())
        .map(credentials -> signer.sign(request, config, credentials));
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

    public Builder credentialsProvider(ReactiveCredentialsProvider credentialsProvider) {
      this.credentialsProvider = credentialsProvider;
      return this;
    }

    public Builder webClient(WebClient webClient) {
      this.webClient = webClient;
      return this;
    }

    public ReactiveMinioClient build() {
      ReactiveMinioClientConfig config = ReactiveMinioClientConfig.of(endpoint, region);
      WebClient actualWebClient =
          webClient != null ? webClient : WebClient.builder().baseUrl(config.endpoint()).build();
      ReactiveCredentialsProvider actualProvider =
          credentialsProvider != null ? credentialsProvider : ReactiveCredentialsProvider.anonymous();
      ReactiveHttpClient httpClient = new ReactiveHttpClient(actualWebClient, config);
      S3RequestSigner signer = new S3RequestSigner();
      return new ReactiveMinioClient(config, actualProvider, httpClient, signer);
    }
  }
}
