package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioClient;
import io.minio.reactive.errors.ReactiveS3Exception;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Manual smoke example against a real MinIO server.
 *
 * <p>This example is intentionally linear so it can be used as a runnable walkthrough
 * for the current SDK prototype.
 */
public final class ReactiveMinioLiveExample {
  private static final String LOCAL_CONFIG_FILE = "config/minio-local.properties";

  private ReactiveMinioLiveExample() {}

  public static void main(String[] args) {
    Properties properties = loadProperties();

    String endpoint = read(properties, "minio.endpoint", "MINIO_ENDPOINT", "http://127.0.0.1:9000");
    String accessKey = required(properties, "minio.access-key", "MINIO_ACCESS_KEY");
    String secretKey = required(properties, "minio.secret-key", "MINIO_SECRET_KEY");
    String region = read(properties, "minio.region", "MINIO_REGION", "us-east-1");
    String bucket =
        read(
            properties,
            "minio.bucket",
            "MINIO_BUCKET",
            "reactive-demo-" + UUID.randomUUID().toString().replace("-", ""));
    String object = read(properties, "minio.object", "MINIO_OBJECT", "hello.txt");
    String content =
        read(properties, "minio.content", "MINIO_CONTENT", "hello from reactive minio sdk");

    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();

    Mono<Void> flow =
        client
            .bucketExists(bucket)
            .doOnNext(exists -> System.out.println("bucket exists before create: " + exists))
            .flatMap(
                exists -> {
                  if (exists) {
                    return Mono.empty();
                  }
                  System.out.println("creating bucket: " + bucket);
                  return client.makeBucket(bucket)
                      .doOnSuccess(ignored -> System.out.println("bucket created: " + bucket));
                })
            .then(client.putObject(bucket, object, content, "text/plain"))
            .doOnSuccess(ignored -> System.out.println("object uploaded: " + object))
            .then(client.statObject(bucket, object))
            .doOnNext(ReactiveMinioLiveExample::printHeaders)
            // This call previously exposed a response-body lifecycle bug in WebClient usage.
            .then(client.getObjectAsString(bucket, object))
            .doOnNext(value -> System.out.println("downloaded content: " + value))
            .then(client.removeObject(bucket, object))
            .doOnSuccess(ignored -> System.out.println("object removed: " + object))
            .then(client.removeBucket(bucket))
            .doOnSuccess(ignored -> System.out.println("bucket removed: " + bucket))
            .doOnError(
                ReactiveS3Exception.class,
                ex -> {
                  System.err.println("MinIO returned an S3 error.");
                  System.err.println("status = " + ex.statusCode());
                  System.err.println("body   = " + ex.responseBody());
                });

    flow.block();
  }

  private static Properties loadProperties() {
    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(Paths.get(LOCAL_CONFIG_FILE))) {
      properties.load(inputStream);
    } catch (IOException ignored) {
      // Local config is optional. Environment variables still work as fallback.
    }
    return properties;
  }

  private static String required(Properties properties, String propertyKey, String envKey) {
    String value = firstNonBlank(System.getenv(envKey), properties.getProperty(propertyKey));
    if (value == null) {
      throw new IllegalStateException(
          "Missing config value. Please set "
              + propertyKey
              + " in "
              + LOCAL_CONFIG_FILE
              + " or "
              + envKey);
    }
    return value;
  }

  private static String read(
      Properties properties, String propertyKey, String envKey, String defaultValue) {
    String value = firstNonBlank(System.getenv(envKey), properties.getProperty(propertyKey));
    return value == null ? defaultValue : value;
  }

  private static String firstNonBlank(String first, String second) {
    if (first != null && !first.trim().isEmpty()) {
      return first.trim();
    }
    if (second != null && !second.trim().isEmpty()) {
      return second.trim();
    }
    return null;
  }

  private static void printHeaders(Map<String, java.util.List<String>> headers) {
    System.out.println("object headers:");
    for (Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
      System.out.println("  " + entry.getKey() + " = " + entry.getValue());
    }
  }
}
