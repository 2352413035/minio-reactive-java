# 19 阶段 21：S3 bucket 子资源 typed 能力

## 1. 背景

阶段 21 继续提升 `ReactiveMinioClient` 的产品化程度，把一批 bucket 子资源从 XML 字符串和 advanced 兼容入口推进到 typed 模型。目标是让业务调用方不需要记忆 query 名、XML 标签和 raw 响应结构。

## 2. 本阶段新增 typed 入口

| 能力 | typed 方法 | advanced 兼容入口 | 说明 |
| --- | --- | --- | --- |
| bucket CORS | `getBucketCorsConfiguration(...)` / `setBucketCorsConfiguration(...)` / `deleteBucketCorsConfiguration(...)` | `s3GetBucketCors(...)` / `s3PutBucketCors(...)` / `s3DeleteBucketCors(...)` | 使用 `BucketCorsConfiguration` 与 `BucketCorsRule` 表达规则列表。 |
| bucket website | `getBucketWebsiteConfiguration(...)` / `deleteBucketWebsiteConfiguration(...)` | `s3GetBucketWebsite(...)` / `s3DeleteBucketWebsite(...)` | 当前 MinIO router 暴露 GET/DELETE，未暴露 PUT website，因此 SDK 不伪造 set 方法。 |
| bucket logging | `getBucketLoggingConfiguration(...)` | `s3GetBucketLogging(...)` | 提取 target bucket 与 target prefix。 |
| bucket policy status | `getBucketPolicyStatus(...)` | `s3GetBucketPolicyStatus(...)` | 提取是否 public。 |
| bucket accelerate | `getBucketAccelerateConfiguration(...)` | `s3GetBucketAccelerate(...)` | 提取 Enabled/Suspended 状态。 |
| request payment | `getBucketRequestPaymentConfiguration(...)` | `s3GetBucketRequestPayment(...)` | 提取 Payer，并提供 requester pays 判断。 |

advanced 兼容入口继续保留，但对应方法标记为 `@Deprecated`，提醒用户优先使用 typed 入口。

## 3. 设计边界

- CORS 写入是可回滚 bucket 子资源操作，live 测试会尝试 set/get/delete；如果服务端返回能力或参数边界错误，测试会明确区分它不是 SDK 请求链路错误。
- Website 当前没有 `setBucketWebsiteConfiguration(...)`，因为本地 MinIO router 未登记 PUT website route。SDK 不应凭空制造服务端没有公开的接口。
- logging、policy status、accelerate、request payment 多数场景是只读摘要；模型保留原始 XML，后续可增量补字段。

## 4. 使用示例

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
BucketCorsConfiguration current = client.getBucketCorsConfiguration("bucket").block();
client.deleteBucketCorsConfiguration("bucket").block();

BucketPolicyStatus status = client.getBucketPolicyStatus("bucket").block();
```

## 5. 验证要求

- `S3XmlTest` 覆盖 CORS XML 和 bucket 子资源摘要解析。
- `ReactiveMinioSpecializedClientsTest` 覆盖 path/query 构造、模型解析和 deprecated 迁移标记。
- `LiveMinioIntegrationTest` 覆盖 bucket CORS set/get/delete 的真实可回滚链路。
- 能力矩阵 S3 `product-typed` 从 58 提升到 67，双分支保持一致。
