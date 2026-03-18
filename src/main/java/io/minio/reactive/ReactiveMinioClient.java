package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.signer.S3RequestSigner;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive MinIO client entrypoint.
 *
 * <p>This class is intentionally thin. Each public method follows the same flow:
 * build an {@link S3Request}, resolve credentials, sign it with SigV4, then hand it to the
 * HTTP layer.
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

  public Mono<Boolean> bucketExists(String bucket) {
    S3Request request =
        S3Request.builder().method(HttpMethod.HEAD).bucket(bucket).region(config.region()).build();

    return sign(request)
        .flatMap(httpClient::exchangeToStatus)
        .map(status -> status >= 200 && status < 300)
        .onErrorResume(
            ReactiveS3Exception.class,
            ex -> ex.statusCode() == 404 ? Mono.just(false) : Mono.error(ex));
  }

  public Mono<Void> makeBucket(String bucket) {
    // Live MinIO testing showed that sending an empty PUT bucket body can fail while
    // the server tries to parse LocationConstraint. This prototype always sends the XML.
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
    // Streaming download variant. This is the path to keep extending for large objects.
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
    // Convenience download variant. Suitable for examples and small objects.
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
    // The provider stays reactive so future temporary-credential refresh can be added
    // without changing the external API shape.
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
