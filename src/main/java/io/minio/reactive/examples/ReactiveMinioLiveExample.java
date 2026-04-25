package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioClient;
import io.minio.reactive.errors.ReactiveS3Exception;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 连接真实 MinIO 的手工冒烟示例。
 *
 * <p>这段代码故意写成线性流程，目的是让你把“一个完整的对象操作闭环”读清楚：
 * 检查桶、建桶、上传、查元数据、下载、删对象、删桶。
 */
public final class ReactiveMinioLiveExample {
  private static final String LOCAL_CONFIG_FILE = "minio-local.properties";

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
        read(properties, "minio.content", "MINIO_CONTENT", "你好，来自 reactive minio sdk");

    ReactiveMinioClient client =
        ReactiveMinioClient.builder()
            .endpoint(endpoint)
            .region(region)
            .credentials(accessKey, secretKey)
            .build();

    Mono<Void> flow =
        client
            .bucketExists(bucket)
            .doOnNext(exists -> System.out.println("建桶前 bucket 是否存在：" + exists))
            .flatMap(
                exists -> {
                  if (exists) {
                    return Mono.empty();
                  }
                  System.out.println("正在创建 bucket：" + bucket);
                  return client.makeBucket(bucket)
                      .doOnSuccess(ignored -> System.out.println("bucket 已创建：" + bucket));
                })
            .then(client.putObject(bucket, object, content, "text/plain"))
            .doOnSuccess(ignored -> System.out.println("对象已上传：" + object))
            .then(client.statObject(bucket, object))
            .doOnNext(ReactiveMinioLiveExample::printHeaders)
            // 这一段曾经暴露过 WebClient 响应体生命周期处理错误，因此保留下来作为验证点。
            .then(client.getObjectAsString(bucket, object))
            .doOnNext(value -> System.out.println("下载内容：" + value))
            .then(client.removeObject(bucket, object))
            .doOnSuccess(ignored -> System.out.println("对象已删除：" + object))
            .then(client.removeBucket(bucket))
            .doOnSuccess(ignored -> System.out.println("bucket 已删除：" + bucket))
            .doOnError(
                ReactiveS3Exception.class,
                ex -> {
                  System.err.println("MinIO 返回了 S3 错误。");
                  System.err.println("状态码 = " + ex.statusCode());
                  System.err.println("响应体 = " + ex.responseBody());
                });

    flow.block();
  }

  private static Properties loadProperties() {
    Properties properties = new Properties();
    try (InputStream inputStream =
        ReactiveMinioLiveExample.class.getClassLoader().getResourceAsStream(LOCAL_CONFIG_FILE)) {
      if (inputStream != null) {
        properties.load(inputStream);
      }
    } catch (Exception ignored) {
      // 本地配置文件是可选的，没有时仍然允许使用环境变量。
    }
    return properties;
  }

  private static String required(Properties properties, String propertyKey, String envKey) {
    String value = firstNonBlank(System.getenv(envKey), properties.getProperty(propertyKey));
    if (value == null) {
      throw new IllegalStateException(
          "缺少必需配置，请在 classpath 资源 "
              + LOCAL_CONFIG_FILE
              + " 中设置 "
              + propertyKey
              + "，或设置环境变量 "
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
    System.out.println("对象响应头：");
    for (Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
      System.out.println("  " + entry.getKey() + " = " + entry.getValue());
    }
  }
}
