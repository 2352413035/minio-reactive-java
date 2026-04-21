package io.minio.reactive;

import io.minio.reactive.catalog.MinioApiCatalog;
import io.minio.reactive.catalog.MinioApiEndpoint;
import io.minio.reactive.http.S3Request;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
  }
  @Test
  void shouldRejectCallerSuppliedSignerHeaders() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> headers = new LinkedHashMap<String, String>();
    headers.put("Authorization", "malicious");

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
  }

  @Test
  void shouldRejectSlashInSingleSegmentPathVariable() {
    ReactiveMinioRawClient client =
        ReactiveMinioRawClient.builder().endpoint("http://localhost:9000").region("us-east-1").build();
    Map<String, String> path = new LinkedHashMap<String, String>();
    path.put("bucket", "a/b");

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

}
