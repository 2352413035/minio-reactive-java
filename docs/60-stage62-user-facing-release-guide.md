# 阶段 62：用户面发布与快速使用指南

## 1. 给普通集成方的结论

如果你只是要在业务项目里接入 MinIO 对象存储，请优先使用 `ReactiveMinioClient`。不要先从 `ReactiveMinioRawClient` 开始，也不要把 Admin/KMS/STS/Metrics/Health 都塞到对象存储客户端下面。

当前 SDK 的用户入口分为平级客户端：

| 场景 | 首选入口 | 典型用途 |
| --- | --- | --- |
| 对象存储 | `ReactiveMinioClient` | 上传、下载、列对象、分片上传、对象标签、bucket 子资源。 |
| 管理端 | `ReactiveMinioAdminClient` | 用户、策略、配置、服务信息、站点复制、维护操作和风险边界。 |
| KMS | `ReactiveMinioKmsClient` | key 状态、key 生命周期和 KMS metrics。 |
| STS | `ReactiveMinioStsClient` | 临时凭证、WebIdentity、ClientGrants、LDAP、SSO/证书/自定义 token 兼容入口。 |
| Metrics | `ReactiveMinioMetricsClient` | Prometheus 文本和指标样本解析。 |
| Health | `ReactiveMinioHealthClient` | liveness/readiness 探针。 |
| 兜底或新接口 | `ReactiveMinioRawClient` | SDK 尚未封装的新路由、临时兼容或高级排障。 |

## 2. 最短对象存储示例

```java
ReactiveMinioClient client = ReactiveMinioClient.builder()
    .endpoint("http://127.0.0.1:9000")
    .region("us-east-1")
    .credentials(accessKey, secretKey)
    .build();

client.makeBucket("demo-bucket").block();
client.putObject("demo-bucket", "hello.txt", "你好，MinIO", "text/plain").block();
String text = client.getObjectAsString("demo-bucket", "hello.txt").block();
client.removeObject("demo-bucket", "hello.txt").block();
client.removeBucket("demo-bucket").block();
```

业务代码应优先使用这些强语义方法，因为它们会处理签名、路径、错误信息和常见模型。raw 只在 SDK 暂未及时封装某个新增路由时使用。

## 3. 管理端使用边界

Admin 客户端已经达到 product-typed 128 / 128，但这不代表每个 Admin 操作都适合在共享环境里执行。

建议分三类使用：

1. **安全只读**：例如 server info、data usage、storage info、bucket quota 查询、health info，可在有权限的环境中读取。
2. **高风险但有产品边界**：例如 tier、remote target、batch job、site replication、force-unlock，SDK 提供明确入口和参数校验，但真实执行必须在独立可回滚 lab。
3. **加密响应边界**：默认 madmin 加密响应只返回 `EncryptedAdminResponse`，Crypto Gate Pass 前不会伪装成明文模型。

示例：

```java
ReactiveMinioAdminClient admin = ReactiveMinioAdminClient.builder()
    .endpoint(endpoint)
    .region("us-east-1")
    .credentials(accessKey, secretKey)
    .build();

AdminStorageSummary storage = admin.getStorageSummary().block();
EncryptedAdminResponse config = admin.getConfigEncrypted().block();
```

看到 `EncryptedAdminResponse` 时，含义是“服务端返回了默认 madmin 加密载荷，SDK 已识别边界，但没有解密”。这不是失败，也不是明文 JSON。

## 4. raw 兜底怎么用

raw 的定位是“兜底调用器”：当 MinIO 新增了 SDK 尚未封装的 API，或者你需要临时验证 catalog 路由时，可以通过 `MinioApiCatalog` 调用。

```java
String body = raw.executeToString(
    MinioApiCatalog.byName("ADMIN_SERVER_INFO"),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    null,
    null).block();
```

使用 raw 前必须确认三点：

1. 这个路由是否会修改服务端状态。
2. 这个路由是否涉及加密响应或敏感字段。
3. 是否已有专用客户端方法；如果已有，应优先使用专用客户端。

## 5. 当前发布候选口径

当前双分支共同口径：

| 口径 | 结果 |
| --- | --- |
| route parity | 233 / 233，缺失 0，额外 0。 |
| product-typed | S3 77 / 77、Admin 128 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8。 |
| raw fallback | 0。 |
| Crypto Gate | Fail，`encrypted-blocked = 11`。 |
| 破坏性 lab | 未配置，`destructive-blocked = 29`。 |
| 版本 | 继续使用 `0.1.0-SNAPSHOT`，暂不打 1.0。 |

`0.1.0-SNAPSHOT` 的含义是：公开路由、调用入口、产品边界和验证证据已经闭环；阶段 63 已确认当前没有公开路由、产品入口或 raw-only 缺口。仍需等待 Crypto Gate 与独立 lab，才能宣称所有风险能力也完成真实执行验证。

## 6. 真实 MinIO 与 mc 证据

真实 MinIO 集成测试继续使用运行时环境变量，不把凭证写入仓库：

```bash
MINIO_ENDPOINT=http://127.0.0.1:9000 \
MINIO_ACCESS_KEY=your-access-key \
MINIO_SECRET_KEY=your-secret-key \
MINIO_REGION=us-east-1 \
mvn -q -Dtest=LiveMinioIntegrationTest test
```

阶段 61 还使用 `mc` 做过只读旁证：端点 healthy、根路径可列、Admin info 可读。该证据只能说明共享环境可用于 smoke 和只读观测，不能证明破坏性写入已经通过。

## 7. 双分支选择

| 分支 | Java 基线 | 适用场景 |
| --- | --- | --- |
| `master` | JDK8 | 需要 Java 8 兼容的项目。 |
| `chore/jdk17-springboot3` | JDK17+ | Spring Boot 3、较新 Java 运行时或高版本构建环境。 |

两条分支的 SDK 语义应保持一致。新增公开 API、示例、文档和测试时必须同步双分支。

## 8. 后续不要做什么

- 不要为了“数字好看”降低 `encrypted-blocked` 或 `destructive-blocked`。
- 不要用共享 MinIO 执行破坏性 Admin 写入。
- 不要把真实 access key、secret key、页面登录密码写入仓库。
- 不要重复新增平行 API；如果已有产品入口，下一步应深化返回模型或补真实 lab 证据。
