# 阶段 89：补齐 appendObject 追加写入入口

## 目标

阶段 88 后，对象存储核心 API 仍缺少 `appendObject`。该能力是 MinIO 扩展写入接口，不是标准 S3 putObject 的简单别名：必须先知道当前对象长度，再用 `x-amz-write-offset-bytes` 指定追加偏移。

## 新增能力

- `ObjectWriteResult`：对象写入类操作的轻量结果，保留响应头并提取 ETag、versionId。
- `ReactiveMinioClient.appendObject(bucket, object, byte[] content, contentType)`：追加字节数组。
- `ReactiveMinioClient.appendObject(bucket, object, String content, contentType)`：追加 UTF-8 字符串内容。
- `ReactiveMinioClient.appendObject(bucket, object, Path filename, contentType)`：读取本地普通文件并追加。

## 实现流程

1. `statObject` 发送 HEAD 请求，读取当前对象 `Content-Length`。
2. 发送 PUT 到同一个 bucket/object。
3. PUT 请求携带：
   - `x-amz-write-offset-bytes: 当前对象长度`
   - `Content-Length: 本次追加字节数`
   - 调用方指定或自动推断的 contentType。
4. 返回 `ObjectWriteResult`，让调用方读取 ETag、versionId 或完整响应头。

## 边界

本阶段实现的是最小可用强类型追加入口。minio-java 对文件/流会进一步按 chunkSize 切片并处理 checksum，本 SDK 后续需要继续补齐大对象分块追加和 checksum 语义。

## 当前 minio-java 对标报告

阶段 89 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 精确同名：`56` 个。
- 缺失：`3` 个。
- 剩余缺失：`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 验证

- 单元测试验证 HEAD + PUT 调用顺序、`x-amz-write-offset-bytes` 偏移、contentType、ETag 和 versionId。
- 双分支重新生成 minio-java 对标报告。
