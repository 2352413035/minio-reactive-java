# 25 阶段 27 S3 ACL 与 Select typed 边界

## 1. 目标

阶段 27 继续提升 `ReactiveMinioClient` 的对象存储 typed 成熟度，优先处理仍停留在 advanced-compatible 的 ACL 与 SelectObjectContent：

1. ACL 有明确 XML 响应结构，适合先提供只读模型和 canned ACL 便捷写入。
2. SelectObjectContent 请求结构稳定，但响应是事件流；当前先提供请求模型和原始响应边界，避免把事件流误说成普通字符串 JSON。

## 2. 新增模型

- `AccessControlOwner`：ACL Owner 的 ID 和 DisplayName。
- `AccessControlGrant`：ACL Grant 的 grantee type、ID、URI、email 和 permission。
- `AccessControlPolicy`：Owner、Grant 列表和原始 XML。
- `CannedAcl`：`private`、`public-read`、`bucket-owner-full-control` 等 canned ACL 请求头取值。
- `SelectObjectContentRequest`：S3 Select 请求表达式、输入/输出序列化 XML 和进度开关。
- `SelectObjectContentResult`：当前阶段的 Select 响应边界，保留原始响应。

## 3. 新增方法

| 方法 | 说明 |
| --- | --- |
| `getObjectAcl(bucket, object)` | GET 对象 ACL，并解析为 `AccessControlPolicy`。 |
| `setObjectCannedAcl(bucket, object, cannedAcl)` | 通过 `x-amz-acl` header 设置对象 canned ACL。 |
| `getBucketAcl(bucket)` | GET bucket ACL，并解析为 `AccessControlPolicy`。 |
| `setBucketCannedAcl(bucket, cannedAcl)` | 通过 `x-amz-acl` header 设置 bucket canned ACL。 |
| `selectObjectContent(bucket, object, request)` | 使用 typed request 发起 SelectObjectContent，返回 `SelectObjectContentResult`。 |

对应 advanced 方法 `s3GetObjectAcl`、`s3PutObjectAcl`、`s3GetBucketAcl`、`s3PutBucketAcl`、`s3SelectObjectContent` 已标记为 `@Deprecated`，保留兼容迁移入口。

## 4. Select 事件流边界

S3 Select 的响应不是普通 XML/JSON，而是 AWS/MinIO 兼容事件流。当前阶段的目标是：

1. 让用户不再手写 Select 请求 XML。
2. 明确 SDK 已识别该能力，但仍保留原始响应边界。
3. 为后续 `records/progress/stats/end` 事件拆分模型留出 public API 空间。

因此 `SelectObjectContentResult` 只承诺保留原始响应，不承诺当前已经完成事件流 typed 解码。

## 5. 验证

阶段 27 至少验证：

- `S3XmlTest`：ACL XML 解析、Select 请求 XML 生成。
- `ReactiveMinioSpecializedClientsTest`：ACL/Select typed 方法暴露、请求 query/header 正确、advanced 方法已迁移为 deprecated。
- `scripts/report-capability-matrix.py`：S3 product-typed 从 67 提升到 72。
- 双分支单元测试、真实 MinIO smoke、route parity、crypto gate、secret scan。
