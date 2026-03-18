//package io.minio.reactive.signer;
//
//import io.minio.reactive.ReactiveMinioClientConfig;
//import io.minio.reactive.credentials.ReactiveCredentials;
//import io.minio.reactive.http.S3Request;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.HttpMethod;
//
//class S3RequestSignerTest {
//  @Test
//  void shouldAddAuthorizationHeadersForSignedRequest() {
//    S3Request request =
//        S3Request.builder().method(HttpMethod.HEAD).bucket("demo").object("test.txt").build();
//
//    S3Request signed =
//        new S3RequestSigner()
//            .sign(
//                request,
//                ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
//                ReactiveCredentials.of("minioadmin", "minioadmin"));
//
//    Assertions.assertTrue(signed.headers().containsKey("Authorization"));
//    Assertions.assertTrue(signed.headers().containsKey("X-Amz-Date"));
//    Assertions.assertTrue(signed.headers().containsKey("X-Amz-Content-Sha256"));
//    Assertions.assertEquals("localhost:9000", signed.headers().get("Host"));
//  }
//
//  @Test
//  void shouldNotAddAuthorizationForAnonymousRequest() {
//    S3Request request = S3Request.builder().method(HttpMethod.HEAD).bucket("demo").build();
//
//    S3Request signed =
//        new S3RequestSigner()
//            .sign(
//                request,
//                ReactiveMinioClientConfig.of("http://localhost:9000", "us-east-1"),
//                ReactiveCredentials.anonymous());
//
//    Assertions.assertFalse(signed.headers().containsKey("Authorization"));
//    Assertions.assertTrue(signed.headers().containsKey("X-Amz-Date"));
//  }
//}
