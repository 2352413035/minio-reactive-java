package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioAdminClient;

/**
 * 管理端 typed 客户端示例。
 *
 * <p>这个示例只调用共享环境中相对安全的只读接口，重点展示三件事：
 * 1. L1 只读接口优先返回业务摘要模型；
 * 2. 配置帮助属于明文安全接口，可以直接解析成 typed 结果；
 * 3. 配置、服务账号等默认加密响应只暴露加密边界，不伪装成明文模型。
 */
public final class ReactiveMinioTypedAdminExample {
  private ReactiveMinioTypedAdminExample() {}

  public static void main(String[] args) {
    String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://127.0.0.1:9000");
    String accessKey = System.getenv().getOrDefault("MINIO_ACCESS_KEY", "your-access-key");
    String secretKey = System.getenv().getOrDefault("MINIO_SECRET_KEY", "your-secret-key");

    ReactiveMinioAdminClient admin =
        ReactiveMinioAdminClient.builder()
            .endpoint(endpoint)
            .region("us-east-1")
            .credentials(accessKey, secretKey)
            .build();

    admin
        .listGroupsTyped()
        .doOnNext(groups -> System.out.println("用户组列表 = " + groups.groups()))
        .block();
    admin
        .getStorageSummary()
        .doOnNext(summary -> System.out.println("在线磁盘数 = " + summary.onlineDiskCount()))
        .block();
    admin
        .getDataUsageSummary()
        .doOnNext(summary -> System.out.println("对象数量 = " + summary.objectsCount()))
        .block();
    admin
        .getConfigHelp("api")
        .doOnNext(help -> System.out.println("api 配置帮助字段 = " + help.keys()))
        .block();
    admin
        .getConfigEncrypted()
        .doOnNext(body -> System.out.println("配置加密算法 = " + body.algorithmName()))
        .block();
    admin
        .listServiceAccountsEncrypted()
        .doOnNext(body -> System.out.println("服务账号响应是否加密 = " + body.isEncrypted()))
        .block();
  }
}
