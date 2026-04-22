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

  public ReactiveMinioRawClient rawClient() {
    return new ReactiveMinioRawClient(config, credentialsProvider, httpClient, signer);
  }

  public ReactiveMinioAdminClient adminClient() {
    return new ReactiveMinioAdminClient(rawClient());
  }

  public ReactiveMinioKmsClient kmsClient() {
    return new ReactiveMinioKmsClient(rawClient());
  }

  public ReactiveMinioStsClient stsClient() {
    return new ReactiveMinioStsClient(rawClient());
  }

  public ReactiveMinioMetricsClient metricsClient() {
    return new ReactiveMinioMetricsClient(rawClient());
  }

  public ReactiveMinioHealthClient healthClient() {
    return new ReactiveMinioHealthClient(rawClient());
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


  // 目录型 S3 原始接口入口开始

  /** 调用目录接口 `S3_HEAD_OBJECT`，返回原始文本响应。 */
  public Mono<String> s3HeadObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_HEAD_OBJECT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT_ATTRIBUTES`，返回原始文本响应。 */
  public Mono<String> s3GetObjectAttributes(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT_ATTRIBUTES"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_COPY_OBJECT_PART`，返回原始文本响应。 */
  public Mono<String> s3CopyObjectPart(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_COPY_OBJECT_PART"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT_PART`，返回原始文本响应。 */
  public Mono<String> s3PutObjectPart(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT_PART"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_OBJECT_PARTS`，返回原始文本响应。 */
  public Mono<String> s3ListObjectParts(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_OBJECT_PARTS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_COMPLETE_MULTIPART_UPLOAD`，返回原始文本响应。 */
  public Mono<String> s3CompleteMultipartUpload(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_COMPLETE_MULTIPART_UPLOAD"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_CREATE_MULTIPART_UPLOAD`，返回原始文本响应。 */
  public Mono<String> s3CreateMultipartUpload(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_CREATE_MULTIPART_UPLOAD"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_ABORT_MULTIPART_UPLOAD`，返回原始文本响应。 */
  public Mono<String> s3AbortMultipartUpload(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_ABORT_MULTIPART_UPLOAD"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT_ACL`，返回原始文本响应。 */
  public Mono<String> s3GetObjectAcl(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT_ACL"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT_ACL`，返回原始文本响应。 */
  public Mono<String> s3PutObjectAcl(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT_ACL"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT_TAGGING`，返回原始文本响应。 */
  public Mono<String> s3GetObjectTagging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT_TAGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT_TAGGING`，返回原始文本响应。 */
  public Mono<String> s3PutObjectTagging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT_TAGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_OBJECT_TAGGING`，返回原始文本响应。 */
  public Mono<String> s3DeleteObjectTagging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_OBJECT_TAGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_SELECT_OBJECT_CONTENT`，返回原始文本响应。 */
  public Mono<String> s3SelectObjectContent(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_SELECT_OBJECT_CONTENT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT_RETENTION`，返回原始文本响应。 */
  public Mono<String> s3GetObjectRetention(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT_RETENTION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT_LEGAL_HOLD`，返回原始文本响应。 */
  public Mono<String> s3GetObjectLegalHold(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT_LEGAL_HOLD"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT_LAMBDA`，返回原始文本响应。 */
  public Mono<String> s3GetObjectLambda(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT_LAMBDA"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_OBJECT`，返回原始文本响应。 */
  public Mono<String> s3GetObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_OBJECT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_COPY_OBJECT`，返回原始文本响应。 */
  public Mono<String> s3CopyObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_COPY_OBJECT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT_RETENTION`，返回原始文本响应。 */
  public Mono<String> s3PutObjectRetention(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT_RETENTION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT_LEGAL_HOLD`，返回原始文本响应。 */
  public Mono<String> s3PutObjectLegalHold(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT_LEGAL_HOLD"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT_EXTRACT`，返回原始文本响应。 */
  public Mono<String> s3PutObjectExtract(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT_EXTRACT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_OBJECT`，返回原始文本响应。 */
  public Mono<String> s3PutObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_OBJECT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_OBJECT`，返回原始文本响应。 */
  public Mono<String> s3DeleteObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_OBJECT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_POST_RESTORE_OBJECT`，返回原始文本响应。 */
  public Mono<String> s3PostRestoreObject(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_POST_RESTORE_OBJECT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_LOCATION`，返回原始文本响应。 */
  public Mono<String> s3GetBucketLocation(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_LOCATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_POLICY`，返回原始文本响应。 */
  public Mono<String> s3GetBucketPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_POLICY"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_LIFECYCLE`，返回原始文本响应。 */
  public Mono<String> s3GetBucketLifecycle(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_LIFECYCLE"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_ENCRYPTION`，返回原始文本响应。 */
  public Mono<String> s3GetBucketEncryption(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_ENCRYPTION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_OBJECT_LOCK`，返回原始文本响应。 */
  public Mono<String> s3GetBucketObjectLock(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_OBJECT_LOCK"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_REPLICATION`，返回原始文本响应。 */
  public Mono<String> s3GetBucketReplication(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_REPLICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_VERSIONING`，返回原始文本响应。 */
  public Mono<String> s3GetBucketVersioning(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_VERSIONING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_NOTIFICATION`，返回原始文本响应。 */
  public Mono<String> s3GetBucketNotification(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_NOTIFICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LISTEN_BUCKET_NOTIFICATION`，返回原始文本响应。 */
  public Mono<String> s3ListenBucketNotification(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LISTEN_BUCKET_NOTIFICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_RESET_BUCKET_REPLICATION_STATUS`，返回原始文本响应。 */
  public Mono<String> s3ResetBucketReplicationStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_RESET_BUCKET_REPLICATION_STATUS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_ACL`，返回原始文本响应。 */
  public Mono<String> s3GetBucketAcl(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_ACL"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_ACL`，返回原始文本响应。 */
  public Mono<String> s3PutBucketAcl(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_ACL"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_CORS`，返回原始文本响应。 */
  public Mono<String> s3GetBucketCors(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_CORS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_CORS`，返回原始文本响应。 */
  public Mono<String> s3PutBucketCors(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_CORS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_CORS`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketCors(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_CORS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_WEBSITE`，返回原始文本响应。 */
  public Mono<String> s3GetBucketWebsite(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_WEBSITE"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_ACCELERATE`，返回原始文本响应。 */
  public Mono<String> s3GetBucketAccelerate(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_ACCELERATE"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_REQUEST_PAYMENT`，返回原始文本响应。 */
  public Mono<String> s3GetBucketRequestPayment(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_REQUEST_PAYMENT"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_LOGGING`，返回原始文本响应。 */
  public Mono<String> s3GetBucketLogging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_LOGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_TAGGING`，返回原始文本响应。 */
  public Mono<String> s3GetBucketTagging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_TAGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_WEBSITE`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketWebsite(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_WEBSITE"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_TAGGING`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketTagging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_TAGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_MULTIPART_UPLOADS`，返回原始文本响应。 */
  public Mono<String> s3ListMultipartUploads(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_MULTIPART_UPLOADS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_OBJECTS_V2_WITH_METADATA`，返回原始文本响应。 */
  public Mono<String> s3ListObjectsV2WithMetadata(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_OBJECTS_V2_WITH_METADATA"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_OBJECTS_V2`，返回原始文本响应。 */
  public Mono<String> s3ListObjectsV2(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_OBJECTS_V2"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_OBJECT_VERSIONS_WITH_METADATA`，返回原始文本响应。 */
  public Mono<String> s3ListObjectVersionsWithMetadata(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_OBJECT_VERSIONS_WITH_METADATA"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_OBJECT_VERSIONS`，返回原始文本响应。 */
  public Mono<String> s3ListObjectVersions(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_OBJECT_VERSIONS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_POLICY_STATUS`，返回原始文本响应。 */
  public Mono<String> s3GetBucketPolicyStatus(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_POLICY_STATUS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_LIFECYCLE`，返回原始文本响应。 */
  public Mono<String> s3PutBucketLifecycle(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_LIFECYCLE"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_REPLICATION`，返回原始文本响应。 */
  public Mono<String> s3PutBucketReplication(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_REPLICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_ENCRYPTION`，返回原始文本响应。 */
  public Mono<String> s3PutBucketEncryption(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_ENCRYPTION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_POLICY`，返回原始文本响应。 */
  public Mono<String> s3PutBucketPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_POLICY"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_OBJECT_LOCK`，返回原始文本响应。 */
  public Mono<String> s3PutBucketObjectLock(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_OBJECT_LOCK"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_TAGGING`，返回原始文本响应。 */
  public Mono<String> s3PutBucketTagging(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_TAGGING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_VERSIONING`，返回原始文本响应。 */
  public Mono<String> s3PutBucketVersioning(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_VERSIONING"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET_NOTIFICATION`，返回原始文本响应。 */
  public Mono<String> s3PutBucketNotification(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET_NOTIFICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_RESET_BUCKET_REPLICATION_START`，返回原始文本响应。 */
  public Mono<String> s3ResetBucketReplicationStart(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_RESET_BUCKET_REPLICATION_START"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_PUT_BUCKET`，返回原始文本响应。 */
  public Mono<String> s3PutBucket(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_PUT_BUCKET"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_HEAD_BUCKET`，返回原始文本响应。 */
  public Mono<String> s3HeadBucket(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_HEAD_BUCKET"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_POST_POLICY_BUCKET`，返回原始文本响应。 */
  public Mono<String> s3PostPolicyBucket(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_POST_POLICY_BUCKET"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_MULTIPLE_OBJECTS`，返回原始文本响应。 */
  public Mono<String> s3DeleteMultipleObjects(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_MULTIPLE_OBJECTS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_POLICY`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketPolicy(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_POLICY"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_REPLICATION`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketReplication(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_REPLICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_LIFECYCLE`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketLifecycle(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_LIFECYCLE"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET_ENCRYPTION`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucketEncryption(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET_ENCRYPTION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_DELETE_BUCKET`，返回原始文本响应。 */
  public Mono<String> s3DeleteBucket(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_DELETE_BUCKET"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_REPLICATION_METRICS_V2`，返回原始文本响应。 */
  public Mono<String> s3GetBucketReplicationMetricsV2(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_REPLICATION_METRICS_V2"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_GET_BUCKET_REPLICATION_METRICS`，返回原始文本响应。 */
  public Mono<String> s3GetBucketReplicationMetrics(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_GET_BUCKET_REPLICATION_METRICS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_VALIDATE_BUCKET_REPLICATION_CREDS`，返回原始文本响应。 */
  public Mono<String> s3ValidateBucketReplicationCreds(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_VALIDATE_BUCKET_REPLICATION_CREDS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_OBJECTS_V1`，返回原始文本响应。 */
  public Mono<String> s3ListObjectsV1(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_OBJECTS_V1"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LISTEN_ROOT_NOTIFICATION`，返回原始文本响应。 */
  public Mono<String> s3ListenRootNotification(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LISTEN_ROOT_NOTIFICATION"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }


  /** 调用目录接口 `S3_LIST_BUCKETS`，返回原始文本响应。 */
  public Mono<String> s3ListBuckets(
      Map<String, String> pathVariables,
      Map<String, String> queryParameters,
      Map<String, String> headers,
      byte[] body,
      String contentType) {
    return rawClient()
        .executeToString(
            io.minio.reactive.catalog.MinioApiCatalog.byName("S3_LIST_BUCKETS"),
            pathVariables,
            queryParameters,
            headers,
            body,
            contentType);
  }

  // 目录型 S3 原始接口入口结束

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
