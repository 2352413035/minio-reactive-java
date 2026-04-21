package io.minio.reactive;

import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.messages.BucketInfo;
import io.minio.reactive.messages.CompletePart;
import io.minio.reactive.messages.CompletedMultipartUpload;
import io.minio.reactive.messages.DeleteObjectsResult;
import io.minio.reactive.messages.ListObjectsResult;
import io.minio.reactive.messages.ListPartsResult;
import io.minio.reactive.messages.MultipartUpload;
import io.minio.reactive.messages.ObjectInfo;
import io.minio.reactive.messages.PartInfo;
import io.minio.reactive.signer.S3RequestSigner;
import io.minio.reactive.util.S3Escaper;
import io.minio.reactive.util.S3Xml;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 响应式 MinIO 客户端入口。
 *
 * <p>公开 API 保持 Reactor 风格，内部仍然坚持“请求模型 -> 凭证 -> SigV4 -> HTTP”这条链路，
 * 这样后续继续补 S3 子协议时不会把签名、URI、XML 和 WebClient 生命周期混在一起。
 */
public final class ReactiveMinioClient {
  private static final Duration DEFAULT_PRESIGN_EXPIRY = Duration.ofMinutes(15);

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

  public Mono<List<BucketInfo>> listBuckets() {
    S3Request request = request(HttpMethod.GET, null, null).build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseBuckets);
  }

  public Mono<String> getBucketLocation(String bucket) {
    S3Request request =
        request(HttpMethod.GET, bucket, null).queryParameter("location", null).build();

    return sign(request)
        .flatMap(httpClient::exchangeToString)
        .map(S3Xml::parseBucketLocation);
  }

  public Mono<Boolean> bucketExists(String bucket) {
    S3Request request = request(HttpMethod.HEAD, bucket, null).build();

    return sign(request)
        .flatMap(httpClient::exchangeToStatus)
        .map(status -> status >= 200 && status < 300)
        .onErrorResume(
            ReactiveS3Exception.class,
            ex -> ex.statusCode() == 404 ? Mono.just(false) : Mono.error(ex));
  }

  public Mono<Void> makeBucket(String bucket) {
    String body =
        "<CreateBucketConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
            + "<LocationConstraint>"
            + S3Xml.escapeXml(config.region())
            + "</LocationConstraint>"
            + "</CreateBucketConfiguration>";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

    S3Request request =
        request(HttpMethod.PUT, bucket, null)
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();

    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<Void> removeBucket(String bucket) {
    return sign(request(HttpMethod.DELETE, bucket, null).build()).flatMap(httpClient::exchangeToVoid);
  }

  public Flux<ObjectInfo> listObjects(String bucket) {
    return listObjects(bucket, null, true);
  }

  public Flux<ObjectInfo> listObjects(String bucket, String prefix, boolean recursive) {
    String delimiter = recursive ? null : "/";
    return listObjectsPage(bucket, prefix, delimiter, null, null, 1000)
        .expand(
            page -> {
              if (page.isTruncated()
                  && page.nextContinuationToken() != null
                  && !page.nextContinuationToken().isEmpty()) {
                return listObjectsPage(bucket, prefix, delimiter, null, page.nextContinuationToken(), 1000);
              }
              return Mono.empty();
            })
        .flatMapIterable(ListObjectsResult::contents);
  }

  public Mono<ListObjectsResult> listObjectsPage(
      String bucket,
      String prefix,
      String delimiter,
      String startAfter,
      String continuationToken,
      int maxKeys) {
    S3Request.Builder builder =
        request(HttpMethod.GET, bucket, null).queryParameter("list-type", "2");
    if (prefix != null && !prefix.isEmpty()) {
      builder.queryParameter("prefix", prefix);
    }
    if (delimiter != null && !delimiter.isEmpty()) {
      builder.queryParameter("delimiter", delimiter);
    }
    if (startAfter != null && !startAfter.isEmpty()) {
      builder.queryParameter("start-after", startAfter);
    }
    if (continuationToken != null && !continuationToken.isEmpty()) {
      builder.queryParameter("continuation-token", continuationToken);
    }
    if (maxKeys > 0) {
      builder.queryParameter("max-keys", Integer.toString(maxKeys));
    }
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseListObjectsV2);
  }

  public Flux<byte[]> getObject(String bucket, String object) {
    return sign(request(HttpMethod.GET, bucket, object).build()).flatMapMany(httpClient::exchangeToBody);
  }

  public Flux<byte[]> getObjectRange(String bucket, String object, long startInclusive, long endInclusive) {
    if (startInclusive < 0 || endInclusive < startInclusive) {
      throw new IllegalArgumentException("invalid byte range");
    }
    S3Request request =
        request(HttpMethod.GET, bucket, object)
            .header("Range", "bytes=" + startInclusive + "-" + endInclusive)
            .build();
    return sign(request).flatMapMany(httpClient::exchangeToBody);
  }

  public Mono<byte[]> getObjectAsBytes(String bucket, String object) {
    return sign(request(HttpMethod.GET, bucket, object).build()).flatMap(httpClient::exchangeToByteArray);
  }

  public Mono<String> getObjectAsString(String bucket, String object, Charset charset) {
    return getObjectAsBytes(bucket, object).map(bytes -> new String(bytes, charset));
  }

  public Mono<String> getObjectAsString(String bucket, String object) {
    return getObjectAsString(bucket, object, StandardCharsets.UTF_8);
  }

  public Mono<Void> putObject(String bucket, String object, byte[] content, String contentType) {
    byte[] actualContent = content == null ? new byte[0] : content;
    S3Request request =
        request(HttpMethod.PUT, bucket, object)
            .contentType(contentType == null ? "application/octet-stream" : contentType)
            .header("Content-Length", Integer.toString(actualContent.length))
            .body(actualContent)
            .build();

    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<Void> putObject(String bucket, String object, String content, String contentType) {
    return putObject(bucket, object, content.getBytes(StandardCharsets.UTF_8), contentType);
  }

  public Mono<Void> putObject(
      String bucket, String object, Publisher<byte[]> content, String contentType) {
    return Flux.from(content).collectList().map(ReactiveMinioClient::concat).flatMap(bytes -> putObject(bucket, object, bytes, contentType));
  }

  public Mono<Map<String, List<String>>> copyObject(
      String bucket, String object, String sourceBucket, String sourceObject) {
    String copySource = S3Escaper.canonicalUri(sourceBucket, sourceObject);
    S3Request request =
        request(HttpMethod.PUT, bucket, object).header("X-Amz-Copy-Source", copySource).build();
    return sign(request).flatMap(httpClient::exchangeToHeaders);
  }

  public Mono<Void> removeObject(String bucket, String object) {
    return sign(request(HttpMethod.DELETE, bucket, object).build()).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<DeleteObjectsResult> removeObjects(String bucket, Iterable<String> objects) {
    String xml = S3Xml.deleteObjectsXml(objects, false);
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    S3Request request =
        request(HttpMethod.POST, bucket, null)
            .queryParameter("delete", null)
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .header("Content-MD5", md5Base64(bytes))
            .body(bytes)
            .build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseDeleteObjects);
  }

  public Mono<Map<String, List<String>>> statObject(String bucket, String object) {
    return sign(request(HttpMethod.HEAD, bucket, object).build()).flatMap(httpClient::exchangeToHeaders);
  }

  public Mono<Map<String, String>> getObjectTags(String bucket, String object) {
    S3Request request = request(HttpMethod.GET, bucket, object).queryParameter("tagging", null).build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseTagging);
  }

  public Mono<Void> setObjectTags(String bucket, String object, Map<String, String> tags) {
    return putTagging(request(HttpMethod.PUT, bucket, object).queryParameter("tagging", null), tags);
  }

  public Mono<Void> deleteObjectTags(String bucket, String object) {
    S3Request request = request(HttpMethod.DELETE, bucket, object).queryParameter("tagging", null).build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<Map<String, String>> getBucketTags(String bucket) {
    S3Request request = request(HttpMethod.GET, bucket, null).queryParameter("tagging", null).build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseTagging);
  }

  public Mono<Void> setBucketTags(String bucket, Map<String, String> tags) {
    return putTagging(request(HttpMethod.PUT, bucket, null).queryParameter("tagging", null), tags);
  }

  public Mono<Void> deleteBucketTags(String bucket) {
    S3Request request = request(HttpMethod.DELETE, bucket, null).queryParameter("tagging", null).build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<String> getBucketPolicy(String bucket) {
    return getBucketSubresource(bucket, "policy");
  }

  public Mono<Void> setBucketPolicy(String bucket, String policyJson) {
    return putBucketSubresource(bucket, "policy", policyJson, "application/json");
  }

  public Mono<Void> deleteBucketPolicy(String bucket) {
    return deleteBucketSubresource(bucket, "policy");
  }

  public Mono<String> getBucketLifecycle(String bucket) {
    return getBucketSubresource(bucket, "lifecycle");
  }

  public Mono<Void> setBucketLifecycle(String bucket, String lifecycleXml) {
    return putBucketSubresource(bucket, "lifecycle", lifecycleXml, "application/xml");
  }

  public Mono<Void> deleteBucketLifecycle(String bucket) {
    return deleteBucketSubresource(bucket, "lifecycle");
  }

  public Mono<String> getBucketVersioning(String bucket) {
    return getBucketSubresource(bucket, "versioning");
  }

  public Mono<Void> setBucketVersioning(String bucket, String versioningXml) {
    return putBucketSubresource(bucket, "versioning", versioningXml, "application/xml");
  }

  public Mono<String> getBucketNotification(String bucket) {
    return getBucketSubresource(bucket, "notification");
  }

  public Mono<Void> setBucketNotification(String bucket, String notificationXml) {
    return putBucketSubresource(bucket, "notification", notificationXml, "application/xml");
  }

  public Mono<String> getBucketEncryption(String bucket) {
    return getBucketSubresource(bucket, "encryption");
  }

  public Mono<Void> setBucketEncryption(String bucket, String encryptionXml) {
    return putBucketSubresource(bucket, "encryption", encryptionXml, "application/xml");
  }

  public Mono<Void> deleteBucketEncryption(String bucket) {
    return deleteBucketSubresource(bucket, "encryption");
  }

  public Mono<String> getBucketObjectLockConfiguration(String bucket) {
    return getBucketSubresource(bucket, "object-lock");
  }

  public Mono<Void> setBucketObjectLockConfiguration(String bucket, String objectLockXml) {
    return putBucketSubresource(bucket, "object-lock", objectLockXml, "application/xml");
  }

  public Mono<String> getBucketReplication(String bucket) {
    return getBucketSubresource(bucket, "replication");
  }

  public Mono<Void> setBucketReplication(String bucket, String replicationXml) {
    return putBucketSubresource(bucket, "replication", replicationXml, "application/xml");
  }

  public Mono<Void> deleteBucketReplication(String bucket) {
    return deleteBucketSubresource(bucket, "replication");
  }

  public Mono<URI> getPresignedObjectUrl(HttpMethod method, String bucket, String object, Duration expires) {
    S3Request request = request(method, bucket, object).build();
    return credentialsProvider
        .getCredentials()
        .defaultIfEmpty(ReactiveCredentials.anonymous())
        .map(credentials -> signer.presign(request, config, credentials, expires));
  }

  public Mono<URI> getPresignedGetObjectUrl(String bucket, String object, Duration expires) {
    return getPresignedObjectUrl(HttpMethod.GET, bucket, object, expires);
  }

  public Mono<URI> getPresignedGetObjectUrl(String bucket, String object) {
    return getPresignedGetObjectUrl(bucket, object, DEFAULT_PRESIGN_EXPIRY);
  }

  public Mono<MultipartUpload> createMultipartUpload(String bucket, String object, String contentType) {
    S3Request.Builder builder = request(HttpMethod.POST, bucket, object).queryParameter("uploads", null);
    if (contentType != null && !contentType.trim().isEmpty()) {
      builder.header("Content-Type", contentType);
    }
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseCreateMultipartUpload);
  }

  public Mono<PartInfo> uploadPart(
      String bucket, String object, String uploadId, int partNumber, byte[] content) {
    byte[] actualContent = content == null ? new byte[0] : content;
    S3Request request =
        request(HttpMethod.PUT, bucket, object)
            .queryParameter("partNumber", Integer.toString(partNumber))
            .queryParameter("uploadId", uploadId)
            .header("Content-Length", Integer.toString(actualContent.length))
            .body(actualContent)
            .build();
    return sign(request)
        .flatMap(httpClient::exchangeToHeaders)
        .map(headers -> new PartInfo(partNumber, firstHeader(headers, "ETag"), actualContent.length, ""));
  }

  public Mono<ListPartsResult> listParts(String bucket, String object, String uploadId) {
    S3Request request =
        request(HttpMethod.GET, bucket, object).queryParameter("uploadId", uploadId).build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseListParts);
  }

  public Mono<CompletedMultipartUpload> completeMultipartUpload(
      String bucket, String object, String uploadId, List<CompletePart> parts) {
    List<CompletePart> safeParts = parts == null ? Collections.<CompletePart>emptyList() : parts;
    String xml = S3Xml.completeMultipartXml(safeParts);
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    S3Request request =
        request(HttpMethod.POST, bucket, object)
            .queryParameter("uploadId", uploadId)
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseCompleteMultipartUpload);
  }

  public Mono<Void> abortMultipartUpload(String bucket, String object, String uploadId) {
    S3Request request =
        request(HttpMethod.DELETE, bucket, object).queryParameter("uploadId", uploadId).build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  public Mono<CompletedMultipartUpload> uploadMultipartObject(
      final String bucket, final String object, final List<byte[]> parts, final String contentType) {
    return createMultipartUpload(bucket, object, contentType)
        .flatMap(
            upload ->
                Flux.range(0, parts.size())
                    .concatMap(index -> uploadPart(bucket, object, upload.uploadId(), index + 1, parts.get(index)))
                    .map(part -> new CompletePart(part.partNumber(), part.etag()))
                    .collectList()
                    .flatMap(completeParts -> completeMultipartUpload(bucket, object, upload.uploadId(), completeParts))
                    .onErrorResume(
                        error -> abortMultipartUpload(bucket, object, upload.uploadId()).then(Mono.error(error))));
  }

  public Mono<String> executeToString(S3Request request) {
    return sign(request).flatMap(httpClient::exchangeToString);
  }

  public Mono<byte[]> executeToBytes(S3Request request) {
    return sign(request).flatMap(httpClient::exchangeToByteArray);
  }

  public Flux<byte[]> executeToBody(S3Request request) {
    return sign(request).flatMapMany(httpClient::exchangeToBody);
  }

  public Mono<Map<String, List<String>>> executeToHeaders(S3Request request) {
    return sign(request).flatMap(httpClient::exchangeToHeaders);
  }

  public Mono<Void> executeToVoid(S3Request request) {
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  private Mono<Void> putTagging(S3Request.Builder builder, Map<String, String> tags) {
    String xml = S3Xml.taggingXml(tags);
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    S3Request request =
        builder
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  private Mono<String> getBucketSubresource(String bucket, String subresource) {
    S3Request request = request(HttpMethod.GET, bucket, null).queryParameter(subresource, null).build();
    return sign(request).flatMap(httpClient::exchangeToString);
  }

  private Mono<Void> putBucketSubresource(
      String bucket, String subresource, String payload, String contentType) {
    byte[] bytes = payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8);
    S3Request request =
        request(HttpMethod.PUT, bucket, null)
            .queryParameter(subresource, null)
            .contentType(contentType)
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  private Mono<Void> deleteBucketSubresource(String bucket, String subresource) {
    S3Request request = request(HttpMethod.DELETE, bucket, null).queryParameter(subresource, null).build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  private S3Request.Builder request(HttpMethod method, String bucket, String object) {
    return S3Request.builder().method(method).bucket(bucket).object(object).region(config.region());
  }

  private Mono<S3Request> sign(S3Request request) {
    return credentialsProvider
        .getCredentials()
        .defaultIfEmpty(ReactiveCredentials.anonymous())
        .map(credentials -> signer.sign(request, config, credentials));
  }

  private static byte[] concat(List<byte[]> chunks) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (byte[] chunk : chunks) {
      if (chunk != null && chunk.length > 0) {
        output.write(chunk, 0, chunk.length);
      }
    }
    return output.toByteArray();
  }

  private static String firstHeader(Map<String, List<String>> headers, String name) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
        return entry.getValue().get(0);
      }
    }
    return "";
  }

  private static String md5Base64(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      return Base64.getEncoder().encodeToString(digest.digest(bytes));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to calculate MD5", e);
    }
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
