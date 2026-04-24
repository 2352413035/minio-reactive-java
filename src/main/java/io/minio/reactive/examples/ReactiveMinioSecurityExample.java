package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioKmsClient;
import io.minio.reactive.ReactiveMinioStsClient;
import io.minio.reactive.messages.sts.AssumeRoleRequest;
import reactor.core.publisher.Mono;

/**
 * KMS 与 STS 安全能力示例。
 *
 * <p>KMS 和 STS 都是对象存储之外的平级专用客户端。KMS 常用于检查密钥服务状态，
 * STS 常用于把长期凭证换成有过期时间的临时凭证。不同 MinIO 部署可能没有打开 KMS 或 STS，
 * 因此示例把不可用情况打印成中文解释，而不是把排障信息吞掉。
 */
public final class ReactiveMinioSecurityExample {
  private ReactiveMinioSecurityExample() {}

  public static void main(String[] args) {
    String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://127.0.0.1:9000");
    String accessKey = System.getenv().getOrDefault("MINIO_ACCESS_KEY", "your-access-key");
    String secretKey = System.getenv().getOrDefault("MINIO_SECRET_KEY", "your-secret-key");
    String kmsKeyId = System.getenv("MINIO_KMS_KEY_ID");

    ReactiveMinioKmsClient kms =
        ReactiveMinioKmsClient.builder()
            .endpoint(endpoint)
            .region("us-east-1")
            .credentials(accessKey, secretKey)
            .build();

    kms.getStatus()
        .doOnNext(status -> System.out.println("KMS 状态字段 = " + status.values().keySet()))
        .onErrorResume(ReactiveMinioSecurityExample::printOptionalFeatureError)
        .block();

    if (notBlank(kmsKeyId)) {
      kms.getKeyStatus(kmsKeyId)
          .doOnNext(status -> System.out.println("KMS key 可用 = " + status.isOk()))
          .onErrorResume(ReactiveMinioSecurityExample::printOptionalFeatureError)
          .block();
    }

    ReactiveMinioStsClient sts =
        ReactiveMinioStsClient.builder()
            .endpoint(endpoint)
            .region("us-east-1")
            .credentials(accessKey, secretKey)
            .build();

    AssumeRoleRequest request = AssumeRoleRequest.builder().durationSeconds(900).build();
    sts.assumeRoleCredentials(request)
        .doOnNext(
            result ->
                System.out.println(
                    "STS 临时凭证已返回，过期时间 = "
                        + result.expiration()
                        + "，accessKey 已设置 = "
                        + (result.credentials() != null && result.credentials().accessKey() != null)))
        .onErrorResume(ReactiveMinioSecurityExample::printOptionalFeatureError)
        .block();
  }

  private static <T> Mono<T> printOptionalFeatureError(Throwable error) {
    System.err.println("可选安全能力当前不可用或未配置：" + error.getMessage());
    return Mono.empty();
  }

  private static boolean notBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
