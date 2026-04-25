# 阶段 88：补齐 getPresignedPostFormData 表单上传入口

## 目标

阶段 87 后，对象存储核心 API 仍缺少 `getPresignedPostFormData`。该能力用于浏览器或非 SDK 客户端通过 HTML multipart form 直接上传对象，是 minio-java 中非常典型的迁移入口。

## 新增模型与入口

- `PostPolicy`：响应式 SDK 自有的预签名 POST 策略模型，支持：
  - bucket 与过期时间；
  - equals 条件；
  - starts-with 条件；
  - content-length-range 条件；
  - 保留字段保护，禁止调用方手动设置 `x-amz-signature` 等 SDK 自动生成字段。
- `ReactiveMinioClient.getPresignedPostFormData(PostPolicy policy)`：返回浏览器表单上传需要的 form-data 字段。
- `S3RequestSigner.presignedPostFormData(...)`：复用 SigV4 signing key 推导流程，签名 Base64 policy。

## 与 minio-java 的对齐点

- 匿名客户端不能生成 presigned POST form-data。
- policy 必须包含 `key` 条件。
- form-data 包含：
  - `x-amz-algorithm`
  - `x-amz-credential`
  - `x-amz-security-token`（临时凭证存在时）
  - `x-amz-date`
  - `policy`
  - `x-amz-signature`
- policy JSON 会把 bucket、key、content-type、大小范围和签名相关字段都写入 conditions。

## 当前 minio-java 对标报告

阶段 88 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 精确同名：`55` 个。
- 缺失：`4` 个。
- 剩余缺失：`appendObject`、`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 验证

- 单元测试验证 form-data 字段、临时凭证 token、Base64 policy 内容、保留字段保护和匿名客户端拒绝。
- 双分支重新生成 minio-java 对标报告。
