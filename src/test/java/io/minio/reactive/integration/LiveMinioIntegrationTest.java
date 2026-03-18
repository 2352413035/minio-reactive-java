//package io.minio.reactive.integration;
//
//import io.minio.reactive.ReactiveMinioClient;
//import java.util.Map;
//import java.util.UUID;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Assumptions;
//import org.junit.jupiter.api.Test;
//
//class LiveMinioIntegrationTest {
//  @Test
//  void shouldRunAgainstRealMinio() {
//    String endpoint = System.getenv("MINIO_ENDPOINT");
//    String accessKey = System.getenv("MINIO_ACCESS_KEY");
//    String secretKey = System.getenv("MINIO_SECRET_KEY");
//    String region = getenvOrDefault("MINIO_REGION", "us-east-1");
//
//    Assumptions.assumeTrue(endpoint != null && accessKey != null && secretKey != null,
//        "MINIO_ENDPOINT, MINIO_ACCESS_KEY and MINIO_SECRET_KEY must be set");
//
//    ReactiveMinioClient client =
//        ReactiveMinioClient.builder()
//            .endpoint(endpoint)
//            .region(region)
//            .credentials(accessKey, secretKey)
//            .build();
//
//    String bucket = "reactive-it-" + UUID.randomUUID().toString().replace("-", "");
//    String object = "it.txt";
//    String content = "integration test payload";
//
//    try {
//      Boolean existsBefore = client.bucketExists(bucket).block();
//      Assertions.assertEquals(Boolean.FALSE, existsBefore);
//
//      client.makeBucket(bucket).block();
//      Assertions.assertEquals(Boolean.TRUE, client.bucketExists(bucket).block());
//
//      client.putObject(bucket, object, content, "text/plain").block();
//
//      Map<String, java.util.List<String>> headers = client.statObject(bucket, object).block();
//      Assertions.assertNotNull(headers);
//      Assertions.assertTrue(headers.containsKey("Content-Length") || headers.containsKey("content-length"));
//
//      String downloaded = client.getObjectAsString(bucket, object).block();
//      Assertions.assertEquals(content, downloaded);
//
//      client.removeObject(bucket, object).block();
//      client.removeBucket(bucket).block();
//    } finally {
//      try {
//        client.removeObject(bucket, object).onErrorResume(ex -> reactor.core.publisher.Mono.empty()).block();
//      } catch (Exception ignored) {
//      }
//      try {
//        client.removeBucket(bucket).onErrorResume(ex -> reactor.core.publisher.Mono.empty()).block();
//      } catch (Exception ignored) {
//      }
//    }
//  }
//
//  private static String getenvOrDefault(String name, String defaultValue) {
//    String value = System.getenv(name);
//    return value == null || value.trim().isEmpty() ? defaultValue : value;
//  }
//}
