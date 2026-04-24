package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.errors.ReactiveMinioAdminException;
import io.minio.reactive.errors.ReactiveMinioProtocolException;
import io.minio.reactive.http.S3Request;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ReactiveMinioRawClientTest {
  @Test
  void shouldBuildRequestFromCatalogEndpoint() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    MinioApiEndpoint endpoint = MinioApiCatalog.byName("ADMIN_SET_USER_STATUS");
    Map<String, String> query = new LinkedHashMap<String, String>();
    query.put("accessKey", "user1");
    query.put("status", "enabled");

    S3Request request =
        client.requestFor(
            endpoint,
            Collections.<String, String>emptyMap(),
            query,
            Collections.<String, String>emptyMap(),
            "body".getBytes(StandardCharsets.UTF_8),
            "application/json");

    Assertions.assertEquals("/minio/admin/v3/set-user-status", request.canonicalUri());
    Assertions.assertEquals("accessKey=user1&status=enabled", request.canonicalQueryString());
    Assertions.assertEquals("4", request.headers().get("Content-Length"));
  }

  @Test
  void shouldExpandPathVariables() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> path = new LinkedHashMap<String, String>();
    path.put("bucket", "b");
    path.put("object", "folder/a b.txt");

    S3Request request =
        client.requestFor(
            MinioApiCatalog.byName("S3_GET_OBJECT"),
            path,
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap(),
            null,
            null);

    Assertions.assertEquals("/b/folder/a%20b.txt", request.canonicalUri());
  }

  @Test
  void shouldRejectMissingRequiredQuery() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                client.requestFor(
                    MinioApiCatalog.byName("ADMIN_SET_USER_STATUS"),
                    Collections.<String, String>emptyMap(),
                    Collections.<String, String>emptyMap(),
                    Collections.<String, String>emptyMap(),
                    null,
                    null));
    Assertions.assertTrue(error.getMessage().contains("缺少必填 query 参数"));
  }

  @Test
  void shouldRejectCallerSuppliedSignerHeaders() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> headers = new LinkedHashMap<String, String>();
    headers.put("Authorization", "malicious");

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                client.requestFor(
                    MinioApiCatalog.byName("S3_LIST_BUCKETS"),
                    Collections.<String, String>emptyMap(),
                    Collections.<String, String>emptyMap(),
                    headers,
                    null,
                    null));
    Assertions.assertTrue(error.getMessage().contains("签名器管理"));
  }

  @Test
  void shouldRejectSlashInSingleSegmentPathVariable() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> path = new LinkedHashMap<String, String>();
    path.put("bucket", "a/b");

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                client.requestFor(
                    MinioApiCatalog.byName("S3_HEAD_BUCKET"),
                    path,
                    Collections.<String, String>emptyMap(),
                    Collections.<String, String>emptyMap(),
                    null,
                    null));
    Assertions.assertTrue(error.getMessage().contains("只能对应单段路径"));
  }

  @Test
  void shouldUseStsSigningServiceForStsEndpoints() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();

    S3Request request =
        client.requestFor(
            MinioApiCatalog.byName("STS_ASSUME_ROLE_FORM"),
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap(),
            Collections.<String, String>emptyMap(),
            new byte[0],
            "application/x-www-form-urlencoded");

    Assertions.assertEquals("sts", request.serviceName());
  }

  @Test
  void shouldRejectDotSegmentsInSingleSegmentPathVariable() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> path = new LinkedHashMap<String, String>();
    path.put("tier", "..");

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                client.requestFor(
                    MinioApiCatalog.byName("ADMIN_VERIFY_TIER"),
                    path,
                    Collections.<String, String>emptyMap(),
                    Collections.<String, String>emptyMap(),
                    null,
                    null));
    Assertions.assertTrue(error.getMessage().contains("不能是"));
  }

  @Test
  void shouldAllowAuthorizationHeaderForBearerEndpoints() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> headers = new LinkedHashMap<String, String>();
    headers.put("Authorization", "Bearer token");
    Map<String, String> path = new LinkedHashMap<String, String>();
    path.put("pathComps", "");

    S3Request request =
        client.requestFor(
            MinioApiCatalog.byName("METRICS_V3"),
            path,
            Collections.<String, String>emptyMap(),
            headers,
            null,
            null);

    Assertions.assertEquals("Bearer token", request.headers().get("Authorization"));
  }

  @Test
  void shouldMapAdminJsonErrorsToProtocolException() {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.BAD_REQUEST)
                            .header("x-amz-request-id", "req-1")
                            .body("{\"code\":\"BadRequest\",\"message\":\"bad admin request\",\"requestId\":\"req-1\"}")
                            .build()))
            .build();
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    ReactiveMinioAdminException error =
        Assertions.assertThrows(
            ReactiveMinioAdminException.class,
            () -> client.executeToString(MinioApiCatalog.byName("ADMIN_SERVER_INFO")).block());

    Assertions.assertEquals("admin", error.protocol());
    Assertions.assertEquals(400, error.statusCode());
    Assertions.assertEquals("BadRequest", error.code());
    Assertions.assertEquals("bad admin request", error.errorMessage());
    Assertions.assertEquals("req-1", error.requestId());
    Assertions.assertEquals("ADMIN_SERVER_INFO", error.endpointName());
    Assertions.assertEquals("GET", error.method());
    Assertions.assertTrue(error.getMessage().contains("接口=ADMIN_SERVER_INFO"));
    Assertions.assertTrue(error.getMessage().contains("诊断建议="));
    Assertions.assertFalse(error.diagnosticHint().isEmpty());
  }

  @Test
  void shouldUseChineseBodySnippetWhenErrorIsPlainText() {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(
                request ->
                    Mono.just(
                        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("plain admin failure")
                            .build()))
            .build();
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .webClient(webClient)
            .credentials("ak", "sk")
            .build();

    ReactiveMinioAdminException error =
        Assertions.assertThrows(
            ReactiveMinioAdminException.class,
            () -> client.executeToString(MinioApiCatalog.byName("ADMIN_SERVER_INFO")).block());

    Assertions.assertTrue(error.getMessage().contains("MinIO admin 请求失败"));
    Assertions.assertTrue(error.getMessage().contains("响应体片段=plain admin failure"));
  }

}
