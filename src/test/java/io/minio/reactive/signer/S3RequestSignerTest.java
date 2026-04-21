package io.minio.reactive.signer;

import io.minio.reactive.ReactiveMinioClientConfig;
import io.minio.reactive.credentials.ReactiveCredentials;
import io.minio.reactive.http.S3Request;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class S3RequestSignerTest {
  @Test
  void shouldAddAuthorizationHeadersForSignedRequest() {
    S3Request request =
        S3Request.builder().method(HttpMethod.HEAD).bucket("demo").object("test.txt").build();

    S3Request signed =
        new S3RequestSigner()
            .sign(
                request,
                ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
                ReactiveCredentials.of("minioadmin", "minioadmin"));

    Assertions.assertTrue(signed.headers().containsKey("Authorization"));
    Assertions.assertTrue(signed.headers().containsKey("X-Amz-Date"));
    Assertions.assertTrue(signed.headers().containsKey("X-Amz-Content-Sha256"));
    Assertions.assertEquals("localhost:9000", signed.headers().get("Host"));
  }

  @Test
  void shouldNotAddAuthorizationForAnonymousRequest() {
    S3Request request = S3Request.builder().method(HttpMethod.HEAD).bucket("demo").build();

    S3Request signed =
        new S3RequestSigner()
            .sign(
                request,
                ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
                ReactiveCredentials.anonymous());

    Assertions.assertFalse(signed.headers().containsKey("Authorization"));
    Assertions.assertTrue(signed.headers().containsKey("X-Amz-Date"));
  }

  @Test
  void shouldGeneratePresignedUrl() {
    S3Request request =
        S3Request.builder().method(HttpMethod.GET).bucket("demo").object("a b.txt").build();

    URI uri =
        new S3RequestSigner()
            .presign(
                request,
                ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
                ReactiveCredentials.of("minioadmin", "minioadmin"),
                Duration.ofMinutes(10));

    String value = uri.toString();
    Assertions.assertTrue(value.startsWith("http://localhost:9000/demo/a%20b.txt?"));
    Assertions.assertTrue(value.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
    Assertions.assertTrue(value.contains("X-Amz-Credential=minioadmin%2F"));
    Assertions.assertTrue(value.contains("X-Amz-Expires=600"));
    Assertions.assertTrue(value.contains("X-Amz-SignedHeaders=host"));
    Assertions.assertTrue(value.contains("X-Amz-Signature="));
  }

  @Test
  void shouldRejectInvalidPresignExpiry() {
    S3Request request = S3Request.builder().method(HttpMethod.GET).bucket("demo").build();

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            new S3RequestSigner()
                .presign(
                    request,
                    ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
                    ReactiveCredentials.of("minioadmin", "minioadmin"),
                    Duration.ofDays(8)));
  }
  @Test
  void shouldUseRequestServiceNameInCredentialScope() {
    S3Request request =
        S3Request.builder().method(HttpMethod.POST).path("/").serviceName("sts").body(new byte[0]).build();

    S3Request signed =
        new S3RequestSigner()
            .sign(
                request,
                ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
                ReactiveCredentials.of("minioadmin", "minioadmin"));

    Assertions.assertTrue(signed.headers().get("Authorization").contains("/sts/aws4_request"));
  }

}
