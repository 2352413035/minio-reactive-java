# 14 typed 客户端使用指南

## 总原则

本 SDK 的推荐使用顺序是：

1. 先用领域 typed 客户端。
2. typed 尚未覆盖时，使用 advanced 兼容入口。
3. advanced 仍不合适时，使用 `ReactiveMinioRawClient` + `MinioApiCatalog` 兜底。

raw 是灵活兜底，不是普通业务主路径。

## 对象存储

```java
ReactiveMinioClient client = ReactiveMinioClient.builder()
    .endpoint(endpoint)
    .region("us-east-1")
    .credentials(accessKey, secretKey)
    .build();

client.putObject("bucket", "hello.txt", "hello", "text/plain").block();
String text = client.getObjectAsString("bucket", "hello.txt").block();
```

常用对象标签和 bucket versioning 已有 typed 入口：

```java
client.setObjectTags("bucket", "hello.txt", tags).block();
client.setBucketVersioningEnabled("bucket", true).block();
```

## 管理端

```java
ReactiveMinioAdminClient admin = ReactiveMinioAdminClient.builder()
    .endpoint(endpoint)
    .region("us-east-1")
    .credentials(accessKey, secretKey)
    .build();

admin.addUser("test-user", "test-secret").block();
admin.setUserEnabled("test-user", false).block();
admin.deleteUser("test-user").block();
```

用户、用户组、策略、服务账号等能力逐步提供 typed 方法。服务账号创建响应在默认 Argon2id 解密能力完成前可能返回加密载荷，SDK 不会把它伪装成已解析凭证。

阶段 15 起，Admin 只读信息优先使用“摘要 + 原始 JSON”的产品模型：

```java
AdminStorageSummary storage = admin.getStorageSummary().block();
AdminDataUsageSummary usage = admin.getDataUsageSummary().block();
AdminAccountSummary account = admin.getAccountSummary().block();
AdminConfigHelp apiHelp = admin.getConfigHelp("api").block();
```

这些方法不是 raw 的薄包装：模型会提取常用稳定字段，同时通过 `rawJson()` / `values()` 保留 MinIO 返回的完整内容，方便后续版本继续补字段。

部分 Admin 只读能力依赖特定环境或配置，默认不作为共享 live 门禁：

```java
AdminBucketQuota quota = admin.getBucketQuotaInfo("bucket").block();
AdminTierList tiers = admin.listTiers().block();
```

如果接口返回 madmin 加密载荷，SDK 会显式返回边界对象，而不是假装已经解密：

```java
EncryptedAdminResponse config = admin.getConfigEncrypted().block();
EncryptedAdminResponse configKv = admin.getConfigKvEncrypted("api").block();
EncryptedAdminResponse history = admin.listConfigHistoryKvEncrypted(10).block();
EncryptedAdminResponse accessKeyInfo = admin.getAccessKeyInfoEncrypted("svc-access-key").block();
EncryptedAdminResponse accessKeys = admin.listAccessKeysEncrypted("all").block();
```

这些响应需要等 Crypto Gate Pass 后，才会进入明文 typed 模型。

## 错误诊断

非 S3 协议族异常会包含协议族、endpoint 名称、HTTP method、路径模板、状态码、requestId、原始响应摘要和中文排查提示。业务调用方可以按异常类型区分：

- `ReactiveMinioAdminException`
- `ReactiveMinioKmsException`
- `ReactiveMinioStsException`
- `ReactiveMinioMetricsException`
- `ReactiveMinioHealthException`

## Raw 兜底

当 MinIO 新增了 SDK 尚未封装的接口时，可以使用 raw：

```java
String body = raw.executeToString(
    MinioApiCatalog.byName("ADMIN_SERVER_INFO"),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    null,
    null).block();
```

使用 raw 前应先确认接口风险等级，尤其是 Admin 写操作。

## 能力总表

后续发布或里程碑说明统一引用 `scripts/report-route-parity.py` 和 `scripts/report-capability-matrix.py` 产物，而不是手写统计值。路由对标（route parity）证明 SDK catalog 是否继续对齐 MinIO router；能力总表再说明 `route-catalog`、`product-typed`、`advanced-compatible`、`raw-fallback`、`encrypted-blocked`、`destructive-blocked` 六类口径。
