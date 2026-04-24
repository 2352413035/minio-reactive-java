# 18 阶段 20：S3 对象治理 typed 能力

## 1. 背景

阶段 19 已经确认 SDK catalog 与 MinIO router 对齐，后续重点转为提升 `ReactiveMinioClient` 的产品化 typed 能力。阶段 20 优先处理对象级治理接口，因为这些能力属于对象存储主客户端的自然职责，比让用户直接拼 XML 或使用 raw 更符合 SDK 易用性目标。

## 2. 本阶段新增 typed 入口

| 能力 | typed 方法 | advanced 兼容入口 | 说明 |
| --- | --- | --- | --- |
| 对象属性 | `getObjectAttributes(...)` | `s3GetObjectAttributes(...)` | typed 方法解析 ETag、对象大小、存储类型、校验和、分片数量，并保留原始 XML。 |
| 对象保留策略 | `getObjectRetention(...)` / `setObjectRetention(...)` | `s3GetObjectRetention(...)` / `s3PutObjectRetention(...)` | 使用 `ObjectRetentionConfiguration` 表达 GOVERNANCE / COMPLIANCE 和保留到期时间。 |
| Legal Hold | `getObjectLegalHold(...)` / `setObjectLegalHold(...)` | `s3GetObjectLegalHold(...)` / `s3PutObjectLegalHold(...)` | 使用 `ObjectLegalHoldConfiguration` 表达 ON / OFF。 |
| 归档恢复 | `restoreObject(...)` | `s3PostRestoreObject(...)` | 使用 `RestoreObjectRequest` 表达恢复天数和恢复优先级。 |

advanced 兼容入口继续保留，但对应方法标记为 `@Deprecated`，提示业务代码迁移到 typed 入口。

## 3. 模型边界

### 3.1 `ObjectAttributes`

MinIO/S3 的 `GetObjectAttributes` 响应会随服务端版本和请求头变化。SDK 当前只提取稳定字段：

- `etag()`
- `objectSize()`
- `storageClass()`
- `checksumCrc32()` / `checksumCrc32c()` / `checksumSha1()` / `checksumSha256()`
- `totalPartsCount()`
- `rawXml()`

如果后续需要更细的分片明细，可以在不破坏现有 API 的前提下扩展模型。

### 3.2 Object Lock 相关配置

`ObjectRetentionConfiguration` 和 `ObjectLegalHoldConfiguration` 只负责构造和解析 S3 兼容 XML。实际调用是否成功取决于 bucket 是否启用 object lock、对象版本状态和服务端策略。共享 live 测试不能强行打开 object lock；遇到服务端能力或配置限制时，应把错误解释为环境边界，而不是把 SDK 方法改回 raw。

### 3.3 Restore

`RestoreObjectRequest` 覆盖最常见的恢复天数和恢复 tier。更复杂的 select restore 请求仍可先使用 advanced 字符串入口，等阶段后续再产品化。

## 4. 使用示例

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

## 5. 验证要求

- `S3XmlTest` 覆盖 XML 生成和解析。
- `ReactiveMinioSpecializedClientsTest` 覆盖请求 path/query/header 构造和 deprecated 迁移标记。
- `LiveMinioIntegrationTest` 在服务端支持时验证 `GetObjectAttributes`，如果服务端返回 400/501，则确认这是服务端能力或参数边界，而不是 SDK 请求链路错误。
- 能力矩阵 S3 `product-typed` 从 52 提升到 58，双分支保持一致。
