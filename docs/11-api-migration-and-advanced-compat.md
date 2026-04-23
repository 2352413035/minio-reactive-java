# 11 API 迁移与高级兼容入口

## 为什么需要迁移表

当前 SDK 同时存在三类入口：

1. 推荐业务 API：参数清楚、返回对象清楚、错误更容易解释。
2. 高级兼容 API：历史上从 catalog 生成或薄封装出来的方法，通常返回 `Mono<String>`。
3. Raw 兜底 API：通过 `ReactiveMinioRawClient` 和 `MinioApiCatalog` 直接调用任意公开接口。

后续不会直接删除高级兼容 API。每个入口在删除或标记 `@Deprecated` 前，都必须先给出推荐替代方式和测试证明。

## 通用迁移原则

- 普通对象存储场景优先使用 `ReactiveMinioClient` 的业务方法。
- 管理端场景优先使用 `ReactiveMinioAdminClient` 的强业务方法。
- KMS 场景优先使用 `ReactiveMinioKmsClient` 的强业务方法。
- STS 场景优先使用 `ReactiveMinioStsClient` 的强业务方法。
- Metrics 场景优先使用 `ReactiveMinioMetricsClient` 的强业务方法。
- Health 场景优先使用 `ReactiveMinioHealthClient` 的强业务方法。
- 未封装或临时新增接口使用 `ReactiveMinioRawClient`。


## 当前 advanced 基线

本基线用于后续迁移时判断“新增 typed 方法是否真的替代了高级入口”，而不是继续扩大公开 API 面。统计口径为反射读取 `src/main/java/io/minio/reactive` 下各 public client 的公开 `Mono<String>` 方法、`@Deprecated` 方法和 raw-ish `executeTo*` 入口；重载方法逐个计数。

| 客户端 | public `Mono<String>` | `@Deprecated` | raw-ish `executeTo*` | 说明 |
| --- | ---: | ---: | ---: | --- |
| `ReactiveMinioClient` | 129 | 26 | 5 | 对象存储主客户端，仍含 S3 catalog 风格高级入口和少量底层执行入口。 |
| `ReactiveMinioAdminClient` | 201 | 0 | 0 | 管理端 advanced 入口最多，是后续 typed 化重点。 |
| `ReactiveMinioKmsClient` | 8 | 0 | 0 | KMS 已有第一批 typed 方法，兼容入口短期保留。 |
| `ReactiveMinioStsClient` | 14 | 0 | 0 | STS 已有请求对象和凭证结果模型，兼容入口短期保留。 |
| `ReactiveMinioMetricsClient` | 6 | 0 | 0 | Metrics 已有 Prometheus 文本模型，兼容入口短期保留。 |
| `ReactiveMinioHealthClient` | 0 | 0 | 0 | Health 当前已基本业务化。 |
| `ReactiveMinioRawClient` | 3 | 0 | 8 | raw 是永久兜底入口，不作为普通业务主路径收敛目标。 |
| 合计 | 361 | 26 | 13 | 后续每阶段记录净新增 typed、净替代 advanced 和仍需 raw 的数量。 |

统计命令示例：

```bash
mvn -q -Dtest=ReactiveMinioSpecializedClientsTest#shouldKeepAdvancedCompatibilityBaselineForMigration test
```

## S3 catalog 风格方法迁移

`ReactiveMinioClient` 中以 `s3` 开头、直接对应 catalog 的方法属于高级兼容入口。已有明确替代路径的方法会先标记 `@Deprecated`，短期仍保留二进制兼容，但 README 不作为主推荐。

| 高级入口类型 | 推荐替代 |
| --- | --- |
| `s3GetObject` | `getObject` / `getObjectAsBytes` / `getObjectAsString` |
| `s3PutObject` | `putObject` |
| `s3HeadObject` | `statObject` |
| `s3DeleteObject` | `removeObject` |
| `s3ListObjectsV2` | `listObjects` / `listObjectsPage` |
| `s3CreateMultipartUpload` | `createMultipartUpload` |
| `s3PutObjectPart` | `uploadPart` |
| `s3CompleteMultipartUpload` | `completeMultipartUpload` |
| `s3AbortMultipartUpload` | `abortMultipartUpload` |
| `s3GetObjectTagging` / `s3PutObjectTagging` / `s3DeleteObjectTagging` | `getObjectTags` / `setObjectTags` / `deleteObjectTags` |
| `s3GetBucketTagging` / `s3PutBucketTagging` | `getBucketTags` / `setBucketTags` |
| `s3GetBucketVersioning` / `s3PutBucketVersioning` | `getBucketVersioningConfiguration` / `setBucketVersioningConfiguration` / `setBucketVersioningEnabled` |
| 其它高级 S3 子资源 | 后续逐步补业务方法；补齐前可继续使用高级入口或 raw 兜底 |

## Admin 高级兼容入口迁移

`ReactiveMinioAdminClient` 中仍有大量返回 `Mono<String>` 的方法。这些方法短期保留，作为 advanced 入口。

第一批推荐业务方法：

| 推荐业务方法 | 说明 |
| --- | --- |
| `getServerInfo()` | 返回 `AdminServerInfo`，保留原始 JSON |
| `getStorageInfo()` | 返回 `AdminJsonResult` |
| `getDataUsageInfo()` | 返回 `AdminJsonResult` |
| `getAccountInfo()` | 返回 `AdminJsonResult` |
| `getUserInfo(accessKey)` | 返回 `AdminJsonResult` |
| `deleteUser(accessKey)` | 删除内部用户 |
| `setUserEnabled(accessKey, enabled)` | 启用或禁用用户 |
| `listUsersEncrypted()` | 默认 madmin 加密响应未解密前，明确返回 `EncryptedAdminResponse` |
| `listGroupsTyped()` / `getGroupInfo(group)` | 返回用户组 typed 模型 |
| `setGroupEnabled(group, enabled)` / `updateGroupMembers(request)` | 用户组可回滚写操作 |
| `createServiceAccount(request)` | 返回 `ServiceAccountCreateResult`，默认加密响应未解密时保留加密原文 |
| `getServiceAccountInfoEncrypted(accessKey)` / `listServiceAccountsEncrypted()` | 明确返回加密响应，不伪装成明文模型 |
| `getAccessKeyInfoTyped(accessKey)` / `listAccessKeysTyped(listType)` | 返回 access key typed 模型，适用于非默认加密阻塞的查询场景 |

注意：MinIO 多个管理端写接口使用 madmin 加密载荷，例如新增用户、设置配置、服务账号等。当前 SDK 已实现 PBKDF2/AES-GCM 写入方向，并已验证 Java 生成载荷可被 madmin-go 解密；但服务端默认加密响应可能使用 Argon2id，Java 端尚不能解密。因此写入方向可以逐步强类型化，读取加密响应的完整解析仍需等待 Argon2id/ChaCha20 兼容能力。

## KMS / STS / Metrics / Health 迁移

| 领域 | 推荐业务方法 |
| --- | --- |
| Health | `isLive()`、`isReady()`、`checkLiveness()`、`checkReadiness()`、`checkCluster()` |
| Metrics | `scrapeClusterMetrics()`、`scrapeNodeMetrics()`、`scrapeBucketMetrics()`、`scrapeResourceMetrics()`、`scrapeV3()` |
| KMS | `getStatus()`、`getApis()`、`getVersion()`、`listKeys()`、`createKey()`、`getKeyStatus()` |
| STS | `assumeRoleWithWebIdentityCredentials()`、`assumeRoleWithClientGrantsCredentials()`、`assumeRoleWithLdapCredentials()` |

## 删除或废弃规则

高级兼容入口只有满足以下条件后，才能考虑标记 `@Deprecated` 或删除：

1. 已有推荐业务方法覆盖同等能力。
2. README 和 docs 已提供迁移说明。
3. 单元测试覆盖推荐业务方法。
4. raw 兜底路径仍可调用同一接口。
5. JDK8 和 JDK17+ 两条分支均完成迁移和验证。


## madmin 加密兼容边界

MinIO 管理端的一些写接口并不是普通 JSON body。服务端会调用 `madmin.DecryptData` 解密请求体，例如新增用户、更新服务账号、设置配置等接口。`madmin-go` 的 `EncryptData` 格式由 32 字节 salt、1 字节算法 ID、8 字节 nonce 和 secure-io DARE 加密流组成，密钥派生涉及 Argon2id 或 PBKDF2，AEAD 可能是 AES-GCM 或 ChaCha20-Poly1305。

当前 Java SDK 已提供 `MadminEncryptionSupport.isEncrypted(...)`，并实现了 PBKDF2 + AES-GCM 路径的 Java 端加密/解密 round-trip。已用临时 Go 工具链验证：Java 生成的 PBKDF2/AES-GCM 载荷可以被 `madmin-go v3.0.109` 的 `DecryptData` 解密。反向方向仍有限制：`madmin-go` 默认可能生成 Argon2id/AES-GCM 或 Argon2id/ChaCha20-Poly1305 载荷，当前 Java 端尚不能解密这些默认响应载荷。

因此，在 madmin 加密兼容层完成之前：

- 新增用户、设置完整配置、设置配置 KV、服务账号创建/更新等依赖“请求加密载荷”的接口，可以基于 PBKDF2/AES-GCM 写入方向继续 typed 化；但涉及“读取服务端加密响应”的接口，仍需先补 Argon2id/ChaCha20 或其它兼容解密能力。
- 这类接口继续保留高级兼容入口或 raw 兜底入口，由调用方传入已经符合 MinIO madmin 格式的载荷。
- 后续把加密载荷接入 Admin 写接口前，必须保留 madmin-go 互操作测试；如果接口需要读取服务端加密响应，必须先补齐 Argon2id 相关解密能力或引入经批准依赖。


## 配置写接口当前边界

`ReactiveMinioAdminClient#setConfigKvText(...)` 和 `setConfigText(...)` 已经能生成 madmin 兼容加密请求体并发送到对应管理端接口。由于这类操作会直接修改 MinIO 服务端配置，集成测试不会在共享测试环境中执行真实配置写入。调用方使用前应先确认配置文本正确，并在可回滚环境中验证。
