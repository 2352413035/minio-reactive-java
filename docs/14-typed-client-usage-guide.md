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


### S3 版本和分片上传列表

阶段 16 起，版本列表和未完成分片上传列表已经有 typed 分页模型：

```java
ListObjectVersionsResult versions = client
    .listObjectVersionsPage("bucket", "prefix/", null, null, null, 1000)
    .block();

ListMultipartUploadsResult uploads = client
    .listMultipartUploadsPage("bucket", "prefix/", null, null, null, 1000)
    .block();
```

如果只关心普通对象版本或上传会话，可以使用 Flux 便捷入口：

```java
client.listObjectVersions("bucket", "prefix/", true).collectList().block();
client.listMultipartUploads("bucket", "prefix/", true).collectList().block();
```

### S3 对象治理

阶段 20 起，对象属性、保留策略、Legal Hold 和归档恢复也有 typed 入口：

```java
ObjectAttributes attributes = client.getObjectAttributes("bucket", "archive/a.txt").block();

client.setObjectRetention(
    "bucket",
    "archive/a.txt",
    ObjectRetentionConfiguration.governance("2030-01-01T00:00:00Z"))
    .block();

client.setObjectLegalHold("bucket", "archive/a.txt", ObjectLegalHoldConfiguration.enabled()).block();
client.restoreObject("bucket", "archive/a.txt", RestoreObjectRequest.of(7, "Standard")).block();
```

这些方法的 advanced 兼容入口仍保留，但已经标记为 `@Deprecated`，业务代码应优先迁移到 typed 模型。object lock 和 restore 是否能在真实环境成功，取决于 bucket/object 的服务端配置。

### S3 bucket 子资源

阶段 21 起，bucket CORS 和一批 bucket 子资源摘要也有 typed 入口：

```java
BucketCorsConfiguration cors = BucketCorsConfiguration.of(
    Collections.singletonList(
        new BucketCorsRule(
            Collections.singletonList("GET"),
            Collections.singletonList("*"),
            Collections.singletonList("Authorization"),
            Collections.singletonList("ETag"),
            60)));

client.setBucketCorsConfiguration("bucket", cors).block();
BucketCorsConfiguration currentCors = client.getBucketCorsConfiguration("bucket").block();
client.deleteBucketCorsConfiguration("bucket").block();

BucketWebsiteConfiguration website = client.getBucketWebsiteConfiguration("bucket").block();
BucketLoggingConfiguration logging = client.getBucketLoggingConfiguration("bucket").block();
BucketPolicyStatus policyStatus = client.getBucketPolicyStatus("bucket").block();
```

本地 MinIO router 当前没有登记 PUT website route，因此 SDK 只提供 website 的 get/delete typed 方法，不伪造服务端没有公开的 set 方法。

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

阶段 22 起，一批 Admin 只读状态接口先进入 `AdminJsonResult` typed 主路径：

```java
AdminJsonResult pools = admin.listPoolsInfo().block();
AdminJsonResult poolStatus = admin.getPoolStatus("pool-0").block();
AdminJsonResult rebalance = admin.getRebalanceStatus().block();
AdminJsonResult health = admin.getHealthInfo().block();
```

这些接口不是 destructive 操作，只读取状态；启动/停止 rebalance、decommission、service restart/update 等仍必须走风险分级和 lab 门禁。

阶段 42 起，站点复制 peer 的 IDP 设置也有安全摘要模型：

```java
AdminSiteReplicationPeerIdpSettings idp = admin.getSiteReplicationPeerIdpSettings().block();
boolean hasIdp = idp.identityProviderConfigured();
int openidRoles = idp.openidRoleCount();
```

该模型不会保留 `rawJson()`。原因是 MinIO 返回里可能包含 OIDC provider 的客户端标识或哈希密钥字段，SDK 只给业务代码暴露可用于判断配置是否存在和数量是否一致的安全摘要。

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

这些响应需要等 Crypto Gate Pass 后，才会进入明文 typed 模型。阶段 18 起，边界对象会暴露 `algorithm()` / `algorithmName()`，便于日志和排障说明当前遇到的是哪一种 madmin 加密响应。

## 错误诊断

非 S3 协议族异常会包含协议族、endpoint 名称、HTTP method、路径模板、状态码、requestId、原始响应摘要和中文排查提示。业务调用方可以按异常类型区分：

- `ReactiveMinioAdminException`
- `ReactiveMinioKmsException`
- `ReactiveMinioStsException`
- `ReactiveMinioMetricsException`
- `ReactiveMinioHealthException`


## STS / Metrics / Health

STS 已覆盖普通 AssumeRole、WebIdentity、ClientGrants、LDAP 四条 typed 主路径。证书登录、自定义 token 和 SSO 表单依赖独立安全环境或外部插件，当前保留 advanced 兼容入口。

Metrics 继续返回 `PrometheusMetrics`，解析 metric 名、label、value，同时保留原始文本；SDK 不为每个 MinIO 指标名生成固定 Java 字段。阶段 23 起，legacy metrics 也可通过 `scrapeLegacyMetrics(...)` 获得同样的 typed 包装。

KMS 除状态、版本、key 列表和 key 状态外，也提供 `scrapeMetrics()`，用于读取 KMS Prometheus 指标文本。

Health 同时提供业务判断方法和路由级状态方法：业务代码优先用 `isLive()` / `isReady()`，探针或网关场景再使用 `liveGet()` / `readyHead()` 等状态码入口。

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

## 可编译示例

仓库中的示例都放在 `io.minio.reactive.examples` 包下：

- `ReactiveMinioLiveExample`：对象存储创建桶、上传、下载、删除闭环。
- `ReactiveMinioTypedAdminExample`：Admin L1 只读摘要模型和加密响应边界。
- `ReactiveMinioRawFallbackExample`：用 catalog + raw 兜底调用 S3/Admin 原始接口。
- `ReactiveMinioOpsExample`：Health 布尔检查和 Metrics Prometheus 文本入口。

## 能力总表

后续发布或里程碑说明统一引用 `scripts/report-route-parity.py` 和 `scripts/report-capability-matrix.py` 产物，而不是手写统计值。路由对标（route parity）证明 SDK catalog 是否继续对齐 MinIO router；能力总表再说明 `route-catalog`、`product-typed`、`advanced-compatible`、`raw-fallback`、`encrypted-blocked`、`destructive-blocked` 六类口径。


## 阶段 35 IAM 边界

需要查看 bucket 相关用户时使用 `listBucketUsersInfo(bucket)`；需要诊断临时账号时使用 `getTemporaryAccountInfo(accessKey)`。如果接口返回 madmin 加密载荷，请继续使用 `EncryptedAdminResponse` 相关方法，不要在 Crypto Gate Pass 前自行假装成明文模型。
