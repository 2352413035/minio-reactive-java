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

注意：MinIO 多个管理端写接口使用 madmin 加密载荷，例如新增用户、设置配置、服务账号等。当前 SDK 暂未引入 madmin 加密兼容实现，因此这些写接口不会伪装成“已完整强类型”。在加密载荷支持完成前，相关方法保留为高级兼容入口或 raw 兜底。

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
