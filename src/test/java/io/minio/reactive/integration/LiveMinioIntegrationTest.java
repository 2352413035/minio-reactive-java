package io.minio.reactive.integration;

import io.minio.reactive.ReactiveMinioAdminClient;
import io.minio.reactive.ReactiveMinioClient;
import io.minio.reactive.ReactiveMinioHealthClient;
import io.minio.reactive.ReactiveMinioKmsClient;
import io.minio.reactive.ReactiveMinioRawClient;
import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.errors.ReactiveMinioKmsException;
import io.minio.reactive.errors.ReactiveS3Exception;
import io.minio.reactive.messages.kms.KmsJsonResult;
import io.minio.reactive.messages.BucketInfo;
import io.minio.reactive.messages.admin.AdminServerInfo;
import io.minio.reactive.messages.admin.AdminUserInfo;
import io.minio.reactive.messages.CompletePart;
import io.minio.reactive.messages.CompletedMultipartUpload;
import io.minio.reactive.messages.MultipartUpload;
import io.minio.reactive.messages.ObjectInfo;
import io.minio.reactive.messages.PartInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class LiveMinioIntegrationTest {
  private ReactiveMinioClient client;
  private ReactiveMinioAdminClient adminClient;
  private ReactiveMinioHealthClient healthClient;
  private ReactiveMinioKmsClient kmsClient;
  private ReactiveMinioRawClient rawClient;
  private String bucket;

  @BeforeEach
  void setUp() {
    String endpoint = System.getenv("MINIO_ENDPOINT");
    String accessKey = System.getenv("MINIO_ACCESS_KEY");
    String secretKey = System.getenv("MINIO_SECRET_KEY");
    String region = getenvOrDefault("MINIO_REGION", "us-east-1");

    Assumptions.assumeTrue(
        endpoint != null && accessKey != null && secretKey != null,
        "MINIO_ENDPOINT, MINIO_ACCESS_KEY and MINIO_SECRET_KEY must be set");

    client =
        ReactiveMinioClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();
    adminClient =
        ReactiveMinioAdminClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();
    healthClient =
        ReactiveMinioHealthClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();
    kmsClient =
        ReactiveMinioKmsClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();
    rawClient =
        ReactiveMinioRawClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();
    bucket = "reactive-it-" + UUID.randomUUID().toString().replace("-", "");
  }

  @AfterEach
  void cleanup() {
    if (client == null || bucket == null) {
      return;
    }
    List<String> knownObjects =
        Arrays.asList(
            "folder/a.txt",
            "folder/b.txt",
            "copy/a-copy.txt",
            "presigned.txt",
            "multipart.bin");
    for (String object : knownObjects) {
      client.removeObject(bucket, object).onErrorResume(error -> reactor.core.publisher.Mono.empty()).block();
    }
    client.removeBucket(bucket).onErrorResume(error -> reactor.core.publisher.Mono.empty()).block();
  }

  @Test
  void shouldExerciseCoreLiveMinioFeatures() throws Exception {
    Assertions.assertEquals(Boolean.FALSE, client.bucketExists(bucket).block());
    client.makeBucket(bucket).block();
    Assertions.assertEquals(Boolean.TRUE, client.bucketExists(bucket).block());

    List<BucketInfo> buckets = client.listBuckets().block();
    Assertions.assertTrue(containsBucket(buckets, bucket));
    Assertions.assertEquals(Boolean.TRUE, healthClient.isLive().block());
    Assertions.assertEquals(Integer.valueOf(200), healthClient.liveGet().block());
    Assertions.assertEquals(
        Integer.valueOf(200), rawClient.executeToStatus(MinioApiCatalog.byName("HEALTH_LIVE_GET")).block());
    Assertions.assertTrue(
        rawClient
            .executeToString(
                MinioApiCatalog.byName("S3_LIST_BUCKETS"),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                null,
                null)
            .block()
            .contains(bucket));
    AdminServerInfo typedAdminInfo = adminClient.getServerInfo().block();
    String adminInfo = adminClient.serverInfo().block();
    String rawAdminInfo =
        rawClient
            .executeToString(
                MinioApiCatalog.byName("ADMIN_SERVER_INFO"),
                emptyMap(),
                emptyMap(),
                emptyMap(),
                null,
                null)
            .block();
    Assertions.assertFalse(typedAdminInfo.deploymentId().isEmpty());
    Assertions.assertTrue(adminInfo.contains("deploymentID"));
    Assertions.assertTrue(rawAdminInfo.contains("deploymentID"));
    Assertions.assertTrue(adminInfo.contains("servers"));
    Assertions.assertTrue(rawAdminInfo.contains("servers"));
    assertKmsStatusIsTypedOrDiagnostic();

    client.putObject(bucket, "folder/a.txt", "alpha", "text/plain").block();
    client.putObject(bucket, "folder/b.txt", "bravo", "text/plain").block();

    List<ObjectInfo> listed = client.listObjects(bucket, "folder/", true).collectList().block();
    Assertions.assertEquals(2, listed.size());
    Assertions.assertEquals("alpha", client.getObjectAsString(bucket, "folder/a.txt").block());
    Assertions.assertFalse(client.statObject(bucket, "folder/a.txt").block().isEmpty());
    ReactiveS3Exception missingObject =
        Assertions.assertThrows(
            ReactiveS3Exception.class, () -> client.getObjectAsBytes(bucket, "missing-object.txt").block());
    Assertions.assertEquals(404, missingObject.statusCode());
    Assertions.assertFalse(missingObject.responseBody().isEmpty());

    Map<String, String> tags = new HashMap<String, String>();
    tags.put("purpose", "integration");
    tags.put("sdk", "reactive");
    client.setObjectTags(bucket, "folder/a.txt", tags).block();
    Assertions.assertEquals("integration", client.getObjectTags(bucket, "folder/a.txt").block().get("purpose"));
    client.deleteObjectTags(bucket, "folder/a.txt").block();

    client.copyObject(bucket, "copy/a-copy.txt", bucket, "folder/a.txt").block();
    Assertions.assertEquals("alpha", client.getObjectAsString(bucket, "copy/a-copy.txt").block());

    URI putUrl =
        client.getPresignedObjectUrl(HttpMethod.PUT, bucket, "presigned.txt", Duration.ofMinutes(5)).block();
    writeWithPresignedPut(putUrl, "from presign".getBytes(StandardCharsets.UTF_8));
    URI getUrl = client.getPresignedGetObjectUrl(bucket, "presigned.txt", Duration.ofMinutes(5)).block();
    Assertions.assertEquals("from presign", new String(readWithPresignedGet(getUrl), StandardCharsets.UTF_8));

    byte[] firstPart = filledBytes(5 * 1024 * 1024, (byte) 'a');
    byte[] secondPart = "tail".getBytes(StandardCharsets.UTF_8);
    MultipartUpload upload = client.createMultipartUpload(bucket, "multipart.bin", "application/octet-stream").block();
    PartInfo part1 = client.uploadPart(bucket, "multipart.bin", upload.uploadId(), 1, firstPart).block();
    PartInfo part2 = client.uploadPart(bucket, "multipart.bin", upload.uploadId(), 2, secondPart).block();
    Assertions.assertEquals(2, client.listParts(bucket, "multipart.bin", upload.uploadId()).block().parts().size());
    CompletedMultipartUpload completed =
        client
            .completeMultipartUpload(
                bucket,
                "multipart.bin",
                upload.uploadId(),
                Arrays.asList(new CompletePart(part1.partNumber(), part1.etag()), new CompletePart(part2.partNumber(), part2.etag())))
            .block();
    Assertions.assertEquals("multipart.bin", completed.key());
    Assertions.assertEquals(firstPart.length + secondPart.length, client.getObjectAsBytes(bucket, "multipart.bin").block().length);

    client
        .removeObjects(
            bucket,
            Arrays.asList(
                "folder/a.txt",
                "folder/b.txt",
                "copy/a-copy.txt",
                "presigned.txt",
                "multipart.bin"))
        .block();
    Assertions.assertTrue(client.listObjects(bucket).collectList().block().isEmpty());
  }



  @Test
  void shouldExerciseAdminUserLifecycle() {
    String accessKey = "reactiveuser" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    String secretKey = "reactive-secret-" + UUID.randomUUID().toString().replace("-", "");
    try {
      adminClient.addUser(accessKey, secretKey).block();
      AdminUserInfo userInfo = adminClient.getUserInfo(accessKey).block();
      Assertions.assertEquals("enabled", userInfo.status());

      adminClient.setUserEnabled(accessKey, false).block();
      AdminUserInfo disabled = adminClient.getUserInfo(accessKey).block();
      Assertions.assertEquals("disabled", disabled.status());
    } finally {
      try {
        adminClient.deleteUser(accessKey).onErrorResume(error -> reactor.core.publisher.Mono.empty()).block();
      } catch (Exception ignored) {
      }
    }
  }

  private void assertKmsStatusIsTypedOrDiagnostic() {
    try {
      KmsJsonResult status = kmsClient.getStatus().block();
      Assertions.assertNotNull(status);
      Assertions.assertNotNull(status.rawJson());
    } catch (ReactiveMinioKmsException error) {
      Assertions.assertEquals("kms", error.protocol());
      Assertions.assertTrue(error.statusCode() >= 400);
      Assertions.assertNotNull(error.rawBody());
    }
  }

  private static Map<String, String> emptyMap() {
    return java.util.Collections.<String, String>emptyMap();
  }

  private static boolean containsBucket(List<BucketInfo> buckets, String bucket) {
    if (buckets == null) {
      return false;
    }
    for (BucketInfo info : buckets) {
      if (bucket.equals(info.name())) {
        return true;
      }
    }
    return false;
  }

  private static byte[] filledBytes(int size, byte value) {
    byte[] bytes = new byte[size];
    Arrays.fill(bytes, value);
    return bytes;
  }

  private static void writeWithPresignedPut(URI uri, byte[] bytes) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
    connection.setRequestMethod("PUT");
    connection.setDoOutput(true);
    connection.setFixedLengthStreamingMode(bytes.length);
    OutputStream output = connection.getOutputStream();
    output.write(bytes);
    output.close();
    int status = connection.getResponseCode();
    if (status < 200 || status >= 300) {
      throw new AssertionError("presigned PUT failed with HTTP " + status + ": " + readError(connection));
    }
  }

  private static byte[] readWithPresignedGet(URI uri) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
    connection.setRequestMethod("GET");
    int status = connection.getResponseCode();
    if (status < 200 || status >= 300) {
      throw new AssertionError("presigned GET failed with HTTP " + status + ": " + readError(connection));
    }
    InputStream input = connection.getInputStream();
    try {
      return readAll(input);
    } finally {
      input.close();
    }
  }

  private static String readError(HttpURLConnection connection) throws Exception {
    InputStream input = connection.getErrorStream();
    if (input == null) {
      return "";
    }
    try {
      return new String(readAll(input), StandardCharsets.UTF_8);
    } finally {
      input.close();
    }
  }

  private static byte[] readAll(InputStream input) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read;
    while ((read = input.read(buffer)) >= 0) {
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  private static String getenvOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value;
  }
}
