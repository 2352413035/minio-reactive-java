package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.credentials.ReactiveCredentialsProvider;
import io.minio.reactive.credentials.StaticCredentialsProvider;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.http.ReactiveHttpClient;
import io.minio.reactive.http.S3Request;
import io.minio.reactive.messages.AccessControlPolicy;
import io.minio.reactive.messages.BucketInfo;
import io.minio.reactive.messages.BucketAccelerateConfiguration;
import io.minio.reactive.messages.BucketCorsConfiguration;
import io.minio.reactive.messages.BucketLoggingConfiguration;
import io.minio.reactive.messages.BucketNotificationConfiguration;
import io.minio.reactive.messages.BucketPolicyStatus;
import io.minio.reactive.messages.BucketRequestPaymentConfiguration;
import io.minio.reactive.messages.BucketReplicationMetrics;
import io.minio.reactive.messages.BucketVersioningConfiguration;
import io.minio.reactive.messages.BucketWebsiteConfiguration;
import io.minio.reactive.messages.CannedAcl;
import io.minio.reactive.messages.CompletePart;
import io.minio.reactive.messages.CompletedMultipartUpload;
import io.minio.reactive.messages.ComposeSource;
import io.minio.reactive.messages.DeleteObjectsResult;
import io.minio.reactive.messages.ListMultipartUploadsResult;
import io.minio.reactive.messages.ListObjectsResult;
import io.minio.reactive.messages.ListObjectVersionsResult;
import io.minio.reactive.messages.ListPartsResult;
import io.minio.reactive.messages.MultipartUpload;
import io.minio.reactive.messages.MultipartUploadInfo;
import io.minio.reactive.messages.ObjectAttributes;
import io.minio.reactive.messages.ObjectInfo;
import io.minio.reactive.messages.ObjectLegalHoldConfiguration;
import io.minio.reactive.messages.ObjectRetentionConfiguration;
import io.minio.reactive.messages.ObjectVersionInfo;
import io.minio.reactive.messages.ObjectWriteResult;
import io.minio.reactive.messages.PartInfo;
import io.minio.reactive.messages.PostPolicy;
import io.minio.reactive.messages.RestoreObjectRequest;
import io.minio.reactive.messages.SelectObjectContentRequest;
import io.minio.reactive.messages.SelectObjectContentResult;
import io.minio.reactive.signer.S3RequestSigner;
import io.minio.reactive.util.S3Escaper;
import io.minio.reactive.util.S3Xml;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import reactor.core.scheduler.Schedulers;

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
      throw new IllegalArgumentException("byte range 无效");
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

  /**
   * 下载对象到本地文件，默认不覆盖已有文件。
   *
   * <p>这里对齐 minio-java 的 `downloadObject` 语义：先 HEAD 取对象长度/ETag，
   * 再 GET 对象内容，写入同目录临时文件并校验长度，最后原子移动到目标文件。
   */
  public Mono<Void> downloadObject(String bucket, String object, Path filename) {
    return downloadObject(bucket, object, filename, false);
  }

  /** 下载对象到本地文件，可显式控制是否覆盖已有目标文件。 */
  public Mono<Void> downloadObject(String bucket, String object, Path filename, boolean overwrite) {
    final Path target = requireFilePath(filename, "filename");
    if (!overwrite && Files.exists(target)) {
      throw new IllegalArgumentException("下载目标文件已存在: " + target);
    }
    return Mono.zip(statObject(bucket, object), getObjectAsBytes(bucket, object))
        .flatMap(
            tuple ->
                Mono.<Void>fromRunnable(
                        () ->
                            writeDownloadedObject(
                                target,
                                overwrite,
                                contentLength(tuple.getT1()),
                                firstHeader(tuple.getT1(), "ETag"),
                                tuple.getT2()))
                    .subscribeOn(Schedulers.boundedElastic()));
  }

  /** 使用字符串文件名下载对象，便于从 minio-java 迁移。 */
  public Mono<Void> downloadObject(String bucket, String object, String filename) {
    return downloadObject(bucket, object, filename, false);
  }

  /** 使用字符串文件名下载对象，并可显式覆盖已有文件。 */
  public Mono<Void> downloadObject(String bucket, String object, String filename, boolean overwrite) {
    return downloadObject(bucket, object, requireFilePath(filename, "filename"), overwrite);
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

  /**
   * 向已有对象末尾追加字节。
   *
   * <p>MinIO 的 appendObject 语义依赖 `x-amz-write-offset-bytes`：先 HEAD 读取当前对象长度，
   * 再 PUT 同一个对象并带上写入偏移。对象不存在或服务端不支持该扩展时，会按服务端错误返回。
   */
  public Mono<ObjectWriteResult> appendObject(
      String bucket, String object, byte[] content, String contentType) {
    byte[] actualContent = content == null ? new byte[0] : content;
    return statObject(bucket, object)
        .map(ReactiveMinioClient::contentLength)
        .flatMap(offset -> appendObjectAtOffset(bucket, object, actualContent, contentType, offset));
  }

  /** 使用字符串内容追加对象，默认按 UTF-8 转字节。 */
  public Mono<ObjectWriteResult> appendObject(
      String bucket, String object, String content, String contentType) {
    return appendObject(
        bucket,
        object,
        content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8),
        contentType);
  }

  /** 上传本地文件内容并追加到已有对象末尾。 */
  public Mono<ObjectWriteResult> appendObject(
      String bucket, String object, Path filename, String contentType) {
    final Path source = requireFilePath(filename, "filename");
    return Mono.fromCallable(() -> readRegularFile(source))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(bytes -> appendObject(bucket, object, bytes, effectiveContentType(source, contentType)));
  }

  /** 上传本地文件，对齐 minio-java 的 `uploadObject` 方法名。 */
  public Mono<Void> uploadObject(String bucket, String object, Path filename) {
    return uploadObject(bucket, object, filename, null);
  }

  /**
   * 上传本地文件。
   *
   * <p>当前底层 `putObject` 仍以 byte[] 作为请求体，所以这里先在 boundedElastic 线程读取文件。
   * 后续补齐分片/大文件能力时，此方法会继续保留同名入口并升级内部传输方式。
   */
  public Mono<Void> uploadObject(String bucket, String object, Path filename, String contentType) {
    final Path source = requireFilePath(filename, "filename");
    return Mono.fromCallable(() -> readRegularFile(source))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(bytes -> putObject(bucket, object, bytes, effectiveContentType(source, contentType)));
  }

  /** 使用字符串文件名上传本地文件，便于从 minio-java 迁移。 */
  public Mono<Void> uploadObject(String bucket, String object, String filename) {
    return uploadObject(bucket, object, requireFilePath(filename, "filename"), null);
  }

  /** 使用字符串文件名和显式 contentType 上传本地文件。 */
  public Mono<Void> uploadObject(String bucket, String object, String filename, String contentType) {
    return uploadObject(bucket, object, requireFilePath(filename, "filename"), contentType);
  }

  private Mono<ObjectWriteResult> appendObjectAtOffset(
      String bucket, String object, byte[] content, String contentType, long offset) {
    if (offset < 0L) {
      throw new IllegalStateException("对象当前长度未知，无法安全追加");
    }
    S3Request request =
        request(HttpMethod.PUT, bucket, object)
            .contentType(contentType == null ? "application/octet-stream" : contentType)
            .header("Content-Length", Integer.toString(content.length))
            .header("x-amz-write-offset-bytes", Long.toString(offset))
            .body(content)
            .build();
    return sign(request)
        .flatMap(httpClient::exchangeToHeaders)
        .map(headers -> new ObjectWriteResult(bucket, object, headers));
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

  /** 获取对象 ACL，返回 Owner、Grant 和原始 XML。 */
  public Mono<AccessControlPolicy> getObjectAcl(String bucket, String object) {
    S3Request request = request(HttpMethod.GET, bucket, object).queryParameter("acl", null).build();
    return sign(request).flatMap(httpClient::exchangeToString).map(S3Xml::parseAccessControlPolicy);
  }

  /** 使用 canned ACL 设置对象权限，复杂 Grant XML 仍可走 advanced/raw 入口。 */
  public Mono<Void> setObjectCannedAcl(String bucket, String object, CannedAcl cannedAcl) {
    S3Request request =
        request(HttpMethod.PUT, bucket, object)
            .queryParameter("acl", null)
            .header("x-amz-acl", requireCannedAcl(cannedAcl).headerValue())
            .build();
    return sign(request).flatMap(httpClient::exchangeToVoid);
  }

  /**
   * 发起 S3 SelectObjectContent 请求。
   *
   * <p>MinIO/S3 Select 返回事件流，当前阶段先把请求模型和原始响应边界固定下来，后续再按事件流格式升级为 records/stats/progress 的完整 typed 解码。
   */
  public Mono<SelectObjectContentResult> selectObjectContent(
      String bucket, String object, SelectObjectContentRequest selectRequest) {
    String xml = S3Xml.selectObjectContentXml(selectRequest);
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    S3Request request =
        request(HttpMethod.POST, bucket, object)
            .queryParameter("select", null)
            .queryParameter("select-type", "2")
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();
    return sign(request)
        .flatMap(httpClient::exchangeToString)
        .map(SelectObjectContentResult::new);
  }

  /**
   * 获取对象属性摘要。
   *
   * <p>默认请求 ETag、对象大小、存储类型、校验和与分片数量。不同服务端版本可能只返回其中一部分字段，
   * 因此 `ObjectAttributes` 会同时保留原始 XML。
   */
  public Mono<ObjectAttributes> getObjectAttributes(String bucket, String object) {
    return getObjectAttributes(bucket, object, "ETag", "ObjectSize", "StorageClass", "Checksum", "ObjectParts");
  }

  /** 按调用方指定的属性名获取对象属性摘要。 */
  public Mono<ObjectAttributes> getObjectAttributes(String bucket, String object, String... attributes) {
    S3Request.Builder builder = request(HttpMethod.GET, bucket, object).queryParameter("attributes", null);
    String headerValue = joinNonBlank(attributes);
    if (!headerValue.isEmpty()) {
      builder.header("X-Amz-Object-Attributes", headerValue);
    }
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseObjectAttributes);
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

  /** 获取对象保留策略配置；bucket 未启用 object lock 时，服务端会返回明确的 S3 错误。 */
  public Mono<ObjectRetentionConfiguration> getObjectRetention(String bucket, String object) {
    return getObjectRetention(bucket, object, null);
  }

  /** 获取指定版本对象的保留策略配置。 */
  public Mono<ObjectRetentionConfiguration> getObjectRetention(
      String bucket, String object, String versionId) {
    S3Request.Builder builder = request(HttpMethod.GET, bucket, object).queryParameter("retention", null);
    addOptionalQuery(builder, "versionId", versionId);
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseObjectRetention);
  }

  /** 设置对象保留策略配置。 */
  public Mono<Void> setObjectRetention(
      String bucket, String object, ObjectRetentionConfiguration configuration) {
    return setObjectRetention(bucket, object, null, configuration);
  }

  /** 设置指定版本对象的保留策略配置。 */
  public Mono<Void> setObjectRetention(
      String bucket, String object, String versionId, ObjectRetentionConfiguration configuration) {
    return putObjectSubresource(
        bucket, object, "retention", versionId, S3Xml.objectRetentionXml(configuration), "application/xml");
  }

  /** 获取对象 Legal Hold 配置。 */
  public Mono<ObjectLegalHoldConfiguration> getObjectLegalHold(String bucket, String object) {
    return getObjectLegalHold(bucket, object, null);
  }

  /** 获取指定版本对象的 Legal Hold 配置。 */
  public Mono<ObjectLegalHoldConfiguration> getObjectLegalHold(
      String bucket, String object, String versionId) {
    S3Request.Builder builder = request(HttpMethod.GET, bucket, object).queryParameter("legal-hold", null);
    addOptionalQuery(builder, "versionId", versionId);
    return sign(builder.build()).flatMap(httpClient::exchangeToString).map(S3Xml::parseObjectLegalHold);
  }

  /** 设置对象 Legal Hold 配置。 */
  public Mono<Void> setObjectLegalHold(
      String bucket, String object, ObjectLegalHoldConfiguration configuration) {
    return setObjectLegalHold(bucket, object, null, configuration);
  }

  /** 对齐 minio-java：启用对象 Legal Hold。 */
  public Mono<Void> enableObjectLegalHold(String bucket, String object) {
    return enableObjectLegalHold(bucket, object, null);
  }

  /** 对齐 minio-java：启用指定版本对象的 Legal Hold。 */
  public Mono<Void> enableObjectLegalHold(String bucket, String object, String versionId) {
    return setObjectLegalHold(bucket, object, versionId, ObjectLegalHoldConfiguration.enabled());
  }

  /** 对齐 minio-java：关闭对象 Legal Hold。 */
  public Mono<Void> disableObjectLegalHold(String bucket, String object) {
    return disableObjectLegalHold(bucket, object, null);
  }

  /** 对齐 minio-java：关闭指定版本对象的 Legal Hold。 */
  public Mono<Void> disableObjectLegalHold(String bucket, String object, String versionId) {
    return setObjectLegalHold(bucket, object, versionId, ObjectLegalHoldConfiguration.disabled());
  }

  /** 对齐 minio-java：判断对象 Legal Hold 是否启用。 */
  public Mono<Boolean> isObjectLegalHoldEnabled(String bucket, String object) {
    return isObjectLegalHoldEnabled(bucket, object, null);
  }

  /** 对齐 minio-java：判断指定版本对象 Legal Hold 是否启用。 */
  public Mono<Boolean> isObjectLegalHoldEnabled(String bucket, String object, String versionId) {
    return getObjectLegalHold(bucket, object, versionId)
        .map(ObjectLegalHoldConfiguration::enabledValue)
        .onErrorResume(
            ReactiveS3Exception.class,
            error ->
                "NoSuchObjectLockConfiguration".equals(error.errorCode())
                    ? Mono.just(false)
                    : Mono.error(error));
  }

  /** 设置指定版本对象的 Legal Hold 配置。 */
  public Mono<Void> setObjectLegalHold(
      String bucket, String object, String versionId, ObjectLegalHoldConfiguration configuration) {
    return putObjectSubresource(
        bucket, object, "legal-hold", versionId, S3Xml.objectLegalHoldXml(configuration), "application/xml");
  }

  /** 发起归档对象恢复请求。 */
  public Mono<Void> restoreObject(String bucket, String object, RestoreObjectRequest request) {
    return restoreObject(bucket, object, null, request);
  }

  /** 发起指定版本归档对象的恢复请求。 */
  public Mono<Void> restoreObject(
      String bucket, String object, String versionId, RestoreObjectRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("对象恢复请求不能为空");
    }
    S3Request.Builder builder = request(HttpMethod.POST, bucket, object).queryParameter("restore", null);
    addOptionalQuery(builder, "versionId", versionId);
    String xml = S3Xml.restoreObjectXml(request);
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    S3Request s3Request =
        builder
            .contentType("application/xml")
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes)
            .build();
    return sign(s3Request).flatMap(httpClient::exchangeToVoid);
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

  /** 获取 bucket CORS 配置，返回规则列表模型。 */
  public Mono<BucketCorsConfiguration> getBucketCorsConfiguration(String bucket) {
    return getBucketSubresource(bucket, "cors").map(S3Xml::parseBucketCors);
  }

  /** 对齐 minio-java 方法名：获取 bucket CORS 配置。 */
  public Mono<BucketCorsConfiguration> getBucketCors(String bucket) {
    return getBucketCorsConfiguration(bucket);
  }

  /** 设置 bucket CORS 配置。 */
  public Mono<Void> setBucketCorsConfiguration(String bucket, BucketCorsConfiguration configuration) {
    return putBucketSubresource(bucket, "cors", S3Xml.bucketCorsXml(configuration), "application/xml");
  }

  /** 对齐 minio-java 方法名：设置 bucket CORS 配置。 */
  public Mono<Void> setBucketCors(String bucket, BucketCorsConfiguration configuration) {
    return setBucketCorsConfiguration(bucket, configuration);
  }

  /** 删除 bucket CORS 配置。 */
  public Mono<Void> deleteBucketCorsConfiguration(String bucket) {
    return deleteBucketSubresource(bucket, "cors");
  }

  /** 对齐 minio-java 方法名：删除 bucket CORS 配置。 */
  public Mono<Void> deleteBucketCors(String bucket) {
    return deleteBucketCorsConfiguration(bucket);
  }

  /** 获取 bucket 静态网站配置摘要。 */
  public Mono<BucketWebsiteConfiguration> getBucketWebsiteConfiguration(String bucket) {
    return getBucketSubresource(bucket, "website").map(S3Xml::parseBucketWebsite);
  }

  /** 删除 bucket 静态网站配置。 */
  public Mono<Void> deleteBucketWebsiteConfiguration(String bucket) {
    return deleteBucketSubresource(bucket, "website");
  }

  /** 获取 bucket 日志配置摘要。 */
  public Mono<BucketLoggingConfiguration> getBucketLoggingConfiguration(String bucket) {
    return getBucketSubresource(bucket, "logging").map(S3Xml::parseBucketLogging);
  }

  /** 获取 bucket policy status 摘要。 */
  public Mono<BucketPolicyStatus> getBucketPolicyStatus(String bucket) {
    return getBucketSubresource(bucket, "policyStatus").map(S3Xml::parseBucketPolicyStatus);
  }

  /** 获取 bucket accelerate 配置摘要。 */
  public Mono<BucketAccelerateConfiguration> getBucketAccelerateConfiguration(String bucket) {
    return getBucketSubresource(bucket, "accelerate").map(S3Xml::parseBucketAccelerate);
  }

  /** 获取 bucket request payment 配置摘要。 */
  public Mono<BucketRequestPaymentConfiguration> getBucketRequestPaymentConfiguration(String bucket) {
    return getBucketSubresource(bucket, "requestPayment").map(S3Xml::parseBucketRequestPayment);
  }

  /** 获取 bucket ACL，返回 Owner、Grant 和原始 XML。 */
  public Mono<AccessControlPolicy> getBucketAcl(String bucket) {
    return getBucketSubresource(bucket, "acl").map(S3Xml::parseAccessControlPolicy);
  }

  /** 使用 canned ACL 设置 bucket 权限，复杂 Grant XML 仍可走 advanced/raw 入口。 */
  public Mono<Void> setBucketCannedAcl(String bucket, CannedAcl cannedAcl) {
    S3Request request =
        request(HttpMethod.PUT, bucket, null)
            .queryParameter("acl", null)
            .header("x-amz-acl", requireCannedAcl(cannedAcl).headerValue())
            .build();
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

  /** 获取 bucket notification 配置，返回目标、事件和 filter 摘要。 */
  public Mono<BucketNotificationConfiguration> getBucketNotificationConfiguration(String bucket) {
    return getBucketNotification(bucket).map(S3Xml::parseBucketNotification);
  }

  /** 设置 bucket notification 配置。 */
  public Mono<Void> setBucketNotificationConfiguration(
      String bucket, BucketNotificationConfiguration configuration) {
    return setBucketNotification(bucket, S3Xml.bucketNotificationXml(configuration));
  }

  /** 对齐 minio-java：通过写入空 notification 配置删除通知规则。 */
  public Mono<Void> deleteBucketNotification(String bucket) {
    return setBucketNotificationConfiguration(bucket, BucketNotificationConfiguration.empty());
  }

  /**
   * 监听指定 bucket 的事件通知，并以响应式字节流返回服务端推送内容。
   *
   * <p>MinIO/S3 通知监听是长连接事件流，不能像普通配置查询那样收敛成一次性 `Mono<String>`。
   * 调用方应主动订阅、设置超时或在业务停止时取消订阅。
   */
  public Flux<byte[]> listenBucketNotification(String bucket, String events) {
    return endpointExecutor()
        .executeToBody(
            endpoint("S3_LISTEN_BUCKET_NOTIFICATION"),
            map("bucket", bucket),
            map("events", events),
            emptyMap(),
            null,
            null);
  }

  /**
   * 监听根路径事件通知，并以响应式字节流返回服务端推送内容。
   *
   * <p>这是 MinIO 扩展能力，适合运维或测试场景；普通业务代码通常优先监听明确 bucket。
   */
  public Flux<byte[]> listenRootNotification(String events) {
    return endpointExecutor()
        .executeToBody(
            endpoint("S3_LISTEN_ROOT_NOTIFICATION"),
            emptyMap(),
            map("events", events),
            emptyMap(),
            null,
            null);
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

  /** 对齐 minio-java 方法名：获取 bucket 默认对象锁配置。 */
  public Mono<String> getObjectLockConfiguration(String bucket) {
    return getBucketObjectLockConfiguration(bucket);
  }

  public Mono<Void> setBucketObjectLockConfiguration(String bucket, String objectLockXml) {
    return putBucketSubresource(bucket, "object-lock", objectLockXml, "application/xml");
  }

  /** 对齐 minio-java 方法名：设置 bucket 默认对象锁配置。 */
  public Mono<Void> setObjectLockConfiguration(String bucket, String objectLockXml) {
    return setBucketObjectLockConfiguration(bucket, objectLockXml);
  }

  /** 对齐 minio-java：通过写入空对象锁配置删除默认保留配置。 */
  public Mono<Void> deleteObjectLockConfiguration(String bucket) {
    return setBucketObjectLockConfiguration(
        bucket, "<ObjectLockConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"/>");
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

  /** 获取旧版 bucket replication metrics JSON 包装。 */
  public Mono<BucketReplicationMetrics> getBucketReplicationMetrics(String bucket) {
    S3Request request =
        request(HttpMethod.GET, bucket, null).queryParameter("replication-metrics", null).build();
    return sign(request)
        .flatMap(httpClient::exchangeToString)
        .map(raw -> BucketReplicationMetrics.parse("v1", raw));
  }

  /** 获取新版 bucket replication metrics JSON 包装。 */
  public Mono<BucketReplicationMetrics> getBucketReplicationMetricsV2(String bucket) {
    S3Request request =
        request(HttpMethod.GET, bucket, null).queryParameter("replication-metrics", "2").build();
    return sign(request)
        .flatMap(httpClient::exchangeToString)
        .map(raw -> BucketReplicationMetrics.parse("v2", raw));
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

  /**
   * 生成浏览器表单 POST 上传所需的预签名字段。
   *
   * <p>该方法对齐 minio-java 的 `getPresignedPostFormData`：调用方提供 PostPolicy，
   * SDK 使用当前凭证生成 policy、x-amz-date、credential 和 signature。匿名客户端不能生成该表单。
   */
  public Mono<Map<String, String>> getPresignedPostFormData(PostPolicy policy) {
    return credentialsProvider
        .getCredentials()
        .switchIfEmpty(
            Mono.error(
                new IllegalArgumentException("匿名访问不需要也不能生成 presigned POST form-data")))
        .map(credentials -> signer.presignedPostFormData(policy, config, credentials));
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

  /**
   * 使用 multipart copy 上传一个 part，是 composeObject 的底层强类型步骤。
   *
   * <p>该方法直接对齐 S3 的 UploadPartCopy：目标对象使用 multipart uploadId，
   * 源对象放入 `x-amz-copy-source` 头，可选范围放入 `x-amz-copy-source-range`。
   */
  public Mono<PartInfo> uploadPartCopy(
      String bucket, String object, String uploadId, int partNumber, ComposeSource source) {
    if (partNumber < 1) {
      throw new IllegalArgumentException("partNumber 必须为正数");
    }
    if (source == null) {
      throw new IllegalArgumentException("compose source 不能为空");
    }
    S3Request.Builder builder =
        request(HttpMethod.PUT, bucket, object)
            .queryParameter("partNumber", Integer.toString(partNumber))
            .queryParameter("uploadId", requireNonBlank(uploadId, "uploadId 不能为空"))
            .header("X-Amz-Copy-Source", source.copySourceHeader());
    if (source.copySourceRangeHeader() != null) {
      builder.header("X-Amz-Copy-Source-Range", source.copySourceRangeHeader());
    }
    return sign(builder.build())
        .flatMap(httpClient::exchangeToString)
        .map(xml -> S3Xml.parseCopyPartResult(partNumber, xml));
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

  /** 使用多个源对象组合成一个目标对象。 */
  public Mono<CompletedMultipartUpload> composeObject(
      final String bucket,
      final String object,
      final List<ComposeSource> sources,
      final String contentType) {
    final List<ComposeSource> safeSources = requireComposeSources(sources);
    return createMultipartUpload(bucket, object, contentType)
        .flatMap(
            upload ->
                Flux.range(0, safeSources.size())
                    .concatMap(
                        index ->
                            uploadPartCopy(
                                bucket, object, upload.uploadId(), index + 1, safeSources.get(index)))
                    .map(part -> new CompletePart(part.partNumber(), part.etag()))
                    .collectList()
                    .flatMap(
                        completeParts ->
                            completeMultipartUpload(bucket, object, upload.uploadId(), completeParts))
                    .onErrorResume(
                        error -> abortMultipartUpload(bucket, object, upload.uploadId()).then(Mono.error(error))));
  }

  /** 使用多个源对象组合成一个目标对象，contentType 由服务端默认处理。 */
  public Mono<CompletedMultipartUpload> composeObject(
      final String bucket, final String object, final List<ComposeSource> sources) {
    return composeObject(bucket, object, sources, null);
  }

  /** 变参形式的对象组合入口，适合少量源对象的直接调用。 */
  public Mono<CompletedMultipartUpload> composeObject(
      final String bucket, final String object, final ComposeSource... sources) {
    List<ComposeSource> list = new ArrayList<ComposeSource>();
    if (sources != null) {
      Collections.addAll(list, sources);
    }
    return composeObject(bucket, object, list, null);
  }

  /** 单源对象组合入口，本质上仍走 multipart copy，便于与后续多源逻辑保持一致。 */
  public Mono<CompletedMultipartUpload> composeObject(
      final String bucket,
      final String object,
      final String sourceBucket,
      final String sourceObject) {
    return composeObject(bucket, object, ComposeSource.of(sourceBucket, sourceObject));
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

  private Mono<Void> putObjectSubresource(
      String bucket,
      String object,
      String subresource,
      String versionId,
      String payload,
      String contentType) {
    byte[] bytes = payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8);
    S3Request.Builder builder =
        request(HttpMethod.PUT, bucket, object)
            .queryParameter(subresource, null)
            .contentType(contentType)
            .header("Content-Length", Integer.toString(bytes.length))
            .body(bytes);
    addOptionalQuery(builder, "versionId", versionId);
    return sign(builder.build()).flatMap(httpClient::exchangeToVoid);
  }

  private static void addOptionalQuery(S3Request.Builder builder, String key, String value) {
    if (value != null && !value.trim().isEmpty()) {
      builder.queryParameter(key, value);
    }
  }

  private static String joinNonBlank(String... values) {
    if (values == null || values.length == 0) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (String value : values) {
      if (value == null || value.trim().isEmpty()) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append(',');
      }
      builder.append(value.trim());
    }
    return builder.toString();
  }

  private static List<ComposeSource> requireComposeSources(List<ComposeSource> sources) {
    if (sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("compose source 列表不能为空");
    }
    List<ComposeSource> safeSources = new ArrayList<ComposeSource>();
    for (ComposeSource source : sources) {
      if (source == null) {
        throw new IllegalArgumentException("compose source 不能为空");
      }
      safeSources.add(source);
    }
    return safeSources;
  }

  private static String requireNonBlank(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  private static CannedAcl requireCannedAcl(CannedAcl cannedAcl) {
    if (cannedAcl == null) {
      throw new IllegalArgumentException("canned ACL 不能为空");
    }
    return cannedAcl;
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

  private static Path requireFilePath(String filename, String fieldName) {
    if (filename == null || filename.trim().isEmpty()) {
      throw new IllegalArgumentException(fieldName + " 不能为空");
    }
    return requireFilePath(Paths.get(filename), fieldName);
  }

  private static Path requireFilePath(Path path, String fieldName) {
    if (path == null) {
      throw new IllegalArgumentException(fieldName + " 不能为空");
    }
    return path.toAbsolutePath().normalize();
  }

  private static byte[] readRegularFile(Path source) {
    if (!Files.isRegularFile(source)) {
      throw new IllegalArgumentException("上传对象文件不存在或不是普通文件: " + source);
    }
    try {
      return Files.readAllBytes(source);
    } catch (IOException e) {
      throw new IllegalStateException("读取上传对象文件失败: " + source, e);
    }
  }

  private static String effectiveContentType(Path source, String contentType) {
    if (contentType != null && !contentType.trim().isEmpty()) {
      return contentType;
    }
    try {
      String detected = Files.probeContentType(source);
      if (detected != null && !detected.trim().isEmpty()) {
        return detected;
      }
    } catch (IOException e) {
      throw new IllegalStateException("探测上传对象 contentType 失败: " + source, e);
    }
    return "application/octet-stream";
  }

  private static long contentLength(Map<String, List<String>> headers) {
    String value = firstHeader(headers, "Content-Length");
    if (value == null || value.trim().isEmpty()) {
      return -1L;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("对象 Content-Length 不是合法数字: " + value, e);
    }
  }

  private static void writeDownloadedObject(
      Path target, boolean overwrite, long expectedLength, String etag, byte[] bytes) {
    byte[] actualBytes = bytes == null ? new byte[0] : bytes;
    if (expectedLength >= 0 && expectedLength != actualBytes.length) {
      throw new IllegalStateException(
          "下载对象长度校验失败: 期望 " + expectedLength + " 字节，实际 " + actualBytes.length + " 字节");
    }
    try {
      Path parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (!overwrite && Files.exists(target)) {
        throw new IllegalArgumentException("下载目标文件已存在: " + target);
      }
      Path temp = temporaryDownloadPath(target, etag);
      Files.deleteIfExists(temp);
      Files.write(temp, actualBytes);
      if (overwrite) {
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
      } else {
        Files.move(temp, target);
      }
    } catch (IOException e) {
      throw new IllegalStateException("写入下载对象文件失败: " + target, e);
    }
  }

  private static Path temporaryDownloadPath(Path target, String etag) {
    String token = etag == null || etag.trim().isEmpty() ? "no-etag" : etag.trim();
    token = token.replace("\"", "").replaceAll("[^A-Za-z0-9._-]", "_");
    String tempName = target.getFileName().toString() + "." + token + ".part.minio";
    Path parent = target.getParent();
    return parent == null ? Paths.get(tempName).toAbsolutePath().normalize() : parent.resolve(tempName);
  }

  private static String md5Base64(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      return Base64.getEncoder().encodeToString(digest.digest(bytes));
    } catch (Exception e) {
      throw new IllegalStateException("无法计算 MD5", e);
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
  @Deprecated
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
  @Deprecated
  public Mono<String> s3GetObjectAcl(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_ACL"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_OBJECT_ACL`。 */
  @Deprecated
  public Mono<String> s3PutObjectAcl(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_ACL"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_ACL`，不携带请求体。 */
  @Deprecated
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
  @Deprecated
  public Mono<String> s3SelectObjectContent(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_SELECT_OBJECT_CONTENT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_SELECT_OBJECT_CONTENT`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3SelectObjectContent(String bucket, String object) {
    return s3SelectObjectContent(bucket, object, null, null);
  }

  /** 调用 `S3_GET_OBJECT_RETENTION`。 */
  @Deprecated
  public Mono<String> s3GetObjectRetention(String bucket, String object) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_OBJECT_RETENTION"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_OBJECT_LEGAL_HOLD`。 */
  @Deprecated
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
  @Deprecated
  public Mono<String> s3PutObjectRetention(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_RETENTION"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_RETENTION`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutObjectRetention(String bucket, String object) {
    return s3PutObjectRetention(bucket, object, null, null);
  }

  /** 调用 `S3_PUT_OBJECT_LEGAL_HOLD`。 */
  @Deprecated
  public Mono<String> s3PutObjectLegalHold(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_OBJECT_LEGAL_HOLD"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_OBJECT_LEGAL_HOLD`，不携带请求体。 */
  @Deprecated
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
  @Deprecated
  public Mono<String> s3PostRestoreObject(String bucket, String object, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_POST_RESTORE_OBJECT"), map("bucket", bucket, "object", object), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_POST_RESTORE_OBJECT`，不携带请求体。 */
  @Deprecated
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
  @Deprecated
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
  @Deprecated
  public Mono<String> s3GetBucketAcl(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_ACL"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_BUCKET_ACL`。 */
  @Deprecated
  public Mono<String> s3PutBucketAcl(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_ACL"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_ACL`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutBucketAcl(String bucket) {
    return s3PutBucketAcl(bucket, null, null);
  }

  /** 调用 `S3_GET_BUCKET_CORS`。 */
  @Deprecated
  public Mono<String> s3GetBucketCors(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_CORS"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_PUT_BUCKET_CORS`。 */
  @Deprecated
  public Mono<String> s3PutBucketCors(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_CORS"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_CORS`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3PutBucketCors(String bucket) {
    return s3PutBucketCors(bucket, null, null);
  }

  /** 调用 `S3_DELETE_BUCKET_CORS`。 */
  @Deprecated
  public Mono<String> s3DeleteBucketCors(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_CORS"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_CORS`，不携带请求体。 */
  @Deprecated
  public Mono<String> s3DeleteBucketCors(String bucket) {
    return s3DeleteBucketCors(bucket, null, null);
  }

  /** 调用 `S3_GET_BUCKET_WEBSITE`。 */
  @Deprecated
  public Mono<String> s3GetBucketWebsite(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_WEBSITE"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_ACCELERATE`。 */
  @Deprecated
  public Mono<String> s3GetBucketAccelerate(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_ACCELERATE"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_REQUEST_PAYMENT`。 */
  @Deprecated
  public Mono<String> s3GetBucketRequestPayment(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_REQUEST_PAYMENT"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_LOGGING`。 */
  @Deprecated
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
  @Deprecated
  public Mono<String> s3DeleteBucketWebsite(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_DELETE_BUCKET_WEBSITE"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_DELETE_BUCKET_WEBSITE`，不携带请求体。 */
  @Deprecated
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
  @Deprecated
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
  @Deprecated
  public Mono<String> s3PutBucketNotification(String bucket, byte[] body, String contentType) {
    return endpointExecutor()
        .executeToString(endpoint("S3_PUT_BUCKET_NOTIFICATION"), map("bucket", bucket), emptyMap(), emptyMap(), body, contentType);
  }

  /** 调用 `S3_PUT_BUCKET_NOTIFICATION`，不携带请求体。 */
  @Deprecated
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
  @Deprecated
  public Mono<String> s3GetBucketReplicationMetricsV2(String bucket) {
    return endpointExecutor()
        .executeToString(endpoint("S3_GET_BUCKET_REPLICATION_METRICS_V2"), map("bucket", bucket), emptyMap(), emptyMap(), null, null);
  }

  /** 调用 `S3_GET_BUCKET_REPLICATION_METRICS`。 */
  @Deprecated
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
