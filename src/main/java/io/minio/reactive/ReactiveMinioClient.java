package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.messages.BucketInfo;
import io.minio.reactive.messages.BucketVersioningConfiguration;
import io.minio.reactive.messages.CompletePart;
import io.minio.reactive.messages.CompletedMultipartUpload;
import io.minio.reactive.messages.DeleteObjectsResult;
import io.minio.reactive.messages.ListMultipartUploadsResult;
import io.minio.reactive.messages.ListObjectsResult;
import io.minio.reactive.messages.ListObjectVersionsResult;
import io.minio.reactive.messages.ListPartsResult;
import io.minio.reactive.messages.MultipartUpload;
import io.minio.reactive.messages.MultipartUploadInfo;
import io.minio.reactive.messages.ObjectInfo;
import io.minio.reactive.messages.ObjectVersionInfo;
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
import java.util.LinkedHashMap;
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

  private ReactiveMinioEndpointExecutor endpointExecutor() {
    return new ReactiveMinioEndpointExecutor(config, credentialsProvider, httpClient, signer);
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

  public Flux<ObjectVersionInfo> listObjectVersions(String bucket) {
    return listObjectVersions(bucket, null, true);
  }

  public Flux<ObjectVersionInfo> listObjectVersions(String bucket, String prefix, boolean recursive) {
    String delimiter = recursive ? null : "/";
    return listObjectVersionsPage(bucket, prefix, null, null, delimiter, 1000)
        .expand(
            page -> {
              if (page.isTruncated() && !page.nextKeyMarker().isEmpty()) {
                return listObjectVersionsPage(
                    bucket, prefix, page.nextKeyMarker(), page.nextVersionIdMarker(), delimiter, 1000);
              }
              return Mono.empty();
            })
        .flatMapIterable(ListObjectVersionsResult::versions);
  }

  public Mono<ListObjectVersionsResult> listObjectVersionsPage(
      String bucket,
      String prefix,
      String keyMarker,
      String versionIdMarker,
      String delimiter,
      int maxKeys) {
    S3Request.Builder builder = request(HttpMethod.GET, bucket, null).queryParameter("versions", null);
    if (prefix != null && !prefix.isEmpty()) {
      builder.queryParameter("prefix", prefix);
    }
    if (keyMarker != null && !keyMarker.isEmpty()) {
      builder.queryParameter("key-marker", keyMarker);
    }
    if (versionIdMarker != null && !versionIdMarker.isEmpty()) {
      builder.queryParameter("version-id-marker", versionIdMarker);
    }
    if (delimiter != null && !delimiter.isEmpty()) {
      builder.queryParameter("delimiter", delimiter);
    }
    if (maxKeys > 0) {
      builder.queryParameter("max-keys", Integer.toString(maxKeys));
    }
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseListObjectVersions);
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

  /** 获取 bucket versioning 配置，返回强类型对象。 */
  public Mono<BucketVersioningConfiguration> getBucketVersioningConfiguration(String bucket) {
    return getBucketVersioning(bucket).map(S3Xml::parseBucketVersioning);
  }

  /** 设置 bucket versioning 配置。 */
  public Mono<Void> setBucketVersioningConfiguration(
      String bucket, BucketVersioningConfiguration configuration) {
    return setBucketVersioning(bucket, S3Xml.bucketVersioningXml(configuration));
  }

  /** 根据布尔值启用或暂停 bucket versioning。 */
  public Mono<Void> setBucketVersioningEnabled(String bucket, boolean enabled) {
    return setBucketVersioningConfiguration(
        bucket, enabled ? BucketVersioningConfiguration.enabled() : BucketVersioningConfiguration.suspended());
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

  public Flux<MultipartUploadInfo> listMultipartUploads(String bucket) {
    return listMultipartUploads(bucket, null, true);
  }

  public Flux<MultipartUploadInfo> listMultipartUploads(String bucket, String prefix, boolean recursive) {
    String delimiter = recursive ? null : "/";
    return listMultipartUploadsPage(bucket, prefix, null, null, delimiter, 1000)
        .expand(
            page -> {
              if (page.isTruncated() && !page.nextKeyMarker().isEmpty()) {
                return listMultipartUploadsPage(
                    bucket, prefix, page.nextKeyMarker(), page.nextUploadIdMarker(), delimiter, 1000);
              }
              return Mono.empty();
            })
        .flatMapIterable(ListMultipartUploadsResult::uploads);
  }

  public Mono<ListMultipartUploadsResult> listMultipartUploadsPage(
      String bucket,
      String prefix,
      String keyMarker,
      String uploadIdMarker,
      String delimiter,
      int maxUploads) {
    S3Request.Builder builder = request(HttpMethod.GET, bucket, null).queryParameter("uploads", null);
    if (prefix != null && !prefix.isEmpty()) {
      builder.queryParameter("prefix", prefix);
    }
    if (keyMarker != null && !keyMarker.isEmpty()) {
      builder.queryParameter("key-marker", keyMarker);
    }
    if (uploadIdMarker != null && !uploadIdMarker.isEmpty()) {
      builder.queryParameter("upload-id-marker", uploadIdMarker);
    }
    if (delimiter != null && !delimiter.isEmpty()) {
      builder.queryParameter("delimiter", delimiter);
    }
    if (maxUploads > 0) {
      builder.queryParameter("max-uploads", Integer.toString(maxUploads));
    }
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseListMultipartUploads);
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

  /** 目录型方法内部复用的空 Map。 */
  private static Map<String, String> emptyMap() {
    return Collections.<String, String>emptyMap();
  }

  /** 根据可变键值对生成 Map，调用方必须成对传入 key 和 value。 */
  private static Map<String, String> map(String... keyValues) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (int i = 0; i < keyValues.length; i += 2) {
      result.put(keyValues[i], keyValues[i + 1]);
    }
    return result;
  }

  private MinioApiEndpoint endpoint(String endpointName) {
    return MinioApiCatalog.byName(endpointName);
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

  /** 调用 `S3_HEAD_OBJECT`。 */
  @Deprecated
  public Mono<Integer> s3HeadObject(String bucket, String object) {
    return endpointExecutor()
        .executeToStatus(endpoint("S3_HEAD_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_OBJECT_ATTRIBUTES`。 */
  public Mono<String> s3GetObjectAttributes(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_ATTRIBUTES"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_COPY_OBJECT_PART`。 */
  public Mono<String> s3CopyObjectPart(String bucket, String object, String partNumber, String uploadId, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_COPY_OBJECT_PART"), map("bucket", bucket, "object", object), map("partNumber", partNumber, "uploadId", uploadId), emptyMap(), body, contentType);
  }

  /** 调用 `S3_COPY_OBJECT_PART`，不携带请求体。 */
  public Mono<String> s3CopyObjectPart(String bucket, String object, String partNumber, String uploadId) {
    return s3CopyObjectPart(bucket, object, partNumber, uploadId, null, null);
  }

  /** 调用 `S3_PUT_OBJECT_PART`。 */
  @Deprecated
  public Mono<String> s3PutObjectPart(String bucket, String object, String partNumber, String uploadId, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_PART"), map("bucket", bucket, "object", object), map("partNumber", partNumber, "uploadId", uploadId), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_PART`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutObjectPart(String bucket, String object, String partNumber, String uploadId) {
    return s3PutObjectPart(bucket, object, partNumber, uploadId, null, null);
  }

  /** 调用 `S3_LIST_OBJECT_PARTS`。 */
  public Mono<String> s3ListObjectParts(String bucket, String object, String uploadId) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_OBJECT_PARTS"), map("bucket", bucket, "object", object), map("uploadId", uploadId), emptyMap(), null, null);
  }

  /** 调用 `S3_COMPLETE_MULTIPART_UPLOAD`。 */
  @Deprecated
  public Mono<String> s3CompleteMultipartUpload(String bucket, String object, String uploadId, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_COMPLETE_MULTIPART_UPLOAD"), map("bucket", bucket, "object", object), map("uploadId", uploadId), emptyMap(), body, contentType);
  }

  /** 调用 `S3_COMPLETE_MULTIPART_UPLOAD`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3CompleteMultipartUpload(String bucket, String object, String uploadId) {
    return s3CompleteMultipartUpload(bucket, object, uploadId, null, null);
  }

  /** 调用 `S3_CREATE_MULTIPART_UPLOAD`。 */
  @Deprecated
  public Mono<String> s3CreateMultipartUpload(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_CREATE_MULTIPART_UPLOAD"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_CREATE_MULTIPART_UPLOAD`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3CreateMultipartUpload(String bucket, String object) {
    return s3CreateMultipartUpload(bucket, object, null, null);
  }

  /** 调用 `S3_ABORT_MULTIPART_UPLOAD`。 */
  @Deprecated
  public Mono<String> s3AbortMultipartUpload(String bucket, String object, String uploadId, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_ABORT_MULTIPART_UPLOAD"), map("bucket", bucket, "object", object), map("uploadId", uploadId), emptyMap(), body, contentType);
  }

  /** 调用 `S3_ABORT_MULTIPART_UPLOAD`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3AbortMultipartUpload(String bucket, String object, String uploadId) {
    return s3AbortMultipartUpload(bucket, object, uploadId, null, null);
  }

  /** 调用 `S3_GET_OBJECT_ACL`。 */
  public Mono<String> s3GetObjectAcl(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_ACL"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_OBJECT_ACL`。 */
  public Mono<String> s3PutObjectAcl(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_ACL"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_ACL`，不携带请求体。 */
  public Mono<String> s3PutObjectAcl(String bucket, String object) {
    return s3PutObjectAcl(bucket, object, null, null);
  }

  /** 调用 `S3_GET_OBJECT_TAGGING`。 */
  @Deprecated
  public Mono<String> s3GetObjectTagging(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_TAGGING"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_OBJECT_TAGGING`。 */
  @Deprecated
  public Mono<String> s3PutObjectTagging(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_TAGGING"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_TAGGING`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutObjectTagging(String bucket, String object) {
    return s3PutObjectTagging(bucket, object, null, null);
  }

  /** 调用 `S3_DELETE_OBJECT_TAGGING`。 */
  @Deprecated
  public Mono<String> s3DeleteObjectTagging(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_OBJECT_TAGGING"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_OBJECT_TAGGING`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3DeleteObjectTagging(String bucket, String object) {
    return s3DeleteObjectTagging(bucket, object, null, null);
  }

  /** 调用 `S3_SELECT_OBJECT_CONTENT`。 */
  public Mono<String> s3SelectObjectContent(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_SELECT_OBJECT_CONTENT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_SELECT_OBJECT_CONTENT`，不携带请求体。 */
  public Mono<String> s3SelectObjectContent(String bucket, String object) {
    return s3SelectObjectContent(bucket, object, null, null);
  }

  /** 调用 `S3_GET_OBJECT_RETENTION`。 */
  public Mono<String> s3GetObjectRetention(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_RETENTION"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_OBJECT_LEGAL_HOLD`。 */
  public Mono<String> s3GetObjectLegalHold(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_LEGAL_HOLD"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_OBJECT_LAMBDA`。 */
  public Mono<String> s3GetObjectLambda(String bucket, String object, String lambdaArn) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_LAMBDA"), map("bucket", bucket, "object", object), map("lambdaArn", lambdaArn), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_OBJECT`。 */
  @Deprecated
  public Mono<String> s3GetObject(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_COPY_OBJECT`。 */
  public Mono<String> s3CopyObject(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_COPY_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_COPY_OBJECT`，不携带请求体。 */
  public Mono<String> s3CopyObject(String bucket, String object) {
    return s3CopyObject(bucket, object, null, null);
  }

  /** 调用 `S3_PUT_OBJECT_RETENTION`。 */
  public Mono<String> s3PutObjectRetention(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_RETENTION"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_RETENTION`，不携带请求体。 */
  public Mono<String> s3PutObjectRetention(String bucket, String object) {
    return s3PutObjectRetention(bucket, object, null, null);
  }

  /** 调用 `S3_PUT_OBJECT_LEGAL_HOLD`。 */
  public Mono<String> s3PutObjectLegalHold(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_LEGAL_HOLD"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_LEGAL_HOLD`，不携带请求体。 */
  public Mono<String> s3PutObjectLegalHold(String bucket, String object) {
    return s3PutObjectLegalHold(bucket, object, null, null);
  }

  /** 调用 `S3_PUT_OBJECT_EXTRACT`。 */
  public Mono<String> s3PutObjectExtract(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_EXTRACT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_EXTRACT`，不携带请求体。 */
  public Mono<String> s3PutObjectExtract(String bucket, String object) {
    return s3PutObjectExtract(bucket, object, null, null);
  }

  /** 调用 `S3_PUT_OBJECT`。 */
  @Deprecated
  public Mono<String> s3PutObject(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutObject(String bucket, String object) {
    return s3PutObject(bucket, object, null, null);
  }

  /** 调用 `S3_DELETE_OBJECT`。 */
  @Deprecated
  public Mono<String> s3DeleteObject(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_OBJECT`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3DeleteObject(String bucket, String object) {
    return s3DeleteObject(bucket, object, null, null);
  }

  /** 调用 `S3_POST_RESTORE_OBJECT`。 */
  public Mono<String> s3PostRestoreObject(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_POST_RESTORE_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_POST_RESTORE_OBJECT`，不携带请求体。 */
  public Mono<String> s3PostRestoreObject(String bucket, String object) {
    return s3PostRestoreObject(bucket, object, null, null);
  }

  /** 调用 `S3_GET_BUCKET_LOCATION`。 */
  public Mono<String> s3GetBucketLocation(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_LOCATION"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_POLICY`。 */
  public Mono<String> s3GetBucketPolicy(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_POLICY"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_LIFECYCLE`。 */
  public Mono<String> s3GetBucketLifecycle(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_LIFECYCLE"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_ENCRYPTION`。 */
  public Mono<String> s3GetBucketEncryption(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_ENCRYPTION"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_OBJECT_LOCK`。 */
  public Mono<String> s3GetBucketObjectLock(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_OBJECT_LOCK"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_REPLICATION`。 */
  public Mono<String> s3GetBucketReplication(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_REPLICATION"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_VERSIONING`。 */
  @Deprecated
  public Mono<String> s3GetBucketVersioning(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_VERSIONING"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_NOTIFICATION`。 */
  public Mono<String> s3GetBucketNotification(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_NOTIFICATION"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LISTEN_BUCKET_NOTIFICATION`。 */
  public Mono<String> s3ListenBucketNotification(String bucket, String events) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LISTEN_BUCKET_NOTIFICATION"), map("bucket", bucket), map("events", events), emptyMap(), null, null);
  }

  /** 调用 `S3_RESET_BUCKET_REPLICATION_STATUS`。 */
  public Mono<String> s3ResetBucketReplicationStatus(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_RESET_BUCKET_REPLICATION_STATUS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_ACL`。 */
  public Mono<String> s3GetBucketAcl(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_ACL"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_BUCKET_ACL`。 */
  public Mono<String> s3PutBucketAcl(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_ACL"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_ACL`，不携带请求体。 */
  public Mono<String> s3PutBucketAcl(String bucket) {
    return s3PutBucketAcl(bucket, null, null);
  }

  /** 调用 `S3_GET_BUCKET_CORS`。 */
  public Mono<String> s3GetBucketCors(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_CORS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_BUCKET_CORS`。 */
  public Mono<String> s3PutBucketCors(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_CORS"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_CORS`，不携带请求体。 */
  public Mono<String> s3PutBucketCors(String bucket) {
    return s3PutBucketCors(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_CORS`。 */
  public Mono<String> s3DeleteBucketCors(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_CORS"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_CORS`，不携带请求体。 */
  public Mono<String> s3DeleteBucketCors(String bucket) {
    return s3DeleteBucketCors(bucket, null, null);
  }

  /** 调用 `S3_GET_BUCKET_WEBSITE`。 */
  public Mono<String> s3GetBucketWebsite(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_WEBSITE"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_ACCELERATE`。 */
  public Mono<String> s3GetBucketAccelerate(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_ACCELERATE"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_REQUEST_PAYMENT`。 */
  public Mono<String> s3GetBucketRequestPayment(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_REQUEST_PAYMENT"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_LOGGING`。 */
  public Mono<String> s3GetBucketLogging(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_LOGGING"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_TAGGING`。 */
  @Deprecated
  public Mono<String> s3GetBucketTagging(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_TAGGING"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_WEBSITE`。 */
  public Mono<String> s3DeleteBucketWebsite(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_WEBSITE"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_WEBSITE`，不携带请求体。 */
  public Mono<String> s3DeleteBucketWebsite(String bucket) {
    return s3DeleteBucketWebsite(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_TAGGING`。 */
  public Mono<String> s3DeleteBucketTagging(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_TAGGING"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_TAGGING`，不携带请求体。 */
  public Mono<String> s3DeleteBucketTagging(String bucket) {
    return s3DeleteBucketTagging(bucket, null, null);
  }

  /** 调用 `S3_LIST_MULTIPART_UPLOADS`。 */
  public Mono<String> s3ListMultipartUploads(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_MULTIPART_UPLOADS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LIST_OBJECTS_V2_WITH_METADATA`。 */
  public Mono<String> s3ListObjectsV2WithMetadata(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_OBJECTS_V2_WITH_METADATA"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LIST_OBJECTS_V2`。 */
  @Deprecated
  public Mono<String> s3ListObjectsV2(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_OBJECTS_V2"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LIST_OBJECT_VERSIONS_WITH_METADATA`。 */
  public Mono<String> s3ListObjectVersionsWithMetadata(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_OBJECT_VERSIONS_WITH_METADATA"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LIST_OBJECT_VERSIONS`。 */
  public Mono<String> s3ListObjectVersions(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_OBJECT_VERSIONS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_POLICY_STATUS`。 */
  public Mono<String> s3GetBucketPolicyStatus(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_POLICY_STATUS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_BUCKET_LIFECYCLE`。 */
  public Mono<String> s3PutBucketLifecycle(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_LIFECYCLE"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_LIFECYCLE`，不携带请求体。 */
  public Mono<String> s3PutBucketLifecycle(String bucket) {
    return s3PutBucketLifecycle(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_REPLICATION`。 */
  public Mono<String> s3PutBucketReplication(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_REPLICATION"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_REPLICATION`，不携带请求体。 */
  public Mono<String> s3PutBucketReplication(String bucket) {
    return s3PutBucketReplication(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_ENCRYPTION`。 */
  public Mono<String> s3PutBucketEncryption(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_ENCRYPTION"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_ENCRYPTION`，不携带请求体。 */
  public Mono<String> s3PutBucketEncryption(String bucket) {
    return s3PutBucketEncryption(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_POLICY`。 */
  public Mono<String> s3PutBucketPolicy(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_POLICY"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_POLICY`，不携带请求体。 */
  public Mono<String> s3PutBucketPolicy(String bucket) {
    return s3PutBucketPolicy(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_OBJECT_LOCK`。 */
  public Mono<String> s3PutBucketObjectLock(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_OBJECT_LOCK"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_OBJECT_LOCK`，不携带请求体。 */
  public Mono<String> s3PutBucketObjectLock(String bucket) {
    return s3PutBucketObjectLock(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_TAGGING`。 */
  @Deprecated
  public Mono<String> s3PutBucketTagging(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_TAGGING"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_TAGGING`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutBucketTagging(String bucket) {
    return s3PutBucketTagging(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_VERSIONING`。 */
  @Deprecated
  public Mono<String> s3PutBucketVersioning(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_VERSIONING"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_VERSIONING`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutBucketVersioning(String bucket) {
    return s3PutBucketVersioning(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET_NOTIFICATION`。 */
  public Mono<String> s3PutBucketNotification(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_NOTIFICATION"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_NOTIFICATION`，不携带请求体。 */
  public Mono<String> s3PutBucketNotification(String bucket) {
    return s3PutBucketNotification(bucket, null, null);
  }

  /** 调用 `S3_RESET_BUCKET_REPLICATION_START`。 */
  public Mono<String> s3ResetBucketReplicationStart(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_RESET_BUCKET_REPLICATION_START"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_RESET_BUCKET_REPLICATION_START`，不携带请求体。 */
  public Mono<String> s3ResetBucketReplicationStart(String bucket) {
    return s3ResetBucketReplicationStart(bucket, null, null);
  }

  /** 调用 `S3_PUT_BUCKET`。 */
  public Mono<String> s3PutBucket(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET`，不携带请求体。 */
  public Mono<String> s3PutBucket(String bucket) {
    return s3PutBucket(bucket, null, null);
  }

  /** 调用 `S3_HEAD_BUCKET`。 */
  public Mono<Integer> s3HeadBucket(String bucket) {
    return endpointExecutor()
        .executeToStatus(endpoint("S3_HEAD_BUCKET"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_POST_POLICY_BUCKET`。 */
  public Mono<String> s3PostPolicyBucket(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_POST_POLICY_BUCKET"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_POST_POLICY_BUCKET`，不携带请求体。 */
  public Mono<String> s3PostPolicyBucket(String bucket) {
    return s3PostPolicyBucket(bucket, null, null);
  }

  /** 调用 `S3_DELETE_MULTIPLE_OBJECTS`。 */
  public Mono<String> s3DeleteMultipleObjects(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_MULTIPLE_OBJECTS"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_MULTIPLE_OBJECTS`，不携带请求体。 */
  public Mono<String> s3DeleteMultipleObjects(String bucket) {
    return s3DeleteMultipleObjects(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_POLICY`。 */
  public Mono<String> s3DeleteBucketPolicy(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_POLICY"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_POLICY`，不携带请求体。 */
  public Mono<String> s3DeleteBucketPolicy(String bucket) {
    return s3DeleteBucketPolicy(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_REPLICATION`。 */
  public Mono<String> s3DeleteBucketReplication(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_REPLICATION"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_REPLICATION`，不携带请求体。 */
  public Mono<String> s3DeleteBucketReplication(String bucket) {
    return s3DeleteBucketReplication(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_LIFECYCLE`。 */
  public Mono<String> s3DeleteBucketLifecycle(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_LIFECYCLE"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_LIFECYCLE`，不携带请求体。 */
  public Mono<String> s3DeleteBucketLifecycle(String bucket) {
    return s3DeleteBucketLifecycle(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_ENCRYPTION`。 */
  public Mono<String> s3DeleteBucketEncryption(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_ENCRYPTION"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_ENCRYPTION`，不携带请求体。 */
  public Mono<String> s3DeleteBucketEncryption(String bucket) {
    return s3DeleteBucketEncryption(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET`。 */
  public Mono<String> s3DeleteBucket(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET`，不携带请求体。 */
  public Mono<String> s3DeleteBucket(String bucket) {
    return s3DeleteBucket(bucket, null, null);
  }

  /** 调用 `S3_GET_BUCKET_REPLICATION_METRICS_V2`。 */
  public Mono<String> s3GetBucketReplicationMetricsV2(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_REPLICATION_METRICS_V2"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_REPLICATION_METRICS`。 */
  public Mono<String> s3GetBucketReplicationMetrics(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_REPLICATION_METRICS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_VALIDATE_BUCKET_REPLICATION_CREDS`。 */
  public Mono<String> s3ValidateBucketReplicationCreds(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_VALIDATE_BUCKET_REPLICATION_CREDS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LIST_OBJECTS_V1`。 */
  public Mono<String> s3ListObjectsV1(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_OBJECTS_V1"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_LISTEN_ROOT_NOTIFICATION`。 */
  public Mono<String> s3ListenRootNotification(String events) {
    return endpointExecutor()
        .executeToString(endpoint("S3_LISTEN_ROOT_NOTIFICATION"), emptyMap(), map("events", events), emptyMap(), null, null);
  }

  /** 调用 `S3_LIST_BUCKETS`。 */
  public Mono<String> s3ListBuckets() {
    return endpointExecutor()
        .executeToString(endpoint("S3_LIST_BUCKETS"), emptyMap(), emptyMap(), emptyMap(), null, null);
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
