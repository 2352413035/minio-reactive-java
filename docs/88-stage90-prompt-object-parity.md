# 阶段 90：补齐 promptObject 推理请求入口

## 目标

阶段 89 后，对象存储核心 API 仍缺少 `promptObject`。该能力是 MinIO 对象 Lambda/推理类扩展入口：把目标对象作为上下文，向 `lambdaArn` 指向的处理器提交 prompt 与附加参数。

## 新增入口

- `ReactiveMinioClient.promptObject(bucket, object, lambdaArn, prompt)`：最小参数入口。
- `ReactiveMinioClient.promptObject(bucket, object, lambdaArn, prompt, promptArgs)`：带自定义 prompt 参数入口。
- `ReactiveMinioClient.promptObject(bucket, object, versionId, lambdaArn, prompt, promptArgs, headers)`：带 versionId 和自定义请求头的完整入口。

## 实现流程

1. 使用 POST 访问目标 bucket/object。
2. query 中写入 `lambdaArn`，可选写入 `versionId`。
3. 请求体为 JSON，包含调用方传入的 promptArgs，并强制写入 `prompt` 字段。
4. contentType 为 `application/json`。
5. 响应可能是流式推理结果，所以返回 `Flux<byte[]>`。

## 边界

本阶段实现强类型请求流程和 mock 验证，不假设当前 MinIO 测试环境一定启用了对象 Lambda/推理后端。真实推理服务、lambdaArn 配置和长流式响应需要后续 live/lab 补证。

## 当前 minio-java 对标报告

阶段 90 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 精确同名：`57` 个。
- 缺失：`2` 个。
- 剩余缺失：`putObjectFanOut`、`uploadSnowballObjects`。

## 验证

- 单元测试验证 POST 路径、`lambdaArn`/`versionId` query、contentType、自定义请求头、响应式字节流和空 prompt 中文错误。
- 双分支重新生成 minio-java 对标报告。
