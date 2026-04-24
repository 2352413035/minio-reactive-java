package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioRawClient;
import io.minio.reactive.catalog.MinioApiCatalog;
import java.util.Collections;

/**
 * raw 兜底调用器示例。
 *
 * <p>业务代码应优先使用 `ReactiveMinioClient`、`ReactiveMinioAdminClient` 等专用 typed 客户端。
 * 只有当 MinIO 新增接口而 SDK 还没有来得及产品化，或者排障时需要直接查看原始响应，才建议使用 raw。
 */
public final class ReactiveMinioRawFallbackExample {
  private ReactiveMinioRawFallbackExample() {}

  public static void main(String[] args) {
    String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://127.0.0.1:9000");
    String accessKey = System.getenv().getOrDefault("MINIO_ACCESS_KEY", "your-access-key");
    String secretKey = System.getenv().getOrDefault("MINIO_SECRET_KEY", "your-secret-key");

    ReactiveMinioRawClient raw =
        ReactiveMinioRawClient.builder()
            .endpoint(endpoint)
            .region("us-east-1")
            .credentials(accessKey, secretKey)
            .build();

    String bucketsXml =
        raw.executeToString(
                MinioApiCatalog.byName("S3_LIST_BUCKETS"),
                Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap(),
                null,
                null)
            .block();
    System.out.println("原始 bucket 列表响应长度 = " + lengthOf(bucketsXml));

    String serverInfoJson =
        raw.executeToString(
                MinioApiCatalog.byName("ADMIN_SERVER_INFO"),
                Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap(),
                Collections.<String, String>emptyMap(),
                null,
                null)
            .block();
    System.out.println("原始服务信息响应长度 = " + lengthOf(serverInfoJson));
  }

  private static int lengthOf(String value) {
    return value == null ? 0 : value.length();
  }
}
