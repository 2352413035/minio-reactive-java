package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.messages.admin.AddServiceAccountRequest;
import io.minio.reactive.messages.admin.AdminAccountSummary;
import io.minio.reactive.messages.admin.AdminBatchJobList;
import io.minio.reactive.messages.admin.AdminBatchJobDescriptionSummary;
import io.minio.reactive.messages.admin.AdminBatchJobStatusSummary;
import io.minio.reactive.messages.admin.AdminBackgroundHealStatus;
import io.minio.reactive.messages.admin.AdminRebalanceStatus;
import io.minio.reactive.messages.admin.AdminTierStatsSummary;
import io.minio.reactive.messages.admin.AdminTopLocksSummary;
import io.minio.reactive.messages.admin.AdminHealthInfoSummary;
import io.minio.reactive.messages.admin.AdminBinaryResult;
import io.minio.reactive.messages.admin.AdminBucketQuota;
import io.minio.reactive.messages.admin.AdminConfigHelp;
import io.minio.reactive.messages.admin.AdminDataUsageSummary;
import io.minio.reactive.messages.admin.AdminStorageSummary;
import io.minio.reactive.messages.admin.AdminTextResult;
import io.minio.reactive.messages.admin.AdminIdpConfigList;
import io.minio.reactive.messages.admin.AdminPolicyEntities;
import io.minio.reactive.messages.admin.AdminPoolStatusSummary;
import io.minio.reactive.messages.admin.AdminPoolListSummary;
import io.minio.reactive.messages.admin.AdminRemoteTargetList;
import io.minio.reactive.messages.admin.AdminReplicationMrfSummary;
import io.minio.reactive.messages.admin.AdminSiteReplicationPeerIdpSettings;
import io.minio.reactive.messages.admin.AdminSiteReplicationStatusSummary;
import io.minio.reactive.messages.admin.AdminSiteReplicationMetaInfoSummary;
import io.minio.reactive.messages.admin.AdminSiteReplicationInfoSummary;
import io.minio.reactive.messages.admin.AdminTierList;
import io.minio.reactive.messages.admin.EncryptedAdminResponse;
import io.minio.reactive.messages.admin.UpdateGroupMembersRequest;
import io.minio.reactive.messages.kms.KmsJsonResult;
import io.minio.reactive.messages.kms.KmsKeyStatus;
import io.minio.reactive.messages.admin.AddUserRequest;
import io.minio.reactive.messages.BucketCorsConfiguration;
import io.minio.reactive.messages.BucketCorsRule;
import io.minio.reactive.messages.BucketNotificationConfiguration;
import io.minio.reactive.messages.BucketNotificationTarget;
import io.minio.reactive.messages.BucketReplicationMetrics;
import io.minio.reactive.messages.CannedAcl;
import io.minio.reactive.messages.CompletedMultipartUpload;
import io.minio.reactive.messages.ComposeSource;
import io.minio.reactive.messages.ObjectLegalHoldConfiguration;
import io.minio.reactive.messages.ObjectRetentionConfiguration;
import io.minio.reactive.messages.ObjectWriteResult;
import io.minio.reactive.messages.PostPolicy;
import io.minio.reactive.messages.PutObjectFanOutEntry;
import io.minio.reactive.messages.PutObjectFanOutResponse;
import io.minio.reactive.messages.RestoreObjectRequest;
import io.minio.reactive.messages.SelectObjectContentRequest;
import io.minio.reactive.messages.SnowballObject;
import io.minio.reactive.messages.sts.AssumeRoleResult;
import io.minio.reactive.messages.sts.AssumeRoleSsoRequest;
import io.minio.reactive.messages.sts.AssumeRoleWithCertificateRequest;
import io.minio.reactive.messages.sts.AssumeRoleWithCustomTokenRequest;
import io.minio.reactive.util.MadminEncryptionSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ReactiveMinioSpecializedClientsTest {
  @Test
  void shouldExposePeerClientBuilders() {
    Assertions.assertNotNull(
        ReactiveMinioClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioAdminClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioKmsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioStsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioMetricsClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioHealthClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
    Assertions.assertNotNull(
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build());
  }

  @Test
  void shouldExposeRepresentativeCatalogMethodsOnSpecializedClients() {
    assertMonoMethodExists(ReactiveMinioClient.class, "s3ListBuckets");
    assertMonoMethodExists(ReactiveMinioClient.class, "s3GetObject");
    assertMonoMethodExists(ReactiveMinioClient.class, "appendObject");
    assertMonoMethodExists(ReactiveMinioClient.class, "composeObject");
    assertMonoMethodExists(ReactiveMinioClient.class, "downloadObject");
    assertMonoMethodExists(ReactiveMinioClient.class, "getPresignedPostFormData");
    assertMonoMethodExists(ReactiveMinioClient.class, "putObjectFanOut");
    assertMonoMethodExists(ReactiveMinioClient.class, "uploadSnowballObjects");
    assertMonoMethodExists(ReactiveMinioClient.class, "uploadObject");
    assertFluxMethodExists(ReactiveMinioClient.class, "promptObject");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "serverInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addUser");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addUpdateGroup");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "removeGroup");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "attachPolicy");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "detachPolicy");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setPolicy");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "clearBucketQuota");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listServiceAccount");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getServiceAccountInfo");
    assertMonoMethodExists(ReactiveMinioKmsClient.class, "keyStatus");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithWebIdentity");
    assertMonoMethodExists(ReactiveMinioMetricsClient.class, "v3");
    assertMonoMethodExists(ReactiveMinioHealthClient.class, "liveGet");
  }

  @Test
  void shouldKeepSpecializedMethodsAlignedWithCatalogFamilies() {
    Assertions.assertTrue(countDistinctMonoMethods(ReactiveMinioClient.class, "s3") >= 77);
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioAdminClient.class, null)
            >= MinioApiCatalog.byFamily("admin").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioKmsClient.class, null)
            >= MinioApiCatalog.byFamily("kms").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioStsClient.class, null)
            >= MinioApiCatalog.byFamily("sts").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioMetricsClient.class, null)
            >= MinioApiCatalog.byFamily("metrics").size());
    Assertions.assertTrue(
        countDistinctMonoMethods(ReactiveMinioHealthClient.class, null)
            >= MinioApiCatalog.byFamily("health").size());
  }


  @Test
  void shouldKeepAdvancedCompatibilityBaselineForMigration() {
    assertAdvancedBaseline(ReactiveMinioClient.class, 131, 60, 5);
    assertAdvancedBaseline(ReactiveMinioAdminClient.class, 203, 18, 0);
    assertAdvancedBaseline(ReactiveMinioKmsClient.class, 8, 0, 0);
    assertAdvancedBaseline(ReactiveMinioStsClient.class, 14, 6, 0);
    assertAdvancedBaseline(ReactiveMinioMetricsClient.class, 6, 0, 0);
    assertAdvancedBaseline(ReactiveMinioHealthClient.class, 0, 0, 0);
    assertAdvancedBaseline(ReactiveMinioRawClient.class, 3, 0, 8);
  }


  @Test
  void shouldNotExposeEndpointExecutorInPublicApi() {
    Class<?>[] publicTypes = {
      ReactiveMinioClient.class,
      ReactiveMinioAdminClient.class,
      ReactiveMinioKmsClient.class,
      ReactiveMinioStsClient.class,
      ReactiveMinioMetricsClient.class,
      ReactiveMinioHealthClient.class,
      ReactiveMinioRawClient.class
    };
    for (Class<?> type : publicTypes) {
      for (Method method : type.getMethods()) {
        Assertions.assertNotEquals(ReactiveMinioEndpointExecutor.class, method.getReturnType());
        for (Class<?> parameterType : method.getParameterTypes()) {
          Assertions.assertNotEquals(ReactiveMinioEndpointExecutor.class, parameterType);
        }
      }
    }
  }


  @Test
  void shouldTreatHealthBusinessMethodsAsBooleanResults() {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("not ready")
                            .build()))
            .build();
    ReactiveMinioHealthClient client =
        ReactiveMinioHealthClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .build();

    Assertions.assertEquals(Boolean.FALSE, client.isReady().block());
    Assertions.assertEquals(503, client.checkReadiness().block().statusCode());
  }

  @Test
  void shouldUploadAndDownloadObjectFilesLikeMinioJava() throws Exception {
    java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("reactive-minio-file-api-");
    java.nio.file.Path uploadFile = tempDir.resolve("upload.txt");
    java.nio.file.Path downloadFile = tempDir.resolve("nested").resolve("download.txt");
    java.nio.file.Files.write(
        uploadFile, "中文上传内容".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    final String downloadedText = "中文下载内容";
    final byte[] downloadedBytes = downloadedText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    java.util.List<org.springframework.http.HttpMethod> methods =
        new java.util.ArrayList<org.springframework.http.HttpMethod>();
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  methods.add(request.method());
                  paths.add(request.url().getPath());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  if (request.method().equals(org.springframework.http.HttpMethod.HEAD)) {
                    return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .header("Content-Length", Integer.toString(downloadedBytes.length))
                            .header("ETag", "\"file-etag\"")
                            .build());
                  }
                  if (request.method().equals(org.springframework.http.HttpMethod.GET)) {
                    return Mono.just(ClientResponse.create(HttpStatus.OK).body(downloadedText).build());
                  }
                  return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    client.uploadObject("bucket1", "from-file.txt", uploadFile, "text/plain").block();
    client.downloadObject("bucket1", "to-file.txt", downloadFile).block();

    Assertions.assertEquals(
        downloadedText,
        new String(
            java.nio.file.Files.readAllBytes(downloadFile), java.nio.charset.StandardCharsets.UTF_8));
    Assertions.assertTrue(methods.contains(org.springframework.http.HttpMethod.PUT));
    Assertions.assertTrue(methods.contains(org.springframework.http.HttpMethod.HEAD));
    Assertions.assertTrue(methods.contains(org.springframework.http.HttpMethod.GET));
    Assertions.assertTrue(paths.contains("/bucket1/from-file.txt"));
    Assertions.assertTrue(paths.contains("/bucket1/to-file.txt"));
    Assertions.assertTrue(contentTypes.contains("text/plain"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> client.downloadObject("bucket1", "to-file.txt", downloadFile));
  }

  @Test
  void shouldComposeObjectsWithMultipartCopyLikeMinioJava() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> copySources = new java.util.ArrayList<String>();
    java.util.List<String> ranges = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  copySources.add(request.headers().getFirst("X-Amz-Copy-Source"));
                  ranges.add(request.headers().getFirst("X-Amz-Copy-Source-Range"));
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  String query = request.url().getQuery();
                  if (query != null && query.contains("uploads")) {
                    return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .body(
                                "<InitiateMultipartUploadResult>"
                                    + "<Bucket>target</Bucket><Key>merged.txt</Key>"
                                    + "<UploadId>upload-1</UploadId>"
                                    + "</InitiateMultipartUploadResult>")
                            .build());
                  }
                  if (query != null && query.contains("partNumber=")) {
                    String etag = query.contains("partNumber=1") ? "\"part-1\"" : "\"part-2\"";
                    return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .body(
                                "<CopyPartResult><LastModified>2026-04-25T00:00:00Z</LastModified>"
                                    + "<ETag>"
                                    + etag
                                    + "</ETag></CopyPartResult>")
                            .build());
                  }
                  if (query != null && query.contains("uploadId=upload-1")) {
                    return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .body(
                                "<CompleteMultipartUploadResult>"
                                    + "<Location>http://localhost:9000/target/merged.txt</Location>"
                                    + "<Bucket>target</Bucket><Key>merged.txt</Key><ETag>\"merged\"</ETag>"
                                    + "</CompleteMultipartUploadResult>")
                            .build());
                  }
                  return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    CompletedMultipartUpload completed =
        client
            .composeObject(
                "target",
                "merged.txt",
                java.util.Arrays.asList(
                    ComposeSource.of("source", "a.txt").withRange(0, 4),
                    ComposeSource.of("source", "b.txt", "v1")),
                "text/plain")
            .block();

    Assertions.assertEquals("target", completed.bucket());
    Assertions.assertEquals("merged.txt", completed.key());
    Assertions.assertEquals("\"merged\"", completed.etag());
    Assertions.assertTrue(paths.contains("/target/merged.txt"));
    Assertions.assertTrue(containsAllQueryParts(queries, "uploads"));
    Assertions.assertTrue(containsAllQueryParts(queries, "partNumber=1", "uploadId=upload-1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "partNumber=2", "uploadId=upload-1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "uploadId=upload-1"));
    Assertions.assertTrue(copySources.contains("/source/a.txt"));
    Assertions.assertTrue(copySources.contains("/source/b.txt?versionId=v1"));
    Assertions.assertTrue(ranges.contains("bytes=0-4"));
    Assertions.assertTrue(contentTypes.contains("text/plain"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> client.composeObject("target", "empty.txt", java.util.Collections.<ComposeSource>emptyList()));
  }

  @Test
  void shouldGeneratePresignedPostFormDataLikeMinioJava() {
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentials("ak", "sk", "session-token")
            .build();
    PostPolicy policy =
        PostPolicy.of(
            "bucket1",
            java.time.ZonedDateTime.of(
                2026, 4, 25, 0, 0, 0, 0, java.time.ZoneOffset.UTC));
    policy.addEqualsCondition("key", "images/avatar.png");
    policy.addStartsWithCondition("Content-Type", "image/");
    policy.addContentLengthRangeCondition(64L, 1024L);

    java.util.Map<String, String> formData = client.getPresignedPostFormData(policy).block();
    String decodedPolicy =
        new String(
            java.util.Base64.getDecoder().decode(formData.get("policy")),
            java.nio.charset.StandardCharsets.UTF_8);

    Assertions.assertEquals("AWS4-HMAC-SHA256", formData.get("x-amz-algorithm"));
    Assertions.assertTrue(formData.get("x-amz-credential").startsWith("ak/"));
    Assertions.assertEquals("session-token", formData.get("x-amz-security-token"));
    Assertions.assertNotNull(formData.get("x-amz-date"));
    Assertions.assertNotNull(formData.get("x-amz-signature"));
    Assertions.assertTrue(decodedPolicy.contains("\"expiration\":\"2026-04-25T00:00:00.000Z\""));
    Assertions.assertTrue(decodedPolicy.contains("[\"eq\",\"$bucket\",\"bucket1\"]"));
    Assertions.assertTrue(decodedPolicy.contains("[\"eq\",\"$key\",\"images/avatar.png\"]"));
    Assertions.assertTrue(decodedPolicy.contains("[\"starts-with\",\"$Content-Type\",\"image/\"]"));
    Assertions.assertTrue(decodedPolicy.contains("[\"content-length-range\",64,1024]"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> policy.addEqualsCondition("x-amz-signature", "manual"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            ReactiveMinioClient.builder()
                .endpoint("http://localhost:9000")
                .region("us-east-1")
                .build()
                .getPresignedPostFormData(policy)
                .block());
  }

  @Test
  void shouldAppendObjectAtCurrentSizeLikeMinioJava() {
    java.util.List<org.springframework.http.HttpMethod> methods =
        new java.util.ArrayList<org.springframework.http.HttpMethod>();
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> offsets = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  methods.add(request.method());
                  paths.add(request.url().getPath());
                  offsets.add(request.headers().getFirst("x-amz-write-offset-bytes"));
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  if (request.method().equals(org.springframework.http.HttpMethod.HEAD)) {
                    return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                            .header("Content-Length", "5")
                            .header("ETag", "\"old\"")
                            .build());
                  }
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .header("ETag", "\"new\"")
                          .header("x-amz-version-id", "v2")
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    ObjectWriteResult result =
        client.appendObject("bucket1", "append.txt", "追加内容", "text/plain").block();

    Assertions.assertEquals("bucket1", result.bucket());
    Assertions.assertEquals("append.txt", result.object());
    Assertions.assertEquals("\"new\"", result.etag());
    Assertions.assertEquals("v2", result.versionId());
    Assertions.assertTrue(methods.contains(org.springframework.http.HttpMethod.HEAD));
    Assertions.assertTrue(methods.contains(org.springframework.http.HttpMethod.PUT));
    Assertions.assertTrue(paths.contains("/bucket1/append.txt"));
    Assertions.assertTrue(offsets.contains("5"));
    Assertions.assertTrue(contentTypes.contains("text/plain"));
  }

  @Test
  void shouldPromptObjectWithLambdaArnLikeMinioJava() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    java.util.List<String> customHeaders = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  customHeaders.add(request.headers().getFirst("x-test-header"));
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body("推理结果").build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    String result =
        new String(
            client
                .promptObject(
                    "bucket1",
                    "context.txt",
                    "v1",
                    "arn:minio:s3-object-lambda::model",
                    "请总结这个对象",
                    java.util.Collections.<String, Object>singletonMap("temperature", 0.2D),
                    java.util.Collections.singletonMap("x-test-header", "yes"))
                .blockFirst(),
            java.nio.charset.StandardCharsets.UTF_8);

    Assertions.assertEquals("推理结果", result);
    Assertions.assertTrue(paths.contains("/bucket1/context.txt"));
    Assertions.assertTrue(containsAllQueryParts(queries, "lambdaArn=", "versionId=v1"));
    Assertions.assertTrue(contentTypes.contains("application/json"));
    Assertions.assertTrue(customHeaders.contains("yes"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> client.promptObject("bucket1", "context.txt", "lambda", " ").blockFirst());
  }

  @Test
  void shouldPutObjectFanOutWithMultipartPostLikeMinioJava() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    java.util.List<String> authorizations = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  authorizations.add(request.headers().getFirst("Authorization"));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(
                              "{\"key\":\"fan-out-a.txt\",\"etag\":\"etag-a\"}\n"
                                  + "{\"key\":\"fan-out-b.txt\",\"error\":\"denied\"}")
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    PutObjectFanOutResponse response =
        client
            .putObjectFanOut(
                "bucket1",
                "同一份内容",
                java.util.Arrays.asList(
                    PutObjectFanOutEntry.of("fan-out-a.txt"),
                    PutObjectFanOutEntry.of("fan-out-b.txt")),
                "text/plain")
            .block();

    Assertions.assertEquals("bucket1", response.bucket());
    Assertions.assertEquals(2, response.resultCount());
    Assertions.assertEquals("fan-out-a.txt", response.results().get(0).key());
    Assertions.assertTrue(response.results().get(0).success());
    Assertions.assertEquals("denied", response.results().get(1).error());
    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(contentTypes.get(0).startsWith("multipart/form-data"));
    Assertions.assertTrue(authorizations.isEmpty() || authorizations.get(0) == null);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            client
                .putObjectFanOut(
                    "bucket1",
                    "内容",
                    java.util.Collections.<PutObjectFanOutEntry>emptyList(),
                    "text/plain")
                .block());
  }

  @Test
  void shouldUploadSnowballObjectsAsTarLikeMinioJava() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> autoExtractHeaders = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  autoExtractHeaders.add(
                      request.headers().getFirst("X-Amz-Meta-Snowball-Auto-Extract"));
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .header("ETag", "\"snowball\"")
                          .header("x-amz-version-id", "snow-v1")
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    ObjectWriteResult result =
        client
            .uploadSnowballObjects(
                "bucket1",
                java.util.Arrays.asList(
                    SnowballObject.ofString("a.txt", "alpha"),
                    SnowballObject.ofString("/b.txt", "bravo")))
            .block();

    Assertions.assertEquals("bucket1", result.bucket());
    Assertions.assertTrue(result.object().startsWith("snowball."));
    Assertions.assertTrue(result.object().endsWith(".tar"));
    Assertions.assertEquals("\"snowball\"", result.etag());
    Assertions.assertEquals("snow-v1", result.versionId());
    Assertions.assertTrue(paths.get(0).startsWith("/bucket1/snowball."));
    Assertions.assertEquals("true", autoExtractHeaders.get(0));
    Assertions.assertEquals("application/x-tar", contentTypes.get(0));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            client
                .uploadSnowballObjects(
                    "bucket1", java.util.Collections.<SnowballObject>emptyList())
                .block());
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () ->
            client
                .uploadSnowballObjects(
                    "bucket1",
                    java.util.Collections.singletonList(SnowballObject.ofString("a.txt", "a")),
                    true));
  }


  @Test
  void shouldBuildAdminIdentityBusinessRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body("{}").build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.setGroupEnabled("dev", false).block();
    admin.updateGroupMembers(UpdateGroupMembersRequest.add("dev", java.util.Collections.singletonList("user1"))).block();
    admin.addUpdateGroup("ops", "enabled", java.util.Collections.singletonList("user2")).block();
    admin.removeGroup("old-team").block();
    admin.deleteServiceAccountTyped("svc1").block();
    admin.listServiceAccount("root-user").block();
    admin.getServiceAccountInfo("svc2").block();

    Assertions.assertTrue(paths.contains("/minio/admin/v3/set-group-status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/update-group-members"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/delete-service-account"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-service-accounts"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/info-service-account"));
    Assertions.assertTrue(containsAllQueryParts(queries, "group=dev", "status=disabled"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accessKey=svc1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "user=root-user"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accessKey=svc2"));
  }


  @Test
  void shouldBuildPolicyAndKmsBusinessRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body("{}").build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioKmsClient kms =
        ReactiveMinioKmsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioMetricsClient metrics =
        ReactiveMinioMetricsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.putPolicy("readonly", "{\"Version\":\"2012-10-17\"}").block();
    admin.setUserPolicy("readonly", "user1").block();
    admin.setPolicy("dev", true, "readwrite").block();
    admin.attachPolicy(new String[] {"readonly"}, "user1", null).block();
    admin.detachPolicy(new String[] {"readonly"}, null, "dev").block();
    admin.clearBucketQuota("bucket1").block();
    kms.getKeyStatus("key1").block();
    Assertions.assertEquals("kms", kms.scrapeMetrics().block().scope());
    Assertions.assertEquals("legacy", metrics.scrapeLegacyMetrics("token").block().scope());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/add-canned-policy"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/builtin/policy/attach"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/builtin/policy/detach"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/set-bucket-quota"));
    Assertions.assertTrue(paths.contains("/minio/kms/v1/metrics"));
    Assertions.assertTrue(paths.contains("/minio/prometheus/metrics"));
    Assertions.assertTrue(queries.contains("name=readonly"));
    Assertions.assertTrue(containsAllQueryParts(queries, "policyName=readonly", "userOrGroup=user1", "isGroup=false"));
    Assertions.assertTrue(containsAllQueryParts(queries, "policyName=readwrite", "userOrGroup=dev", "isGroup=true"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1"));
    Assertions.assertTrue(queries.contains("key-id=key1"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> admin.attachPolicy(new String[] {"readonly"}, "user1", "dev"));
  }


  @Test
  void shouldExposeStage23KmsAndMetricsTypedMethods() {
    assertMonoMethodExists(ReactiveMinioKmsClient.class, "scrapeMetrics");
    assertMonoMethodExists(ReactiveMinioMetricsClient.class, "scrapeLegacyMetrics");
  }

  @Test
  void shouldExposeStage29StsAdvancedIdentityMethods() {
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleSsoCredentials");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithCertificateCredentials");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleWithCustomTokenCredentials");
    assertAllPublicOverloadsDeprecated(ReactiveMinioStsClient.class, "assumeRoleSsoForm");
    assertAllPublicOverloadsDeprecated(ReactiveMinioStsClient.class, "assumeRoleWithCertificate");
    assertAllPublicOverloadsDeprecated(ReactiveMinioStsClient.class, "assumeRoleWithCustomToken");
  }

  @Test
  void shouldBuildStage29StsAdvancedIdentityRequests() {
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  queries.add(request.url().getQuery());
                  return Mono.just(ClientResponse.create(HttpStatus.OK).body(stsSuccessXml()).build());
                })
            .build();
    ReactiveMinioStsClient sts =
        ReactiveMinioStsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .build();

    AssumeRoleResult sso =
        sts.assumeRoleSsoCredentials(
                AssumeRoleSsoRequest.webIdentity("jwt").withRoleArn("arn:minio:iam:::role/demo"))
            .block();
    AssumeRoleResult certificate =
        sts.assumeRoleWithCertificateCredentials(
                AssumeRoleWithCertificateRequest.create().withDurationSeconds(900))
            .block();
    AssumeRoleResult custom =
        sts.assumeRoleWithCustomTokenCredentials(
                AssumeRoleWithCustomTokenRequest.of("opaque").withRoleArn("arn:minio:iam:::role/demo"))
            .block();

    Assertions.assertEquals("sts-access", sso.credentials().accessKey());
    Assertions.assertEquals("sts-access", certificate.credentials().accessKey());
    Assertions.assertEquals("sts-access", custom.credentials().accessKey());
    Assertions.assertTrue(containsAllQueryParts(queries, "Action=AssumeRoleWithCertificate", "DurationSeconds=900"));
    Assertions.assertTrue(containsAllQueryParts(queries, "Action=AssumeRoleWithCustomToken", "Token=opaque"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> AssumeRoleSsoRequest.webIdentity(""));
    Assertions.assertThrows(IllegalArgumentException.class, () -> AssumeRoleWithCustomTokenRequest.of(""));
  }



  @Test
  void shouldMarkMigratedS3CatalogMethodsDeprecated() {
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3HeadObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3DeleteObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3ListObjectsV2");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3CreateMultipartUpload");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutObjectPart");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3CompleteMultipartUpload");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3AbortMultipartUpload");
  }



  @Test
  void shouldEncryptAddUserRequestPayload() {
    byte[] encrypted =
        MadminEncryptionSupport.encryptData(
            "admin-secret",
            io.minio.reactive.util.JsonSupport.toJsonBytes(
                AddUserRequest.of("user1", "user-secret").toPayload()));

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertTrue(
        new String(
                MadminEncryptionSupport.decryptData("admin-secret", encrypted),
                java.nio.charset.StandardCharsets.UTF_8)
            .contains("user-secret"));
  }


  @Test
  void shouldBuildEncryptedServiceAccountRequestPayload() {
    AddServiceAccountRequest request =
        AddServiceAccountRequest.builder()
            .name("svc1")
            .description("demo service account")
            .policyJson("{\"Version\":\"2012-10-17\"}")
            .build();
    byte[] encrypted =
        MadminEncryptionSupport.encryptData(
            "admin-secret", io.minio.reactive.util.JsonSupport.toJsonBytes(request.toPayload()));
    String decrypted =
        new String(
            MadminEncryptionSupport.decryptData("admin-secret", encrypted),
            java.nio.charset.StandardCharsets.UTF_8);

    Assertions.assertTrue(MadminEncryptionSupport.isEncrypted(encrypted));
    Assertions.assertTrue(decrypted.contains("svc1"));
    Assertions.assertTrue(decrypted.contains("demo service account"));
  }


  @Test
  void shouldExposeServiceAccountBusinessMethod() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addServiceAccount");
    EncryptedAdminResponse response = new EncryptedAdminResponse(new byte[41]);
    Assertions.assertTrue(response.isEncrypted());
  }


  @Test
  void shouldExposeConfigWriteBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setConfigKvText");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setConfigText");
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .credentials("ak", "sk")
            .build();

    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setConfigKvText(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setConfigText(""));
  }



  @Test
  void shouldExposeAccessKeyBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getAccessKeyInfoEncrypted");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listAccessKeysEncrypted");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getAccessKeyInfoTyped");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listAccessKeysTyped");
  }


  @Test
  void shouldParseStage15AdminPlaintextSafeModels() {
    AdminConfigHelp help =
        AdminConfigHelp.parse(
            "{\"subSys\":\"api\",\"description\":\"API 配置\",\"multipleTargets\":false,\"keysHelp\":[{\"key\":\"requests_max\",\"description\":\"最大请求数\",\"optional\":true,\"type\":\"number\",\"multipleTargets\":false}]}");
    Assertions.assertEquals("api", help.subSys());
    Assertions.assertEquals("requests_max", help.keys().get(0));
    Assertions.assertTrue(help.keysHelp().get(0).optional());

    AdminStorageSummary storage =
        AdminStorageSummary.parse(
            "{\"Disks\":[{\"healing\":true},{\"healing\":false}],\"Backend\":{\"Type\":1,\"OnlineDisks\":{\"pool-0\":2},\"OfflineDisks\":{\"pool-0\":1}}}");
    Assertions.assertEquals(2, storage.diskCount());
    Assertions.assertEquals(2, storage.onlineDiskCount());
    Assertions.assertEquals(1, storage.offlineDiskCount());
    Assertions.assertEquals(1, storage.healingDiskCount());

    AdminDataUsageSummary usage =
        AdminDataUsageSummary.parse(
            "{\"objectsCount\":7,\"objectsTotalSize\":4096,\"bucketsCount\":3,\"capacity\":100,\"freeCapacity\":60,\"usedCapacity\":40}");
    Assertions.assertEquals(7, usage.objectsCount());
    Assertions.assertEquals(4096, usage.objectsTotalSize());
    Assertions.assertEquals(3, usage.bucketsCount());
    Assertions.assertEquals(40, usage.totalUsedCapacity());

    AdminAccountSummary account =
        AdminAccountSummary.parse(
            "{\"AccountName\":\"root\",\"Server\":{\"Type\":1},\"Policy\":{\"Version\":\"2012-10-17\"},\"Buckets\":[{\"name\":\"a\",\"access\":{\"read\":true,\"write\":true}},{\"name\":\"b\",\"access\":{\"read\":true,\"write\":false}}]}");
    Assertions.assertEquals("root", account.accountName());
    Assertions.assertEquals(2, account.bucketCount());
    Assertions.assertEquals(2, account.readableBucketCount());
    Assertions.assertEquals(1, account.writableBucketCount());
    Assertions.assertTrue(account.policyJson().contains("2012-10-17"));

    AdminBackgroundHealStatus healStatus =
        AdminBackgroundHealStatus.parse(
            "{\"ScannedItemsCount\":12,\"offline_nodes\":[\"node1\"],\"HealDisks\":[\"disk1\",\"disk2\"],\"sets\":[{}],\"mrf\":{\"node1\":{}},\"sc_parity\":{\"STANDARD\":2}}");
    Assertions.assertEquals(12L, healStatus.scannedItemsCount());
    Assertions.assertEquals(1, healStatus.offlineEndpointCount());
    Assertions.assertEquals(2, healStatus.healDiskCount());
    Assertions.assertEquals(1, healStatus.setCount());
    Assertions.assertEquals(1, healStatus.mrfNodeCount());
    Assertions.assertEquals(1, healStatus.storageClassParityCount());

    AdminRebalanceStatus rebalanceStatus =
        AdminRebalanceStatus.parse(
            "{\"ID\":\"rebalance-1\",\"pools\":[{\"id\":0,\"status\":\"active\"},{\"id\":1,\"status\":\"\"}]}");
    Assertions.assertEquals("rebalance-1", rebalanceStatus.operationId());
    Assertions.assertEquals(2, rebalanceStatus.poolCount());
    Assertions.assertEquals(1, rebalanceStatus.activePoolCount());

    AdminTierStatsSummary tierStats =
        AdminTierStatsSummary.parse(
            "[{\"Name\":\"ARCHIVE\",\"Type\":\"s3\",\"Stats\":{\"totalSize\":1024,\"numVersions\":3,\"numObjects\":2}}]");
    Assertions.assertEquals(1, tierStats.tierCount());
    Assertions.assertEquals("ARCHIVE", tierStats.tierNames().get(0));
    Assertions.assertEquals(1024L, tierStats.totalSize());
    Assertions.assertEquals(3L, tierStats.totalVersions());
    Assertions.assertEquals(2L, tierStats.totalObjects());

    AdminTopLocksSummary topLocks =
        AdminTopLocksSummary.parse(
            "[{\"type\":\"Write\",\"elapsed\":300,\"serverlist\":[\"node1\"],\"quorum\":2},{\"type\":\"Read\",\"elapsed\":100,\"serverlist\":[\"node1\",\"node2\"],\"quorum\":2}]");
    Assertions.assertEquals(2, topLocks.lockCount());
    Assertions.assertEquals(1, topLocks.writeLockCount());
    Assertions.assertEquals(1, topLocks.readLockCount());
    Assertions.assertEquals(1, topLocks.belowQuorumLockCount());
    Assertions.assertEquals(300L, topLocks.maxElapsedNanos());

    AdminHealthInfoSummary healthInfo =
        AdminHealthInfoSummary.parse(
            "{\"version\":\"3\",\"timestamp\":\"2026-04-24T00:00:00Z\",\"minio\":{\"info\":{\"deploymentID\":\"dep\",\"region\":\"us-east-1\",\"buckets\":{\"count\":2},\"objects\":{\"count\":5},\"servers\":[{},{}]}}}");
    Assertions.assertEquals("success", healthInfo.status());
    Assertions.assertEquals("3", healthInfo.version());
    Assertions.assertEquals("dep", healthInfo.deploymentId());
    Assertions.assertEquals("us-east-1", healthInfo.region());
    Assertions.assertEquals(2, healthInfo.serverCount());
    Assertions.assertEquals(2L, healthInfo.bucketCount());
    Assertions.assertEquals(5L, healthInfo.objectCount());
    Assertions.assertEquals("error", AdminHealthInfoSummary.parse("{\"error\":\"boom\"}").status());

    AdminSiteReplicationInfoSummary siteInfo =
        AdminSiteReplicationInfoSummary.parse(
            "{\"enabled\":true,\"name\":\"primary\",\"sites\":[{\"name\":\"a\"},{\"name\":\"b\"}],\"serviceAccountAccessKey\":\"svc-key\",\"apiVersion\":\"1\"}");
    Assertions.assertTrue(siteInfo.enabled());
    Assertions.assertEquals("primary", siteInfo.name());
    Assertions.assertEquals(2, siteInfo.siteCount());
    Assertions.assertTrue(siteInfo.serviceAccountAccessKeyPresent());
    Assertions.assertEquals("1", siteInfo.apiVersion());

    AdminSiteReplicationStatusSummary siteStatus =
        AdminSiteReplicationStatusSummary.parse(
            "{\"Enabled\":true,\"MaxBuckets\":2,\"MaxUsers\":3,\"MaxGroups\":4,\"MaxPolicies\":5,\"Sites\":{\"dep1\":{\"name\":\"a\"},\"dep2\":{\"name\":\"b\"}},\"BucketStats\":{\"bucket1\":{\"dep1\":{}}},\"PolicyStats\":{\"readonly\":{\"dep1\":{}}},\"UserStats\":{\"user1\":{\"dep1\":{}}},\"GroupStats\":{\"dev\":{\"dep1\":{}}},\"ILMExpiryStats\":{\"rule1\":{\"dep1\":{}}},\"Metrics\":{\"pending\":1},\"apiVersion\":\"1\"}");
    Assertions.assertTrue(siteStatus.enabled());
    Assertions.assertEquals(2, siteStatus.siteCount());
    Assertions.assertEquals(2, siteStatus.maxBuckets());
    Assertions.assertEquals(3, siteStatus.maxUsers());
    Assertions.assertEquals(4, siteStatus.maxGroups());
    Assertions.assertEquals(5, siteStatus.maxPolicies());
    Assertions.assertEquals(1, siteStatus.bucketStatsEntryCount());
    Assertions.assertEquals(1, siteStatus.policyStatsEntryCount());
    Assertions.assertEquals(1, siteStatus.userStatsEntryCount());
    Assertions.assertEquals(1, siteStatus.groupStatsEntryCount());
    Assertions.assertEquals(1, siteStatus.ilmExpiryStatsEntryCount());
    Assertions.assertTrue(siteStatus.metricsPresent());

    AdminSiteReplicationMetaInfoSummary siteMeta =
        AdminSiteReplicationMetaInfoSummary.parse(
            "{\"Enabled\":true,\"Name\":\"primary\",\"DeploymentID\":\"dep1\",\"Buckets\":{\"bucket1\":{}},\"Policies\":{\"readonly\":{}},\"UserInfoMap\":{\"user1\":{}},\"GroupDescMap\":{\"dev\":{}},\"ReplicationCfg\":{\"bucket1\":{}},\"ILMExpiryRules\":{\"rule1\":{}},\"apiVersion\":\"1\"}");
    Assertions.assertTrue(siteMeta.enabled());
    Assertions.assertEquals("primary", siteMeta.name());
    Assertions.assertEquals("dep1", siteMeta.deploymentId());
    Assertions.assertEquals(1, siteMeta.bucketCount());
    Assertions.assertEquals(1, siteMeta.policyCount());
    Assertions.assertEquals(1, siteMeta.userCount());
    Assertions.assertEquals(1, siteMeta.groupCount());
    Assertions.assertEquals(1, siteMeta.replicationConfigCount());
    Assertions.assertEquals(1, siteMeta.ilmExpiryRuleCount());

    AdminPoolStatusSummary poolStatus =
        AdminPoolStatusSummary.parse(
            "{\"id\":0,\"cmdline\":\"pool-0\",\"lastUpdate\":\"2026-04-24T00:00:00Z\",\"decommissionInfo\":{\"totalSize\":100,\"currentSize\":60,\"complete\":false,\"failed\":false,\"canceled\":false,\"objectsDecommissioned\":7,\"objectsDecommissionedFailed\":1,\"bytesDecommissioned\":40,\"bytesDecommissionedFailed\":2}}");
    Assertions.assertEquals(0, poolStatus.id());
    Assertions.assertEquals("pool-0", poolStatus.commandLine());
    Assertions.assertTrue(poolStatus.decommissionInfoPresent());
    Assertions.assertFalse(poolStatus.decommissionComplete());
    Assertions.assertEquals(100L, poolStatus.totalSize());
    Assertions.assertEquals(60L, poolStatus.currentSize());
    Assertions.assertEquals(7L, poolStatus.objectsDecommissioned());
    Assertions.assertEquals(1L, poolStatus.objectsDecommissionedFailed());

    AdminPoolListSummary poolList =
        AdminPoolListSummary.parse(
            "[{\"id\":0,\"cmdline\":\"pool-0\",\"decommissionInfo\":{\"totalSize\":100,\"currentSize\":60,\"complete\":false}},{\"id\":1,\"cmdline\":\"pool-1\",\"decommissionInfo\":{\"totalSize\":50,\"currentSize\":0,\"complete\":true}}]");
    Assertions.assertEquals(2, poolList.poolCount());
    Assertions.assertEquals(2, poolList.decommissionInfoCount());
    Assertions.assertEquals(1, poolList.activeDecommissionCount());
    Assertions.assertEquals(1, poolList.completedDecommissionCount());
    Assertions.assertEquals(150L, poolList.totalSize());
    Assertions.assertEquals(60L, poolList.currentSize());

    AdminBucketQuota quota =
        AdminBucketQuota.parse("{\"quota\":1024,\"size\":2048,\"rate\":0,\"requests\":0,\"quotatype\":\"hard\"}");
    Assertions.assertEquals(1024, quota.quota());
    Assertions.assertEquals(2048, quota.size());
    Assertions.assertEquals("hard", quota.quotaType());

    AdminTierList tiers =
        AdminTierList.parse(
            "[{\"Version\":\"v1\",\"Type\":\"s3\",\"Name\":\"archive\"},{\"Version\":\"v1\",\"Type\":\"minio\",\"Name\":\"backup\"}]");
    Assertions.assertEquals(2, tiers.tierCount());
    Assertions.assertEquals("archive", tiers.tiers().get(0).name());

    AdminPolicyEntities entities =
        AdminPolicyEntities.parse(
            "{\"UserMappings\":{\"user1\":[\"readonly\"]},\"GroupMappings\":{\"dev\":[\"readwrite\"]},\"PolicyMappings\":{\"readonly\":[\"user1\"]}}");
    Assertions.assertEquals(3, entities.totalMappingCount());

    AdminIdpConfigList idps = AdminIdpConfigList.parse("openid", "{\"primary\":{}}");
    Assertions.assertEquals("openid", idps.type());
    Assertions.assertEquals("primary", idps.names().get(0));

    AdminRemoteTargetList targets =
        AdminRemoteTargetList.parse(
            "{\"Targets\":[{\"Arn\":\"arn:minio:replication::bucket:target\",\"Type\":\"replication\",\"Endpoint\":\"http://remote\",\"Secure\":true}]}");
    Assertions.assertEquals(1, targets.targetCount());
    Assertions.assertTrue(targets.targets().get(0).secure());

    AdminReplicationMrfSummary mrf =
        AdminReplicationMrfSummary.parse(
            "   \n"
                + "{\"nodeName\":\"node-1\",\"bucket\":\"bucket1\",\"object\":\"a.txt\",\"versionId\":\"v1\",\"retryCount\":2}\n"
                + "{\"NodeName\":\"node-2\",\"Bucket\":\"bucket1\",\"Object\":\"b.txt\",\"VersionID\":\"v2\",\"RetryCount\":3,\"Err\":\"temporary\"}");
    Assertions.assertEquals(2, mrf.entryCount());
    Assertions.assertEquals(1, mrf.errorCount());
    Assertions.assertEquals(5, mrf.totalRetryCount());
    Assertions.assertEquals("b.txt", mrf.entries().get(1).object());

    AdminBatchJobList jobs =
        AdminBatchJobList.parse(
            "{\"Jobs\":[{\"ID\":\"job-1\",\"Type\":\"replicate\",\"Status\":\"running\",\"User\":\"root\"}]}");
    Assertions.assertEquals(1, jobs.jobCount());
    Assertions.assertEquals("running", jobs.jobs().get(0).status());

    AdminBatchJobStatusSummary jobStatus =
        AdminBatchJobStatusSummary.parse(
            "{\"LastMetric\":{\"JobID\":\"replicate-job-1\",\"JobType\":\"replicate\",\"RetryAttempts\":2,\"Complete\":false,\"Failed\":false,\"StartTime\":\"2026-04-24T00:00:00Z\",\"LastUpdate\":\"2026-04-24T00:01:00Z\"}}");
    Assertions.assertEquals("replicate-job-1", jobStatus.jobId());
    Assertions.assertEquals("replicate", jobStatus.jobType());
    Assertions.assertEquals("running", jobStatus.status());
    Assertions.assertEquals(2, jobStatus.retryAttempts());
    Assertions.assertFalse(jobStatus.complete());
    Assertions.assertFalse(jobStatus.failed());

    AdminBatchJobDescriptionSummary jobDescription =
        AdminBatchJobDescriptionSummary.parse(
            "id: replicate-job-1\nuser: root\nstarted: 2026-04-24T00:00:00Z\nreplicate:\n  source: {}\n");
    Assertions.assertEquals("replicate-job-1", jobDescription.id());
    Assertions.assertEquals("root", jobDescription.user());
    Assertions.assertEquals("replicate", jobDescription.jobType());
    Assertions.assertTrue(jobDescription.rawText().contains("replicate"));
  }


  @Test
  void shouldBuildStage15AdminPlaintextSafeRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage15AdminResponseBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    admin.getConfigHelp("api", "requests_max").block();
    Assertions.assertEquals(3, admin.listPolicyEntities().block().totalMappingCount());
    Assertions.assertEquals("primary", admin.listIdpConfigs("openid").block().names().get(0));
    Assertions.assertTrue(admin.listIdpConfigsEncrypted("openid").block().encryptedData().length > 0);
    Assertions.assertEquals("ok", admin.getIdpConfigInfo("openid", "primary").block().values().get("status"));
    Assertions.assertTrue(admin.getIdpConfigEncrypted("openid", "primary").block().encryptedData().length > 0);
    admin.getStorageSummary().block();
    admin.getDataUsageSummary().block();
    admin.getAccountSummary().block();
    Assertions.assertTrue(admin.getAccessKeyInfoEncrypted("svc1").block().encryptedData().length > 0);
    Assertions.assertTrue(admin.listAccessKeysEncrypted("all").block().encryptedData().length > 0);
    Assertions.assertEquals("enabled", admin.getTemporaryAccountInfo("tmp1").block().accountStatus());
    Assertions.assertEquals(
        "enabled", admin.listBucketUsersInfo("bucket1").block().users().get("user1").status());
    admin.getBucketQuotaInfo("bucket1").block();
    admin.listTiers().block();
    Assertions.assertEquals(2, admin.listPoolsSummary().block().poolCount());
    Assertions.assertEquals(60L, admin.getPoolStatusSummary("pool-0").block().currentSize());
    Assertions.assertEquals(1, admin.listRemoteTargetsInfo("bucket1", "replication").block().targetCount());
    Assertions.assertEquals(1, admin.listBatchJobsInfo().block().jobCount());
    Assertions.assertEquals("running", admin.getBatchJobStatusInfo().block().values().get("status"));
    Assertions.assertEquals("replicate", admin.getBatchJobStatusSummary("job-1").block().jobType());
    Assertions.assertEquals("job-1", admin.describeBatchJobInfo().block().values().get("id"));
    Assertions.assertEquals("replicate", admin.describeBatchJobSummary("job-1").block().jobType());
    Assertions.assertEquals(12L, admin.getBackgroundHealStatusSummary().block().scannedItemsCount());
    admin.getBackgroundHealStatus().block();
    admin.listPoolsInfo().block();
    admin.getPoolStatus("pool-0").block();
    Assertions.assertEquals(1, admin.getRebalanceStatusSummary().block().activePoolCount());
    admin.getRebalanceStatus().block();
    Assertions.assertEquals(1, admin.getTierStatsSummary().block().tierCount());
    admin.getTierStats().block();
    Assertions.assertTrue(admin.getSiteReplicationInfoSummary().block().enabled());
    admin.getSiteReplicationInfo().block();
    Assertions.assertEquals(2, admin.getSiteReplicationStatusSummary().block().siteCount());
    admin.getSiteReplicationStatus().block();
    Assertions.assertEquals(1, admin.getSiteReplicationMetainfoSummary().block().bucketCount());
    Assertions.assertEquals("primary", admin.getSiteReplicationMetainfo().block().values().get("Name"));
    Assertions.assertEquals(1, admin.getTopLocksSummary().block().writeLockCount());
    admin.getTopLocksInfo().block();
    Assertions.assertEquals("dep", admin.getObdInfoSummary().block().deploymentId());
    admin.getObdInfo().block();
    Assertions.assertEquals(2, admin.getHealthInfoSummary().block().serverCount());
    admin.getHealthInfo().block();
    Assertions.assertEquals(
        "trace-line",
        new String(admin.traceStream().blockFirst(), java.nio.charset.StandardCharsets.UTF_8).trim());
    Assertions.assertEquals(
        "log-line",
        new String(admin.logStream().blockFirst(), java.nio.charset.StandardCharsets.UTF_8).trim());
    Assertions.assertTrue(admin.getConfigKvEncrypted("api").block().encryptedData().length > 0);
    Assertions.assertTrue(admin.listConfigHistoryKvEncrypted(1).block().encryptedData().length > 0);
    Assertions.assertTrue(admin.getConfigEncrypted().block().encryptedData().length > 0);
    Assertions.assertThrows(UnsupportedOperationException.class, () -> admin.getAccessKeyInfoTyped("svc1").block());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> admin.listAccessKeysTyped("all").block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/help-config-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/builtin/policy-entities"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp-config/openid"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp-config/openid/primary"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/storageinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/datausageinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/accountinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/info-access-key"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-access-keys-bulk"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/temporary-account-info"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-users"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/get-bucket-quota"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-remote-targets"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-jobs"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/status-job"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/describe-job"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/background-heal/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/pools/list"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/pools/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/rebalance/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier-stats"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/info"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/metainfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/top/locks"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/obdinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/healthinfo"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/trace"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/log"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/get-config-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/list-config-history-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/config"));
    Assertions.assertTrue(containsAllQueryParts(queries, "subSys=api", "key=requests_max"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accessKey=svc1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accessKey=tmp1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "listType=all"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1", "type=replication"));
    Assertions.assertTrue(containsAllQueryParts(queries, "pool=pool-0"));
    Assertions.assertTrue(containsAllQueryParts(queries, "jobId=job-1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "key=api"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.listConfigHistoryKvEncrypted(-1));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.listIdpConfigsEncrypted(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.getIdpConfigEncrypted("openid", null));
  }


  @Test
  void shouldExposeStage22AdminJsonSummaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getBackgroundHealStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getBackgroundHealStatusSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listPoolsInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listPoolsSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getPoolStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getPoolStatusSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getRebalanceStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getRebalanceStatusSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTierStats");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTierStatsSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationInfoSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationStatusSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationMetainfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationMetainfoSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTopLocksInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTopLocksSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getObdInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getObdInfoSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getHealthInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getHealthInfoSummary");
    assertFluxMethodExists(ReactiveMinioAdminClient.class, "traceStream");
    assertFluxMethodExists(ReactiveMinioAdminClient.class, "logStream");
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.getPoolStatus(" "));
  }

  @Test
  void shouldExposeStage30AdminSummaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listPolicyEntities");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listIdpConfigs");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listIdpConfigsEncrypted");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getIdpConfigInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getIdpConfigEncrypted");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listRemoteTargetsInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listBatchJobsInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getBatchJobStatusInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getBatchJobStatusSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "describeBatchJobInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "describeBatchJobSummary");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listBuiltinPolicyEntities");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listIdpConfig");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "getIdpConfig");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listRemoteTargets");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "listBatchJobs");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "batchJobStatus");
    assertDeprecatedMethodExists(ReactiveMinioAdminClient.class, "describeBatchJob");
  }

  @Test
  void shouldExposeStage35AdminIamBoundaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listBucketUsersInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getTemporaryAccountInfo");
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.listBucketUsersInfo(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.getTemporaryAccountInfo(" "));
  }

  @Test
  void shouldExposeStage40AdminDiagnosticMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "scrapeAdminMetrics");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "downloadInspectData");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startProfiling");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "downloadProfilingData");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getProfileResult");
  }

  @Test
  void shouldExposeStage47AdminSensitiveExportMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "exportIamData");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "exportBucketMetadataData");
  }

  @Test
  void shouldExposeStage50SensitiveImportArchiveMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "importIamArchive");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "importIamV2Archive");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "importBucketMetadataArchive");
    assertAllPublicOverloadsDeprecated(ReactiveMinioAdminClient.class, "importIam");
    assertAllPublicOverloadsDeprecated(ReactiveMinioAdminClient.class, "importIamV2");
    assertAllPublicOverloadsDeprecated(ReactiveMinioAdminClient.class, "importBucketMetadata");
  }

  @Test
  void shouldExposeStage49AdminKmsBridgeMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getAdminKmsStatus");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "createAdminKmsKey");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getAdminKmsKeyStatus");
    assertAllPublicOverloadsDeprecated(ReactiveMinioAdminClient.class, "kmsStatus");
    assertAllPublicOverloadsDeprecated(ReactiveMinioAdminClient.class, "kmsKeyCreate");
    assertAllPublicOverloadsDeprecated(ReactiveMinioAdminClient.class, "kmsKeyStatus");
  }

  @Test
  void shouldExposeStage48AdminDiagnosticProbeMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runClientDevnull");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runClientDevnullExtraTime");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runSiteReplicationDevnull");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runSiteReplicationNetperf");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runSpeedtest");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runObjectSpeedtest");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runDriveSpeedtest");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runNetworkSpeedtest");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runSiteSpeedtest");
  }

  @Test
  void shouldExposeStage53AdminMaintenanceOperationMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startRootHeal");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startBucketHeal");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startPrefixHeal");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startPoolDecommission");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "cancelPoolDecommission");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startRebalance");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "stopRebalance");
  }

  @Test
  void shouldExposeStage54AdminPolicyAndReplicationBoundaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getReplicationMrfInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getReplicationMrfSummary");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "verifyTierInfo");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "attachBuiltinPolicy");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "detachBuiltinPolicy");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "attachLdapPolicy");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "detachLdapPolicy");
  }

  @Test
  void shouldExposeStage55AdminConfigRiskBoundaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "deleteConfigKvEntry");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "clearConfigHistoryEntry");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "restoreConfigHistoryEntry");
  }

  @Test
  void shouldExposeStage56SiteReplicationPeerLabBoundaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "joinSiteReplicationPeer");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "applySiteReplicationPeerBucketOperation");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "applySiteReplicationPeerIamItem");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "applySiteReplicationPeerBucketMetadata");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runSiteReplicationResyncOperation");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "editSiteReplicationState");
  }

  @Test
  void shouldExposeStage57ServiceUpdateTokenRiskBoundaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "executeServiceControl");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "executeServiceControlV2");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startServerUpdate");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startServerUpdateV2");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "revokeUserProviderTokens");
  }

  @Test
  void shouldExposeStage59RemainingAdminLabRiskBoundaryMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addIdpConfigEntry");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "updateIdpConfigEntry");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "deleteIdpConfigEntry");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addLdapServiceAccountEntry");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setBucketQuotaConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "setRemoteTargetConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "removeRemoteTargetConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "runReplicationDiff");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "startBatchJobRequest");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "cancelBatchJobRequest");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addTierConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "editTierConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "removeTierConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "addSiteReplicationConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "removeSiteReplicationConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "editSiteReplicationConfig");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "editSiteReplicationPeer");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "removeSiteReplicationPeer");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "forceUnlockPaths");
  }

  @Test
  void shouldBuildStage47AdminSensitiveExportRequestsAsBytes() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage47AdminSensitiveExportBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    AdminBinaryResult iam = admin.exportIamData().block();
    AdminBinaryResult bucketMetadata = admin.exportBucketMetadataData().block();
    byte[] rawIam =
        raw.executeToBytes(
                MinioApiCatalog.byName("ADMIN_EXPORT_IAM"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                null,
                null)
            .block();

    Assertions.assertEquals("export-iam", iam.source());
    Assertions.assertEquals("export-bucket-metadata", bucketMetadata.source());
    Assertions.assertArrayEquals(
        "iam-export-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8), iam.bytes());
    Assertions.assertEquals(
        "bucket-metadata-bytes".length(), bucketMetadata.size());
    Assertions.assertEquals(
        "iam-export-bytes", new String(rawIam, java.nio.charset.StandardCharsets.UTF_8));
    byte[] copy = iam.bytes();
    copy[0] = 'X';
    Assertions.assertEquals('i', iam.bytes()[0]);

    Assertions.assertTrue(paths.contains("/minio/admin/v3/export-iam"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/export-bucket-metadata"));
  }

  @Test
  void shouldBuildStage50SensitiveImportArchiveRequestsWithoutLoggingBody() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage50SensitiveImportBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    byte[] archive = "fake-archive".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    AdminTextResult iam = admin.importIamArchive(archive).block();
    AdminTextResult iamV2 = admin.importIamV2Archive(archive, "application/zip").block();
    AdminTextResult metadata = admin.importBucketMetadataArchive(archive).block();

    Assertions.assertEquals("import-iam", iam.source());
    Assertions.assertEquals("import-iam-ok", iam.rawText());
    Assertions.assertEquals("import-iam-v2", iamV2.source());
    Assertions.assertEquals("import-bucket-metadata", metadata.source());
    Assertions.assertEquals(
        "import-iam-ok",
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_IMPORT_IAM"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                archive,
                "application/octet-stream")
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/import-iam"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/import-iam-v2"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/import-bucket-metadata"));
    Assertions.assertTrue(contentTypes.contains("application/octet-stream"));
    Assertions.assertTrue(contentTypes.contains("application/zip"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.importIamArchive(new byte[0]));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.importBucketMetadataArchive(null));
  }

  @Test
  void shouldBuildStage49AdminKmsBridgeRequestsAndKeepDedicatedKmsPreferred() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage49AdminKmsBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioKmsClient kms =
        ReactiveMinioKmsClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    KmsJsonResult adminStatus = admin.getAdminKmsStatus().block();
    admin.createAdminKmsKey("admin-key").block();
    KmsKeyStatus adminKeyStatus = admin.getAdminKmsKeyStatus("admin-key").block();
    KmsJsonResult dedicatedStatus = kms.getStatus().block();
    KmsKeyStatus dedicatedKeyStatus = kms.getKeyStatus("dedicated-key").block();

    Assertions.assertEquals("admin", adminStatus.values().get("source"));
    Assertions.assertEquals("admin-key", adminKeyStatus.keyId());
    Assertions.assertTrue(adminKeyStatus.isOk());
    Assertions.assertEquals("dedicated", dedicatedStatus.values().get("source"));
    Assertions.assertEquals("dedicated-key", dedicatedKeyStatus.keyId());
    Assertions.assertTrue(
        raw.executeToString(MinioApiCatalog.byName("ADMIN_KMS_STATUS"))
            .block()
            .contains("admin"));

    Assertions.assertTrue(paths.contains("/minio/admin/v3/kms/status"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/kms/key/create"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/kms/key/status"));
    Assertions.assertTrue(paths.contains("/minio/kms/v1/status"));
    Assertions.assertTrue(paths.contains("/minio/kms/v1/key/status"));
    Assertions.assertTrue(containsAllQueryParts(queries, "key-id=admin-key"));
    Assertions.assertTrue(containsAllQueryParts(queries, "key-id=dedicated-key"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.createAdminKmsKey(" "));
  }

  @Test
  void shouldBuildStage48AdminDiagnosticProbeRequestsAsText() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage48AdminDiagnosticProbeBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    AdminTextResult clientDevnull = admin.runClientDevnull().block();
    Assertions.assertEquals("client-devnull", clientDevnull.source());
    Assertions.assertEquals("client-devnull-ok", clientDevnull.rawText());
    Assertions.assertEquals(
        "client-devnull-extra-time-ok", admin.runClientDevnullExtraTime().block().rawText());
    Assertions.assertEquals(
        "site-replication-devnull-ok", admin.runSiteReplicationDevnull().block().rawText());
    Assertions.assertEquals(
        "site-replication-netperf-ok", admin.runSiteReplicationNetperf().block().rawText());
    Assertions.assertEquals("speedtest-ok", admin.runSpeedtest().block().rawText());
    Assertions.assertEquals("speedtest-object-ok", admin.runObjectSpeedtest().block().rawText());
    Assertions.assertEquals("speedtest-drive-ok", admin.runDriveSpeedtest().block().rawText());
    Assertions.assertEquals("speedtest-net-ok", admin.runNetworkSpeedtest().block().rawText());
    Assertions.assertEquals("speedtest-site-ok", admin.runSiteSpeedtest().block().rawText());
    Assertions.assertEquals(
        "speedtest-ok",
        raw.executeToString(MinioApiCatalog.byName("ADMIN_SPEEDTEST"))
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest/client/devnull"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest/client/devnull/extratime"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/devnull"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/netperf"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest/object"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest/drive"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest/net"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/speedtest/site"));
  }

  @Test
  void shouldBuildStage59RemainingAdminLabRiskBoundaryRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> methods = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  methods.add(request.method().name());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage59RemainingAdminLabRiskBody(request.url().getPath(), request.method().name()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    byte[] jsonBody = "{\"enabled\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] yamlBody = "job: test".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    AdminTextResult idpAdd = admin.addIdpConfigEntry("openid", "primary", jsonBody).block();
    Assertions.assertEquals("idp-config-add", idpAdd.source());
    Assertions.assertEquals("idp-config-add-ok", idpAdd.rawText());
    Assertions.assertEquals(
        "idp-config-update-ok",
        admin.updateIdpConfigEntry("openid", "primary", jsonBody).block().rawText());
    Assertions.assertEquals(
        "idp-config-delete-ok", admin.deleteIdpConfigEntry("openid", "primary").block().rawText());
    Assertions.assertEquals(
        "ldap-service-account-add-ok", admin.addLdapServiceAccountEntry(jsonBody).block().rawText());
    Assertions.assertEquals(
        "bucket-quota-set-ok", admin.setBucketQuotaConfig("bucket1", jsonBody).block().rawText());
    Assertions.assertEquals(
        "remote-target-set-ok", admin.setRemoteTargetConfig("bucket1", jsonBody).block().rawText());
    Assertions.assertEquals(
        "remote-target-remove-ok",
        admin.removeRemoteTargetConfig("bucket1", "arn1").block().rawText());
    Assertions.assertEquals(
        "replication-diff-ok", admin.runReplicationDiff("bucket1", jsonBody).block().rawText());
    Assertions.assertEquals("batch-job-start-ok", admin.startBatchJobRequest(yamlBody).block().rawText());
    Assertions.assertEquals("batch-job-cancel-ok", admin.cancelBatchJobRequest(yamlBody).block().rawText());
    Assertions.assertEquals("tier-add-ok", admin.addTierConfig(jsonBody).block().rawText());
    Assertions.assertEquals("tier-edit-ok", admin.editTierConfig("warm-tier", jsonBody).block().rawText());
    Assertions.assertEquals("tier-remove-ok", admin.removeTierConfig("warm-tier").block().rawText());
    Assertions.assertEquals(
        "site-replication-add-ok", admin.addSiteReplicationConfig(jsonBody).block().rawText());
    Assertions.assertEquals(
        "site-replication-remove-ok", admin.removeSiteReplicationConfig(jsonBody).block().rawText());
    Assertions.assertEquals(
        "site-replication-edit-ok", admin.editSiteReplicationConfig(jsonBody).block().rawText());
    Assertions.assertEquals("sr-peer-edit-ok", admin.editSiteReplicationPeer(jsonBody).block().rawText());
    Assertions.assertEquals("sr-peer-remove-ok", admin.removeSiteReplicationPeer(jsonBody).block().rawText());
    Assertions.assertEquals("force-unlock-ok", admin.forceUnlockPaths("bucket1/object1").block().rawText());
    Assertions.assertEquals(
        "tier-add-ok",
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_ADD_TIER"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                jsonBody,
                "application/json")
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp-config/openid/primary"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/ldap/add-service-account"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/set-bucket-quota"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/set-remote-target"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/remove-remote-target"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/replication/diff"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/start-job"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/cancel-job"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier/warm-tier"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/add"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/remove"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/edit"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/edit"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/remove"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/force-unlock"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1", "arn=arn1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "paths=bucket1/object1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "api-version=1"));
    Assertions.assertTrue(methods.contains("PUT"));
    Assertions.assertTrue(methods.contains("POST"));
    Assertions.assertTrue(methods.contains("DELETE"));
    Assertions.assertTrue(contentTypes.contains("application/json"));
    Assertions.assertTrue(contentTypes.contains("application/yaml"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.addIdpConfigEntry("openid", "primary", new byte[0]));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.setBucketQuotaConfig(" ", jsonBody));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.editTierConfig("warm-tier", null));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.forceUnlockPaths(""));
  }

  @Test
  void shouldBuildStage57ServiceUpdateTokenRiskBoundaryRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage57ServiceUpdateTokenBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals("service-control-ok", admin.executeServiceControl("restart").block().rawText());
    Assertions.assertEquals("service-control-v2", admin.executeServiceControlV2("restart").block().source());
    Assertions.assertEquals("server-update-ok", admin.startServerUpdate("http://update.local/minio").block().rawText());
    Assertions.assertEquals("server-update-v2", admin.startServerUpdateV2("http://update.local/minio").block().source());
    Assertions.assertEquals("revoke-tokens-ok", admin.revokeUserProviderTokens("ldap").block().rawText());
    Assertions.assertEquals(
        "service-control-ok",
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_SERVICE"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.singletonMap("action", "restart"),
                java.util.Collections.<String, String>emptyMap(),
                null,
                null)
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/service"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/update"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/revoke-tokens/ldap"));
    Assertions.assertTrue(containsAllQueryParts(queries, "action=restart"));
    Assertions.assertTrue(containsAllQueryParts(queries, "type=2", "action=restart"));
    Assertions.assertTrue(containsAllQueryParts(queries, "updateURL=http://update.local/minio"));
    Assertions.assertTrue(containsAllQueryParts(queries, "type=2", "updateURL=http://update.local/minio"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.executeServiceControl(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.startServerUpdate(null));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.revokeUserProviderTokens(""));
  }

  @Test
  void shouldBuildStage56SiteReplicationPeerLabBoundaryRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage56SiteReplicationPeerBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    byte[] body = "{\"op\":\"lab\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Assertions.assertEquals("site-replication-peer-join", admin.joinSiteReplicationPeer(body).block().source());
    Assertions.assertEquals(
        "peer-bucket-ops-ok",
        admin.applySiteReplicationPeerBucketOperation("bucket1", "enable", body).block().rawText());
    Assertions.assertEquals(
        "peer-iam-item-ok", admin.applySiteReplicationPeerIamItem(body).block().rawText());
    Assertions.assertEquals(
        "peer-bucket-meta-ok",
        admin.applySiteReplicationPeerBucketMetadata(body).block().rawText());
    Assertions.assertEquals(
        "resync-op-ok", admin.runSiteReplicationResyncOperation("start", body).block().rawText());
    Assertions.assertEquals("state-edit-ok", admin.editSiteReplicationState(body).block().rawText());
    Assertions.assertEquals(
        "peer-iam-item-ok",
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_SR_PEER_IAM_ITEM"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.singletonMap("api-version", "1"),
                java.util.Collections.<String, String>emptyMap(),
                body,
                "application/json")
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/join"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/bucket-ops"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/iam-item"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/bucket-meta"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/resync/op"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/state/edit"));
    Assertions.assertTrue(containsAllQueryParts(queries, "api-version=1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1", "operation=enable"));
    Assertions.assertTrue(containsAllQueryParts(queries, "operation=start", "api-version=1"));
    Assertions.assertTrue(contentTypes.contains("application/json"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.joinSiteReplicationPeer(new byte[0]));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> admin.applySiteReplicationPeerBucketOperation("bucket1", " ", body));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.editSiteReplicationState(null));
  }

  @Test
  void shouldBuildStage55AdminConfigRiskBoundaryRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage55AdminConfigRiskBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    byte[] deleteBody = "api requests_max".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Assertions.assertEquals("config-kv-delete", admin.deleteConfigKvEntry(deleteBody).block().source());
    Assertions.assertEquals(
        "config-history-clear-ok", admin.clearConfigHistoryEntry("restore-1").block().rawText());
    Assertions.assertEquals(
        "config-history-restore-ok", admin.restoreConfigHistoryEntry("restore-1").block().rawText());
    Assertions.assertEquals(
        "config-history-restore-ok",
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_RESTORE_CONFIG_HISTORY_KV"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.singletonMap("restoreId", "restore-1"),
                java.util.Collections.<String, String>emptyMap(),
                null,
                null)
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/del-config-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/clear-config-history-kv"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/restore-config-history-kv"));
    Assertions.assertTrue(containsAllQueryParts(queries, "restoreId=restore-1"));
    Assertions.assertTrue(contentTypes.contains("text/plain"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.deleteConfigKvEntry(new byte[0]));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.clearConfigHistoryEntry(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.restoreConfigHistoryEntry(null));
  }

  @Test
  void shouldBuildStage54AdminPolicyAndReplicationBoundaryRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> contentTypes = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  contentTypes.add(String.valueOf(request.headers().getContentType()));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage54AdminPolicyAndReplicationBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    byte[] body = "{\"policy\":\"readonly\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Assertions.assertEquals("ok", admin.getReplicationMrfInfo("bucket1").block().values().get("status"));
    Assertions.assertEquals(1, admin.getReplicationMrfSummary("bucket1").block().entryCount());
    Assertions.assertEquals("tier-verify-ok", admin.verifyTierInfo("warm-tier").block().rawText());
    Assertions.assertEquals("builtin-policy-attach", admin.attachBuiltinPolicy(body).block().source());
    Assertions.assertEquals("builtin-policy-detach-ok", admin.detachBuiltinPolicy(body).block().rawText());
    Assertions.assertEquals("ldap-policy-attach", admin.attachLdapPolicy(body).block().source());
    Assertions.assertEquals("ldap-policy-detach-ok", admin.detachLdapPolicy(body).block().rawText());
    Assertions.assertEquals(
        "tier-verify-ok",
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_VERIFY_TIER"),
                java.util.Collections.singletonMap("tier", "warm-tier"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.<String, String>emptyMap(),
                null,
                null)
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/replication/mrf"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/tier/warm-tier"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/builtin/policy/attach"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/builtin/policy/detach"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/ldap/policy/attach"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/ldap/policy/detach"));
    Assertions.assertTrue(containsAllQueryParts(queries, "bucket=bucket1"));
    Assertions.assertTrue(contentTypes.contains("application/json"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.getReplicationMrfInfo(""));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.getReplicationMrfSummary(""));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.verifyTierInfo(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.attachBuiltinPolicy(new byte[0]));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.detachLdapPolicy(null));
  }

  @Test
  void shouldBuildStage53AdminMaintenanceOperationRequestsAsText() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage53AdminMaintenanceBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    AdminTextResult rootHeal = admin.startRootHeal().block();
    Assertions.assertEquals("heal-root", rootHeal.source());
    Assertions.assertEquals("heal-root-ok", rootHeal.rawText());
    Assertions.assertEquals("heal-bucket-ok", admin.startBucketHeal("bucket1").block().rawText());
    Assertions.assertEquals(
        "heal-prefix-ok", admin.startPrefixHeal("bucket1", "prefix1").block().rawText());
    Assertions.assertEquals(
        "pool-decommission-start", admin.startPoolDecommission("pool-0").block().source());
    Assertions.assertEquals(
        "pool-decommission-cancel-ok",
        admin.cancelPoolDecommission("pool-0").block().rawText());
    Assertions.assertEquals("rebalance-start-ok", admin.startRebalance().block().rawText());
    Assertions.assertEquals("rebalance-stop-ok", admin.stopRebalance().block().rawText());
    Assertions.assertEquals(
        "rebalance-start-ok",
        raw.executeToString(MinioApiCatalog.byName("ADMIN_REBALANCE_START"))
            .block());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/heal/"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/heal/bucket1"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/heal/bucket1/prefix1"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/pools/decommission"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/pools/cancel"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/rebalance/start"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/rebalance/stop"));
    Assertions.assertTrue(containsAllQueryParts(queries, "pool=pool-0"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.startBucketHeal(" "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.startPrefixHeal("bucket1", " "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> admin.startPoolDecommission(""));
  }

  @Test
  void shouldBuildStage40AdminDiagnosticRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage40AdminDiagnosticBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals("admin", admin.scrapeAdminMetrics().block().scope());
    Assertions.assertEquals(19, admin.downloadInspectData().block().size());
    Assertions.assertEquals(
        "inspect-data-post",
        admin.downloadInspectData(
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8), "application/json")
            .block()
            .source());
    Assertions.assertEquals("profiling-start", admin.startProfiling("cpu").block().source());
    Assertions.assertEquals(14, admin.downloadProfilingData().block().size());
    Assertions.assertEquals("profile", admin.getProfileResult().block().source());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/metrics"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/inspect-data"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/profiling/start"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/profiling/download"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/profile"));
    Assertions.assertTrue(containsAllQueryParts(queries, "profilerType=cpu"));
  }

  @Test
  void shouldExposeStage41AdminIamIdpReadonlyMethods() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listLdapPolicyEntities");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listLdapAccessKeySummaries");
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "listOpenidAccessKeySummaries");
  }

  @Test
  void shouldBuildStage41AdminIamIdpReadonlyRequestsWithoutSecrets() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage41AdminIamIdpBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals(2, admin.listLdapPolicyEntities().block().totalMappingCount());
    Assertions.assertEquals(
        1, admin.listLdapAccessKeySummaries("user1", "svcacc").block().totalCount());
    Assertions.assertEquals(1, admin.listLdapAccessKeySummaries("sts").block().totalCount());
    Assertions.assertEquals(1, admin.listOpenidAccessKeySummaries("svcacc").block().totalCount());
    Assertions.assertEquals(
        "ldap-ak",
        admin.listLdapAccessKeySummaries("user1", "svcacc")
            .block()
            .serviceAccounts()
            .get(0)
            .accessKey());

    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/ldap/policy-entities"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/ldap/list-access-keys"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/ldap/list-access-keys-bulk"));
    Assertions.assertTrue(paths.contains("/minio/admin/v3/idp/openid/list-access-keys-bulk"));
    Assertions.assertTrue(containsAllQueryParts(queries, "userDN=user1", "listType=svcacc"));
    Assertions.assertTrue(containsAllQueryParts(queries, "listType=sts"));
  }

  @Test
  void shouldExposeStage42SiteReplicationPeerIdpSummaryMethod() {
    assertMonoMethodExists(ReactiveMinioAdminClient.class, "getSiteReplicationPeerIdpSettings");
    assertNoPublicMethod(AdminSiteReplicationPeerIdpSettings.class, "rawJson");
  }

  @Test
  void shouldBuildStage42SiteReplicationPeerIdpRequestWithoutRawSecrets() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage42SiteReplicationPeerBody(request.url().getPath()))
                          .build());
                })
            .build();
    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();
    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    AdminSiteReplicationPeerIdpSettings settings =
        admin.getSiteReplicationPeerIdpSettings().block();
    Assertions.assertTrue(settings.ldapEnabled());
    Assertions.assertTrue(settings.openidEnabled());
    Assertions.assertEquals("us-east-1", settings.openidRegion());
    Assertions.assertEquals(1, settings.openidRoleCount());
    Assertions.assertEquals("arn:minio:iam:::role/readonly", settings.openidRoleNames().get(0));
    Assertions.assertTrue(settings.claimProviderConfigured());
    Assertions.assertTrue(settings.identityProviderConfigured());

    AdminSiteReplicationPeerIdpSettings legacy =
        AdminSiteReplicationPeerIdpSettings.parse(
            "{\"IsLDAPEnabled\":false,\"LDAPUserDNSearchBase\":\"\",\"LDAPUserDNSearchFilter\":\"\"}");
    Assertions.assertFalse(legacy.identityProviderConfigured());
    Assertions.assertTrue(
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_SR_PEER_IDP_SETTINGS"),
                java.util.Collections.<String, String>emptyMap(),
                java.util.Collections.singletonMap("api-version", "1"),
                java.util.Collections.<String, String>emptyMap(),
                null,
                null)
            .block()
            .contains("LDAP"));

    Assertions.assertTrue(paths.contains("/minio/admin/v3/site-replication/peer/idp-settings"));
    Assertions.assertTrue(containsAllQueryParts(queries, "api-version=1"));
  }


  @Test
  void shouldExposeS3VersioningBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketVersioningConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketVersioningConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "listObjectVersionsPage");
    assertMonoMethodExists(ReactiveMinioClient.class, "listMultipartUploadsPage");
    assertFluxMethodExists(ReactiveMinioClient.class, "listObjectVersions");
    assertFluxMethodExists(ReactiveMinioClient.class, "listMultipartUploads");
    assertMonoMethodExists(ReactiveMinioStsClient.class, "assumeRoleCredentials");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketVersioning");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3PutBucketVersioning");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3DeleteObjectTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketTagging");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketVersioning");
  }


  @Test
  void shouldExposeS3ObjectGovernanceBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectAttributes");
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectRetention");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectRetention");
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectLegalHold");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectLegalHold");
    assertMonoMethodExists(ReactiveMinioClient.class, "enableObjectLegalHold");
    assertMonoMethodExists(ReactiveMinioClient.class, "disableObjectLegalHold");
    assertMonoMethodExists(ReactiveMinioClient.class, "isObjectLegalHoldEnabled");
    assertMonoMethodExists(ReactiveMinioClient.class, "restoreObject");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectAttributes");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectRetention");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectLegalHold");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectRetention");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectLegalHold");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PostRestoreObject");
  }


  @Test
  void shouldBuildS3ObjectGovernanceRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> objectAttributeHeaders = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  objectAttributeHeaders.add(request.headers().getFirst("X-Amz-Object-Attributes"));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage20S3ResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals(42L, client.getObjectAttributes("bucket1", "folder/a.txt").block().objectSize());
    Assertions.assertEquals(
        ObjectRetentionConfiguration.GOVERNANCE,
        client.getObjectRetention("bucket1", "folder/a.txt", "v1").block().mode());
    client
        .setObjectRetention(
            "bucket1",
            "folder/a.txt",
            "v1",
            ObjectRetentionConfiguration.governance("2030-01-01T00:00:00Z"))
        .block();
    Assertions.assertTrue(client.getObjectLegalHold("bucket1", "folder/a.txt", "v2").block().enabledValue());
    client.setObjectLegalHold("bucket1", "folder/a.txt", "v2", ObjectLegalHoldConfiguration.enabled()).block();
    client.restoreObject("bucket1", "folder/a.txt", "v3", RestoreObjectRequest.of(5, "Bulk")).block();

    Assertions.assertTrue(paths.contains("/bucket1/folder/a.txt"));
    Assertions.assertTrue(containsAllQueryParts(queries, "attributes"));
    Assertions.assertTrue(containsAllQueryParts(queries, "retention", "versionId=v1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "legal-hold", "versionId=v2"));
    Assertions.assertTrue(containsAllQueryParts(queries, "restore", "versionId=v3"));
    Assertions.assertTrue(objectAttributeHeaders.contains("ETag,ObjectSize,StorageClass,Checksum,ObjectParts"));
  }


  @Test
  void shouldExposeS3BucketSubresourceBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketCorsConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketCorsConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteBucketCorsConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketCors");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketCors");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteBucketCors");
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectLockConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectLockConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteObjectLockConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketWebsiteConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteBucketWebsiteConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketLoggingConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketPolicyStatus");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketAccelerateConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketRequestPaymentConfiguration");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketCors");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3DeleteBucketCors");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3DeleteBucketWebsite");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketCors");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketWebsite");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketLogging");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketPolicyStatus");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketAccelerate");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketRequestPayment");
  }

  @Test
  void shouldExposeS3AclAndSelectBusinessMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getObjectAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "setObjectCannedAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketCannedAcl");
    assertMonoMethodExists(ReactiveMinioClient.class, "selectObjectContent");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetObjectAcl");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutObjectAcl");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketAcl");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketAcl");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3SelectObjectContent");
  }

  @Test
  void shouldExposeS3NotificationAndReplicationMetricsMethods() {
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketNotificationConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "setBucketNotificationConfiguration");
    assertMonoMethodExists(ReactiveMinioClient.class, "deleteBucketNotification");
    assertFluxMethodExists(ReactiveMinioClient.class, "listenBucketNotification");
    assertFluxMethodExists(ReactiveMinioClient.class, "listenRootNotification");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketReplicationMetrics");
    assertMonoMethodExists(ReactiveMinioClient.class, "getBucketReplicationMetricsV2");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketNotification");
    assertAllPublicOverloadsDeprecated(ReactiveMinioClient.class, "s3PutBucketNotification");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketReplicationMetrics");
    assertDeprecatedMethodExists(ReactiveMinioClient.class, "s3GetBucketReplicationMetricsV2");
  }


  @Test
  void shouldBuildS3BucketSubresourceRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage21BucketResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    BucketCorsConfiguration cors =
        BucketCorsConfiguration.of(
            java.util.Arrays.asList(
                new BucketCorsRule(
                    java.util.Arrays.asList("GET"),
                    java.util.Arrays.asList("*"),
                    java.util.Arrays.asList("Authorization"),
                    java.util.Arrays.asList("ETag"),
                    60)));
    client.setBucketCorsConfiguration("bucket1", cors).block();
    Assertions.assertEquals("GET", client.getBucketCorsConfiguration("bucket1").block().rules().get(0).allowedMethods().get(0));
    client.deleteBucketCorsConfiguration("bucket1").block();
    Assertions.assertEquals("index.html", client.getBucketWebsiteConfiguration("bucket1").block().indexDocumentSuffix());
    client.deleteBucketWebsiteConfiguration("bucket1").block();
    Assertions.assertEquals("logs", client.getBucketLoggingConfiguration("bucket1").block().targetBucket());
    Assertions.assertTrue(client.getBucketPolicyStatus("bucket1").block().publicBucket());
    Assertions.assertTrue(client.getBucketAccelerateConfiguration("bucket1").block().enabled());
    Assertions.assertTrue(client.getBucketRequestPaymentConfiguration("bucket1").block().requesterPays());

    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "cors"));
    Assertions.assertTrue(containsAllQueryParts(queries, "website"));
    Assertions.assertTrue(containsAllQueryParts(queries, "logging"));
    Assertions.assertTrue(containsAllQueryParts(queries, "policyStatus"));
    Assertions.assertTrue(containsAllQueryParts(queries, "accelerate"));
    Assertions.assertTrue(containsAllQueryParts(queries, "requestPayment"));
  }

  @Test
  void shouldBuildS3AclAndSelectRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    java.util.List<String> aclHeaders = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  aclHeaders.add(request.headers().getFirst("x-amz-acl"));
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage27S3ResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    Assertions.assertEquals("owner-id", client.getObjectAcl("bucket1", "a.csv").block().owner().id());
    client.setObjectCannedAcl("bucket1", "a.csv", CannedAcl.PRIVATE).block();
    Assertions.assertTrue(client.getBucketAcl("bucket1").block().hasGrant("FULL_CONTROL"));
    client.setBucketCannedAcl("bucket1", CannedAcl.PUBLIC_READ).block();
    Assertions.assertEquals(
        "select-response",
        client
            .selectObjectContent("bucket1", "a.csv", SelectObjectContentRequest.csv("select * from s3object"))
            .block()
            .rawResponse());

    Assertions.assertTrue(paths.contains("/bucket1/a.csv"));
    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(containsAllQueryParts(queries, "acl"));
    Assertions.assertTrue(containsAllQueryParts(queries, "select", "select-type=2"));
    Assertions.assertTrue(aclHeaders.contains("private"));
    Assertions.assertTrue(aclHeaders.contains("public-read"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> client.setBucketCannedAcl("bucket1", null));
  }

  @Test
  void shouldBuildS3NotificationAndReplicationMetricsRequests() {
    java.util.List<String> paths = new java.util.ArrayList<String>();
    java.util.List<String> queries = new java.util.ArrayList<String>();
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request -> {
                  paths.add(request.url().getPath());
                  queries.add(request.url().getQuery());
                  return Mono.just(
                      ClientResponse.create(HttpStatus.OK)
                          .body(stage28S3ResponseBody(request.url().getQuery()))
                          .build());
                })
            .build();
    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    BucketNotificationConfiguration configuration =
        BucketNotificationConfiguration.of(
            java.util.Arrays.asList(
                BucketNotificationTarget.queue(
                    "arn:minio:sqs::1:webhook", java.util.Arrays.asList("s3:ObjectCreated:*"))));
    client.setBucketNotificationConfiguration("bucket1", configuration).block();
    Assertions.assertEquals(
        "arn:minio:sqs::1:webhook",
        client.getBucketNotificationConfiguration("bucket1").block().targets().get(0).arn());
    Assertions.assertEquals(
        "event-stream",
        new String(
            client.listenBucketNotification("bucket1", "s3:ObjectCreated:*").blockFirst(),
            java.nio.charset.StandardCharsets.UTF_8));
    Assertions.assertEquals(
        "event-stream",
        new String(
            client.listenRootNotification("s3:ObjectRemoved:*").blockFirst(),
            java.nio.charset.StandardCharsets.UTF_8));
    BucketReplicationMetrics metrics = client.getBucketReplicationMetrics("bucket1").block();
    BucketReplicationMetrics metricsV2 = client.getBucketReplicationMetricsV2("bucket1").block();

    Assertions.assertEquals("v1", metrics.version());
    Assertions.assertEquals("v2", metricsV2.version());
    Assertions.assertEquals(123L, metricsV2.uptime());
    Assertions.assertTrue(paths.contains("/bucket1"));
    Assertions.assertTrue(paths.contains("/"));
    Assertions.assertTrue(containsAllQueryParts(queries, "notification"));
    Assertions.assertTrue(containsAllQueryParts(queries, "events=s3:ObjectCreated:*"));
    Assertions.assertTrue(containsAllQueryParts(queries, "events=s3:ObjectRemoved:*"));
    Assertions.assertTrue(containsAllQueryParts(queries, "replication-metrics"));
    Assertions.assertTrue(containsAllQueryParts(queries, "replication-metrics=2"));
  }


  private static void assertAllPublicOverloadsDeprecated(Class<?> type, String name) {
    int matched = 0;
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass().equals(type) && method.getName().equals(name)) {
        matched++;
        Assertions.assertNotNull(
            method.getAnnotation(Deprecated.class),
            "缺少 @Deprecated 重载: " + type.getSimpleName() + "." + name + java.util.Arrays.toString(method.getParameterTypes()));
      }
    }
    Assertions.assertTrue(matched > 0, "缺少方法: " + type.getSimpleName() + "." + name);
  }

  private static void assertAdvancedBaseline(
      Class<?> type, int monoStringCount, int deprecatedCount, int rawishCount) {
    Assertions.assertEquals(monoStringCount, countPublicMonoStringMethods(type), type.getSimpleName());
    Assertions.assertEquals(deprecatedCount, countDeprecatedMethods(type), type.getSimpleName());
    Assertions.assertEquals(rawishCount, countRawishExecuteMethods(type), type.getSimpleName());
  }

  private static int countPublicMonoStringMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && method.getReturnType().equals(Mono.class)
          && method.getGenericReturnType().toString().contains("java.lang.String")) {
        count++;
      }
    }
    return count;
  }

  private static int countDeprecatedMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getAnnotation(Deprecated.class) != null) {
        count++;
      }
    }
    return count;
  }

  private static int countRawishExecuteMethods(Class<?> type) {
    int count = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("executeTo")) {
        count++;
      }
    }
    return count;
  }

  private static void assertDeprecatedMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getAnnotation(Deprecated.class) != null) {
        return;
      }
    }
    Assertions.fail("缺少 @Deprecated 方法: " + type.getSimpleName() + "." + name);
  }

  private static void assertNoPublicMethod(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && Modifier.isPublic(method.getModifiers())) {
        Assertions.fail("不应暴露 public 方法: " + type.getSimpleName() + "." + name);
      }
    }
  }

  private static boolean containsAllQueryParts(java.util.List<String> queries, String... parts) {
    for (String query : queries) {
      boolean matched = true;
      for (String part : parts) {
        matched = matched && query != null && query.contains(part);
      }
      if (matched) {
        return true;
      }
    }
    return false;
  }

  private static String stage15AdminResponseBody(String path) {
    if (path.endsWith("/help-config-kv")) {
      return "{\"subSys\":\"api\",\"description\":\"API 配置\",\"keysHelp\":[]}";
    }
    if (path.endsWith("/idp/builtin/policy-entities")) {
      return "{\"UserMappings\":{\"user1\":[\"readonly\"]},\"GroupMappings\":{\"dev\":[\"readwrite\"]},\"PolicyMappings\":{\"readonly\":[\"user1\"]}}";
    }
    if (path.endsWith("/idp-config/openid")) {
      return "{\"primary\":{}}";
    }
    if (path.endsWith("/idp-config/openid/primary")) {
      return "{\"status\":\"ok\"}";
    }
    if (path.endsWith("/storageinfo")) {
      return "{\"Disks\":[],\"Backend\":{\"Type\":1,\"OnlineDisks\":{},\"OfflineDisks\":{}}}";
    }
    if (path.endsWith("/datausageinfo")) {
      return "{\"objectsCount\":0,\"objectsTotalSize\":0,\"bucketsCount\":0}";
    }
    if (path.endsWith("/accountinfo")) {
      return "{\"AccountName\":\"root\",\"Server\":{\"Type\":1},\"Buckets\":[]}";
    }
    if (path.endsWith("/info-access-key")) {
      return "{\"accessKey\":\"svc1\",\"accountStatus\":\"enabled\"}";
    }
    if (path.endsWith("/list-access-keys-bulk")) {
      return "{\"serviceAccounts\":[],\"stsKeys\":[]}";
    }
    if (path.endsWith("/temporary-account-info")) {
      return "{\"accessKey\":\"tmp1\",\"accountStatus\":\"enabled\",\"parentUser\":\"root\"}";
    }
    if (path.endsWith("/list-users")) {
      return "{\"users\":{\"user1\":{\"status\":\"enabled\",\"policyName\":\"readonly\",\"memberOf\":[]}}}";
    }
    if (path.endsWith("/get-bucket-quota")) {
      return "{\"quota\":0,\"size\":0,\"rate\":0,\"requests\":0,\"quotatype\":\"hard\"}";
    }
    if (path.endsWith("/tier")) {
      return "[]";
    }
    if (path.endsWith("/list-remote-targets")) {
      return "{\"Targets\":[{\"Arn\":\"arn:minio:replication::bucket:target\",\"Type\":\"replication\",\"Endpoint\":\"http://remote\",\"Secure\":true}]}";
    }
    if (path.endsWith("/list-jobs")) {
      return "{\"Jobs\":[{\"ID\":\"job-1\",\"Type\":\"replicate\",\"Status\":\"running\",\"User\":\"root\"}]}";
    }
    if (path.endsWith("/status-job")) {
      return "{\"status\":\"running\",\"JobID\":\"job-1\",\"JobType\":\"replicate\",\"RetryAttempts\":1}";
    }
    if (path.endsWith("/describe-job")) {
      return "{\"id\":\"job-1\",\"status\":\"running\",\"type\":\"replicate\"}";
    }
    if (path.endsWith("/background-heal/status")) {
      return "{\"ScannedItemsCount\":12,\"offline_nodes\":[\"node1\"],\"HealDisks\":[\"disk1\"],\"sets\":[{}],\"mrf\":{\"node1\":{}},\"sc_parity\":{\"STANDARD\":2}}";
    }
    if (path.endsWith("/rebalance/status")) {
      return "{\"ID\":\"rebalance-1\",\"pools\":[{\"id\":0,\"status\":\"active\"},{\"id\":1,\"status\":\"\"}]}";
    }
    if (path.endsWith("/tier-stats")) {
      return "[{\"Name\":\"ARCHIVE\",\"Type\":\"s3\",\"Stats\":{\"totalSize\":1024,\"numVersions\":3,\"numObjects\":2}}]";
    }
    if (path.endsWith("/top/locks")) {
      return "[{\"type\":\"Write\",\"elapsed\":300,\"serverlist\":[\"node1\"],\"quorum\":2},{\"type\":\"Read\",\"elapsed\":100,\"serverlist\":[\"node1\",\"node2\"],\"quorum\":2}]";
    }
    if (path.endsWith("/obdinfo") || path.endsWith("/healthinfo")) {
      return "{\"version\":\"3\",\"timestamp\":\"2026-04-24T00:00:00Z\",\"minio\":{\"info\":{\"deploymentID\":\"dep\",\"region\":\"us-east-1\",\"buckets\":{\"count\":2},\"objects\":{\"count\":5},\"servers\":[{},{}]}}}";
    }
    if (path.endsWith("/site-replication/info")) {
      return "{\"enabled\":true,\"name\":\"primary\",\"sites\":[{\"name\":\"a\"},{\"name\":\"b\"}],\"serviceAccountAccessKey\":\"svc-key\",\"apiVersion\":\"1\"}";
    }
    if (path.endsWith("/site-replication/status")) {
      return "{\"Enabled\":true,\"MaxBuckets\":2,\"MaxUsers\":3,\"MaxGroups\":4,\"MaxPolicies\":5,\"Sites\":{\"dep1\":{\"name\":\"a\"},\"dep2\":{\"name\":\"b\"}},\"BucketStats\":{\"bucket1\":{\"dep1\":{}}},\"Metrics\":{\"pending\":1},\"apiVersion\":\"1\"}";
    }
    if (path.endsWith("/site-replication/metainfo")) {
      return "{\"Enabled\":true,\"Name\":\"primary\",\"DeploymentID\":\"dep1\",\"Buckets\":{\"bucket1\":{}},\"Policies\":{\"readonly\":{}},\"UserInfoMap\":{\"user1\":{}},\"GroupDescMap\":{\"dev\":{}},\"ReplicationCfg\":{\"bucket1\":{}},\"ILMExpiryRules\":{\"rule1\":{}},\"apiVersion\":\"1\"}";
    }
    if (path.endsWith("/pools/list")) {
      return "[{\"id\":0,\"cmdline\":\"pool-0\",\"decommissionInfo\":{\"totalSize\":100,\"currentSize\":60,\"complete\":false}},{\"id\":1,\"cmdline\":\"pool-1\",\"decommissionInfo\":{\"totalSize\":50,\"currentSize\":0,\"complete\":true}}]";
    }
    if (path.endsWith("/pools/status")) {
      return "{\"id\":0,\"cmdline\":\"pool-0\",\"lastUpdate\":\"2026-04-24T00:00:00Z\",\"decommissionInfo\":{\"totalSize\":100,\"currentSize\":60,\"complete\":false,\"failed\":false,\"canceled\":false,\"objectsDecommissioned\":7,\"objectsDecommissionedFailed\":1,\"bytesDecommissioned\":40,\"bytesDecommissionedFailed\":2}}";
    }
    if (path.endsWith("/trace")) {
      return "trace-line\n";
    }
    if (path.endsWith("/log")) {
      return "log-line\n";
    }
    return "encrypted-placeholder-response-body";
  }

  private static String stage47AdminSensitiveExportBody(String path) {
    if (path.endsWith("/export-iam")) {
      return "iam-export-bytes";
    }
    if (path.endsWith("/export-bucket-metadata")) {
      return "bucket-metadata-bytes";
    }
    return "";
  }

  private static String stage50SensitiveImportBody(String path) {
    if (path.equals("/minio/admin/v3/import-iam")) {
      return "import-iam-ok";
    }
    if (path.equals("/minio/admin/v3/import-iam-v2")) {
      return "import-iam-v2-ok";
    }
    if (path.equals("/minio/admin/v3/import-bucket-metadata")) {
      return "import-bucket-metadata-ok";
    }
    return "";
  }

  private static String stage49AdminKmsBody(String path) {
    if (path.equals("/minio/admin/v3/kms/status")) {
      return "{\"source\":\"admin\"}";
    }
    if (path.equals("/minio/admin/v3/kms/key/status")) {
      return "{\"key-id\":\"admin-key\",\"encryptionErr\":\"\",\"decryptionErr\":\"\"}";
    }
    if (path.equals("/minio/kms/v1/status")) {
      return "{\"source\":\"dedicated\"}";
    }
    if (path.equals("/minio/kms/v1/key/status")) {
      return "{\"key-id\":\"dedicated-key\",\"encryptionErr\":\"\",\"decryptionErr\":\"\"}";
    }
    return "{}";
  }

  private static String stage59RemainingAdminLabRiskBody(String path, String method) {
    if (path.equals("/minio/admin/v3/idp-config/openid/primary")) {
      if ("DELETE".equals(method)) {
        return "idp-config-delete-ok";
      }
      if ("POST".equals(method)) {
        return "idp-config-update-ok";
      }
      return "idp-config-add-ok";
    }
    if (path.equals("/minio/admin/v3/idp/ldap/add-service-account")) {
      return "ldap-service-account-add-ok";
    }
    if (path.equals("/minio/admin/v3/set-bucket-quota")) {
      return "bucket-quota-set-ok";
    }
    if (path.equals("/minio/admin/v3/set-remote-target")) {
      return "remote-target-set-ok";
    }
    if (path.equals("/minio/admin/v3/remove-remote-target")) {
      return "remote-target-remove-ok";
    }
    if (path.equals("/minio/admin/v3/replication/diff")) {
      return "replication-diff-ok";
    }
    if (path.equals("/minio/admin/v3/start-job")) {
      return "batch-job-start-ok";
    }
    if (path.equals("/minio/admin/v3/cancel-job")) {
      return "batch-job-cancel-ok";
    }
    if (path.equals("/minio/admin/v3/tier")) {
      return "tier-add-ok";
    }
    if (path.equals("/minio/admin/v3/tier/warm-tier")) {
      if ("DELETE".equals(method)) {
        return "tier-remove-ok";
      }
      return "tier-edit-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/add")) {
      return "site-replication-add-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/remove")) {
      return "site-replication-remove-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/edit")) {
      return "site-replication-edit-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/peer/edit")) {
      return "sr-peer-edit-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/peer/remove")) {
      return "sr-peer-remove-ok";
    }
    if (path.equals("/minio/admin/v3/force-unlock")) {
      return "force-unlock-ok";
    }
    return "";
  }

  private static String stage57ServiceUpdateTokenBody(String path) {
    if (path.equals("/minio/admin/v3/service")) {
      return "service-control-ok";
    }
    if (path.equals("/minio/admin/v3/update")) {
      return "server-update-ok";
    }
    if (path.equals("/minio/admin/v3/revoke-tokens/ldap")) {
      return "revoke-tokens-ok";
    }
    return "";
  }

  private static String stage56SiteReplicationPeerBody(String path) {
    if (path.equals("/minio/admin/v3/site-replication/peer/join")) {
      return "peer-join-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/peer/bucket-ops")) {
      return "peer-bucket-ops-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/peer/iam-item")) {
      return "peer-iam-item-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/peer/bucket-meta")) {
      return "peer-bucket-meta-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/resync/op")) {
      return "resync-op-ok";
    }
    if (path.equals("/minio/admin/v3/site-replication/state/edit")) {
      return "state-edit-ok";
    }
    return "";
  }

  private static String stage55AdminConfigRiskBody(String path) {
    if (path.equals("/minio/admin/v3/del-config-kv")) {
      return "config-kv-delete-ok";
    }
    if (path.equals("/minio/admin/v3/clear-config-history-kv")) {
      return "config-history-clear-ok";
    }
    if (path.equals("/minio/admin/v3/restore-config-history-kv")) {
      return "config-history-restore-ok";
    }
    return "";
  }

  private static String stage54AdminPolicyAndReplicationBody(String path) {
    if (path.equals("/minio/admin/v3/replication/mrf")) {
      return "{\"status\":\"ok\",\"nodeName\":\"node-1\",\"bucket\":\"bucket1\",\"object\":\"a.txt\",\"retryCount\":1}";
    }
    if (path.equals("/minio/admin/v3/tier/warm-tier")) {
      return "tier-verify-ok";
    }
    if (path.equals("/minio/admin/v3/idp/builtin/policy/attach")) {
      return "builtin-policy-attach-ok";
    }
    if (path.equals("/minio/admin/v3/idp/builtin/policy/detach")) {
      return "builtin-policy-detach-ok";
    }
    if (path.equals("/minio/admin/v3/idp/ldap/policy/attach")) {
      return "ldap-policy-attach-ok";
    }
    if (path.equals("/minio/admin/v3/idp/ldap/policy/detach")) {
      return "ldap-policy-detach-ok";
    }
    return "";
  }

  private static String stage53AdminMaintenanceBody(String path) {
    if (path.equals("/minio/admin/v3/heal/")) {
      return "heal-root-ok";
    }
    if (path.equals("/minio/admin/v3/heal/bucket1")) {
      return "heal-bucket-ok";
    }
    if (path.equals("/minio/admin/v3/heal/bucket1/prefix1")) {
      return "heal-prefix-ok";
    }
    if (path.equals("/minio/admin/v3/pools/decommission")) {
      return "pool-decommission-start-ok";
    }
    if (path.equals("/minio/admin/v3/pools/cancel")) {
      return "pool-decommission-cancel-ok";
    }
    if (path.equals("/minio/admin/v3/rebalance/start")) {
      return "rebalance-start-ok";
    }
    if (path.equals("/minio/admin/v3/rebalance/stop")) {
      return "rebalance-stop-ok";
    }
    return "";
  }

  private static String stage48AdminDiagnosticProbeBody(String path) {
    if (path.endsWith("/speedtest/client/devnull")) {
      return "client-devnull-ok";
    }
    if (path.endsWith("/speedtest/client/devnull/extratime")) {
      return "client-devnull-extra-time-ok";
    }
    if (path.endsWith("/site-replication/devnull")) {
      return "site-replication-devnull-ok";
    }
    if (path.endsWith("/site-replication/netperf")) {
      return "site-replication-netperf-ok";
    }
    if (path.endsWith("/speedtest/object")) {
      return "speedtest-object-ok";
    }
    if (path.endsWith("/speedtest/drive")) {
      return "speedtest-drive-ok";
    }
    if (path.endsWith("/speedtest/net")) {
      return "speedtest-net-ok";
    }
    if (path.endsWith("/speedtest/site")) {
      return "speedtest-site-ok";
    }
    if (path.endsWith("/speedtest")) {
      return "speedtest-ok";
    }
    return "";
  }

  private static String stage40AdminDiagnosticBody(String path) {
    if (path.endsWith("/metrics")) {
      return "# HELP minio_admin_requests_total Admin 请求数\nminio_admin_requests_total 1\n";
    }
    if (path.endsWith("/inspect-data")) {
      return "inspect-archive-bin";
    }
    if (path.endsWith("/profiling/start")) {
      return "profiling started";
    }
    if (path.endsWith("/profiling/download")) {
      return "profile-bin-14";
    }
    if (path.endsWith("/profile")) {
      return "profile text";
    }
    return "{}";
  }

  private static String stage41AdminIamIdpBody(String path) {
    if (path.endsWith("/idp/ldap/policy-entities")) {
      return "{\"UserMappings\":{\"user1\":[\"readonly\"]},\"GroupMappings\":{\"dev\":[\"readwrite\"]}}";
    }
    if (path.endsWith("/idp/ldap/list-access-keys")) {
      return "{\"serviceAccounts\":[{\"accessKey\":\"ldap-ak\",\"secretKey\":\"不应暴露\",\"accountStatus\":\"enabled\",\"policy\":\"{}\"}],\"stsKeys\":[]}";
    }
    if (path.endsWith("/idp/ldap/list-access-keys-bulk")) {
      return "{\"serviceAccounts\":[],\"stsKeys\":[{\"accessKey\":\"ldap-sts\",\"sessionToken\":\"不应暴露\",\"accountStatus\":\"enabled\"}]}";
    }
    if (path.endsWith("/idp/openid/list-access-keys-bulk")) {
      return "{\"serviceAccounts\":[{\"accessKey\":\"openid-ak\",\"secretKey\":\"不应暴露\",\"accountStatus\":\"enabled\"}],\"stsKeys\":[]}";
    }
    return "{}";
  }

  private static String stage42SiteReplicationPeerBody(String path) {
    if (path.endsWith("/site-replication/peer/idp-settings")) {
      return "{\"LDAP\":{\"IsLDAPEnabled\":true,\"LDAPUserDNSearchBase\":\"ou=users,dc=example,dc=com\","
          + "\"LDAPUserDNSearchFilter\":\"(uid=%s)\",\"LDAPGroupSearchBase\":\"ou=groups,dc=example,dc=com\","
          + "\"LDAPGroupSearchFilter\":\"(member=%d)\"},\"OpenID\":{\"Enabled\":true,\"Region\":\"us-east-1\","
          + "\"Roles\":{\"arn:minio:iam:::role/readonly\":{\"ClaimName\":\"policy\",\"ClientID\":\"不应暴露\","
          + "\"HashedClientSecret\":\"不应暴露\"}},\"ClaimProvider\":{\"ClaimName\":\"policy\","
          + "\"HashedClientSecret\":\"不应暴露\"}}}";
    }
    return "{}";
  }

  private static String stsSuccessXml() {
    return "<AssumeRoleResponse><AssumeRoleResult><Credentials>"
        + "<AccessKeyId>sts-access</AccessKeyId>"
        + "<SecretAccessKey>sts-secret</SecretAccessKey>"
        + "<SessionToken>sts-token</SessionToken>"
        + "<Expiration>2030-01-01T00:00:00Z</Expiration>"
        + "</Credentials></AssumeRoleResult></AssumeRoleResponse>";
  }

  private static String stage20S3ResponseBody(String query) {
    if (query != null && query.contains("attributes")) {
      return "<GetObjectAttributesOutput><ETag>\"etag\"</ETag><ObjectSize>42</ObjectSize><StorageClass>STANDARD</StorageClass></GetObjectAttributesOutput>";
    }
    if (query != null && query.contains("retention")) {
      return "<Retention><Mode>GOVERNANCE</Mode><RetainUntilDate>2030-01-01T00:00:00Z</RetainUntilDate></Retention>";
    }
    if (query != null && query.contains("legal-hold")) {
      return "<LegalHold><Status>ON</Status></LegalHold>";
    }
    return "";
  }

  private static String stage21BucketResponseBody(String query) {
    if (query != null && query.contains("cors")) {
      return "<CORSConfiguration><CORSRule><AllowedOrigin>*</AllowedOrigin><AllowedMethod>GET</AllowedMethod><AllowedHeader>Authorization</AllowedHeader><ExposeHeader>ETag</ExposeHeader><MaxAgeSeconds>60</MaxAgeSeconds></CORSRule></CORSConfiguration>";
    }
    if (query != null && query.contains("website")) {
      return "<WebsiteConfiguration><IndexDocument><Suffix>index.html</Suffix></IndexDocument></WebsiteConfiguration>";
    }
    if (query != null && query.contains("logging")) {
      return "<BucketLoggingStatus><LoggingEnabled><TargetBucket>logs</TargetBucket><TargetPrefix>app/</TargetPrefix></LoggingEnabled></BucketLoggingStatus>";
    }
    if (query != null && query.contains("policyStatus")) {
      return "<PolicyStatus><IsPublic>true</IsPublic></PolicyStatus>";
    }
    if (query != null && query.contains("accelerate")) {
      return "<AccelerateConfiguration><Status>Enabled</Status></AccelerateConfiguration>";
    }
    if (query != null && query.contains("requestPayment")) {
      return "<RequestPaymentConfiguration><Payer>Requester</Payer></RequestPaymentConfiguration>";
    }
    return "";
  }

  private static String stage27S3ResponseBody(String query) {
    if (query != null && query.contains("acl")) {
      return "<AccessControlPolicy>"
          + "<Owner><ID>owner-id</ID><DisplayName>root</DisplayName></Owner>"
          + "<AccessControlList><Grant><Grantee xsi:type=\"CanonicalUser\"><ID>owner-id</ID><DisplayName>root</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList>"
          + "</AccessControlPolicy>";
    }
    if (query != null && query.contains("select")) {
      return "select-response";
    }
    return "";
  }

  private static String stage28S3ResponseBody(String query) {
    if (query != null && query.contains("events=")) {
      return "event-stream";
    }
    if (query != null && query.contains("notification")) {
      return "<NotificationConfiguration><QueueConfiguration><Id>queue-1</Id><Queue>arn:minio:sqs::1:webhook</Queue><Event>s3:ObjectCreated:*</Event></QueueConfiguration></NotificationConfiguration>";
    }
    if (query != null && query.contains("replication-metrics=2")) {
      return "{\"uptime\":123,\"Stats\":{}}";
    }
    if (query != null && query.contains("replication-metrics")) {
      return "{\"Stats\":{}}";
    }
    return "";
  }

  private static void assertMonoMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getReturnType().equals(Mono.class)) {
        return;
      }
    }
    Assertions.fail("缺少返回 Mono 的方法: " + type.getSimpleName() + "." + name);
  }

  private static void assertFluxMethodExists(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getReturnType().equals(reactor.core.publisher.Flux.class)) {
        return;
      }
    }
    Assertions.fail("缺少返回 Flux 的方法: " + type.getSimpleName() + "." + name);
  }

  private static int countDistinctMonoMethods(Class<?> type, String prefix) {
    Set<String> names = new HashSet<String>();
    for (Method method : type.getMethods()) {
      if (method.getDeclaringClass().equals(type)
          && method.getReturnType().equals(Mono.class)
          && (prefix == null || method.getName().startsWith(prefix))) {
        names.add(method.getName());
      }
    }
    return names.size();
  }
}
