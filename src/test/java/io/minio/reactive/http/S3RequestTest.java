package io.minio.reactive.http;

import io.minio.reactive.ReactiveMinioClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class S3RequestTest {
  @Test
  void shouldEncodeBucketObjectAndQueryIntoUri() {
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.GET)
            .bucket("my-bucket")
            .object("folder/a b.txt")
            .queryParameter("prefix", "a b")
            .build();

    String uri = request.toUri(ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1")).toString();

    Assertions.assertEquals(
        "http://localhost:9000/my-bucket/folder/a%20b.txt?prefix=a%20b",
        uri);
  }

  @Test
  void shouldPreserveEmptySubresourceQueryValue() {
    S3Request request =
        S3Request.builder()
            .method(HttpMethod.GET)
            .bucket("my-bucket")
            .queryParameter("location", null)
            .build();

    Assertions.assertEquals("location=", request.canonicalQueryString());
    Assertions.assertEquals(
        "http://localhost:9000/my-bucket?location=",
        request.toUri(ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1")).toString());
  }

  @Test
  void shouldTreatExplicitEmptyBodyAsBody() {
    S3Request request = S3Request.builder().method(HttpMethod.PUT).bucket("b").body(new byte[0]).build();

    Assertions.assertTrue(request.hasBody());
  }
}
