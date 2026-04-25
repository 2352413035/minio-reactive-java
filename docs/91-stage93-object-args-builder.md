# 阶段 93：对象存储高频 Args builder 起步

## 目标

`minio-java` 大量公开方法都使用 `*Args` builder 承载请求参数。阶段 92 后，方法名已经收口，但响应式 SDK 仍缺少迁移友好的 Args 对象。本阶段先补最常用的对象存储 Args，让用户可以从“方法名迁移”继续过渡到“请求对象迁移”。

## 新增 Args 类

本阶段新增 22 个 `*Args` 类：

- 基类：`BaseArgs`、`BucketArgs`、`ObjectArgs`。
- bucket 常用入口：`ListBucketsArgs`、`BucketExistsArgs`、`MakeBucketArgs`、`RemoveBucketArgs`、`GetBucketLocationArgs`。
- 对象常用入口：`ListObjectsArgs`、`GetObjectArgs`、`PutObjectArgs`、`StatObjectArgs`、`RemoveObjectArgs`、`RemoveObjectsArgs`、`CopyObjectArgs`。
- 文件和高级对象入口：`DownloadObjectArgs`、`UploadObjectArgs`、`AppendObjectArgs`、`ComposeObjectArgs`、`GetPresignedObjectUrlArgs`、`PutObjectFanOutArgs`、`UploadSnowballObjectsArgs`。

## 客户端重载

`ReactiveMinioClient` 新增 Args 重载，例如：

```java
client.putObject(
    PutObjectArgs.builder()
        .bucket("bucket1")
        .object("hello.txt")
        .content("中文内容")
        .contentType("text/plain")
        .build());
```

这些重载继续委托既有强类型方法，底层签名、HTTP、XML 与文件读写流程不分叉。

## 设计边界

- 本阶段不是一次性复制 `minio-java` 的完整 Args 继承树，而是先覆盖对象存储高频路径。
- Builder 会在构造时做基础中文校验，例如 bucket/object/filename/objects 不能为空。
- `GetObjectArgs` 当前范围读取要求同时提供 `offset` 和 `length`，避免构造出当前底层方法尚未表达的开放区间请求。
- 大文件分片上传、bucket 子资源、对象治理等更多 Args 将在后续阶段继续补齐。

## 当前 minio-java 对标报告

阶段 93 后重新生成报告：

- 对象存储核心 API：`59 / 59` 精确同名。
- Admin 核心 API：`24 / 24` 精确同名。
- `*Args` builder：从 `0 / 86` 提升到 `22 / 86`。
- credentials provider：仍需要补环境变量、链式和配置 provider 迁移体验。

## 验证

- 新增 `ReactiveMinioSpecializedClientsTest.shouldBuildStage93ObjectArgsRequests`，覆盖 Args builder 构造、请求 path/header 传递、预签名 URL 和中文校验。
- 双分支聚焦测试通过。
- 双分支重新生成 minio-java 对标报告，确认 Args 计数提升。
