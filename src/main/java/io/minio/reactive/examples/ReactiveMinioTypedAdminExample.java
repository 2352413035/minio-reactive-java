package io.minio.reactive.examples;

import io.minio.reactive.ReactiveMinioAdminClient;
import io.minio.reactive.messages.admin.UpdateGroupMembersRequest;
import java.util.Collections;

/**
 * typed Admin 客户端示例。
 *
 * <p>这个示例不建议直接在共享 MinIO 上执行，它主要展示：
 * 1. 用户组 typed 方法
 * 2. Access key typed 查询
 * 3. 服务账号 typed / 加密边界方法
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

    admin.listGroupsTyped().doOnNext(groups -> System.out.println("groups = " + groups.groups())).block();
    admin
        .updateGroupMembers(UpdateGroupMembersRequest.add("demo-group", Collections.singletonList("demo-user")))
        .doOnSuccess(ignored -> System.out.println("group member update submitted"))
        .block();
    admin
        .getAccessKeyInfoTyped(accessKey)
        .doOnNext(info -> System.out.println("access key owner = " + info.parentUser()))
        .block();
    admin
        .listServiceAccountsEncrypted()
        .doOnNext(body -> System.out.println("service account response encrypted = " + body.isEncrypted()))
        .block();
  }
}
