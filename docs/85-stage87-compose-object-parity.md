# 阶段 87：补齐 composeObject 对象组合入口

## 目标

阶段 86 后，minio-java 对象存储核心 API 仍缺少 `composeObject` 等 6 个同名入口。阶段 87 先补齐对象组合能力，因为它可以复用标准 S3 multipart copy 协议，并且能明确体现“强类型客户端不是 raw 包装”的设计原则。

## 新增模型与入口

- `ComposeSource`：描述源 bucket、源 object、可选 versionId 和可选字节范围。
- `ReactiveMinioClient.uploadPartCopy(...)`：multipart copy 的底层强类型步骤，负责生成 `x-amz-copy-source` 和可选 `x-amz-copy-source-range`。
- `ReactiveMinioClient.composeObject(bucket, object, List<ComposeSource>, contentType)`：多源对象组合入口。
- `ReactiveMinioClient.composeObject(bucket, object, List<ComposeSource>)`：使用服务端默认 contentType 的多源入口。
- `ReactiveMinioClient.composeObject(bucket, object, ComposeSource...)`：少量源对象的变参入口。
- `ReactiveMinioClient.composeObject(bucket, object, sourceBucket, sourceObject)`：单源便捷入口。

## 实现流程

1. `createMultipartUpload` 创建目标对象的 multipart upload。
2. 逐个源对象调用 `uploadPartCopy`。
3. 将每个 copy part 响应中的 ETag 转成 `CompletePart`。
4. 调用 `completeMultipartUpload` 完成组合。
5. 任一 copy/complete 步骤失败时，自动调用 `abortMultipartUpload` 清理 uploadId。

这条链路和已有 `uploadMultipartObject` 一样遵循 SDK 内部的强类型 S3 请求流程：请求模型、签名、HTTP、XML 解析各自清晰，不走 `ReactiveMinioRawClient`。

## 当前 minio-java 对标报告

阶段 87 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 精确同名：`54` 个。
- 缺失：`5` 个。
- 剩余缺失：`appendObject`、`getPresignedPostFormData`、`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 验证

- 单元测试覆盖 create multipart、upload part copy、complete multipart 的请求路径、查询参数、copy-source 头、range 头和 contentType。
- 单元测试覆盖空源列表的中文错误。
- 双分支重新生成 minio-java 对标报告。
