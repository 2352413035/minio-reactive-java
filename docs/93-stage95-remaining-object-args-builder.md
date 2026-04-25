# 阶段 95：剩余对象与分片 Args builder 收口

## 目标

阶段 94 后，`*Args` builder 对标达到 48 / 86。剩余部分主要集中在对象治理、分片上传、PromptObject、SelectObjectContent、通知监听，以及 minio-java 内部继承体系的基础 Args。本阶段把这些 Args 名称补齐，并为可调用入口接上 `ReactiveMinioClient` 现有强类型方法。

## 新增 Args 类

本阶段新增剩余 38 个 `*Args` 类，覆盖：

- 对象治理：对象标签、对象保留策略、Legal Hold、对象属性、对象 ACL、恢复归档对象、SelectObjectContent。
- 分片上传：创建 upload、上传 part、copy part、列 part、完成 upload、终止 upload、列 multipart uploads。
- 通知与 Prompt：`ListenBucketNotificationArgs`、`PromptObjectArgs`。
- minio-java 基础继承体系：`CreateBucketBaseArgs`、`HeadBucketBaseArgs`、`HeadObjectBaseArgs`、`ObjectVersionArgs`、`ObjectReadArgs`、`ObjectConditionalReadArgs`、`ObjectWriteArgs`、`PutObjectAPIBaseArgs`、`PutObjectAPIArgs`、`PutObjectBaseArgs`。
- 迁移别名：`CreateBucketArgs`、`HeadBucketArgs`、`HeadObjectArgs`、`DeleteObjectsArgs`、`ListObjectsV1Args`、`ListObjectsV2Args`。

## 客户端重载

可执行 Args 均新增 `ReactiveMinioClient` 重载，并复用既有强类型方法。例如：

```java
client.uploadPart(
    UploadPartArgs.builder()
        .bucket("bucket1")
        .object("large.bin")
        .uploadId("upload-1")
        .partNumber(1)
        .content(bytes)
        .build());
```

基础继承体系类只作为迁移兼容的父类名称存在，不直接伪装成完整业务入口。

## 当前 minio-java 对标报告

阶段 95 后重新生成报告：

- 对象存储核心 API：`59 / 59` 精确同名。
- Admin 核心 API：`24 / 24` 精确同名。
- `*Args` builder：`86 / 86` 同名收口。
- credentials provider：当前仍只有响应式基础 provider 和 STS 刷新 provider，后续阶段继续补官方 Java SDK 的 provider 迁移体验。

## 验证

- 新增 `shouldBuildStage95RemainingObjectArgsRequests`，覆盖 create/head、对象标签、Legal Hold、Prompt、通知监听、uploadPart、abortMultipartUpload 等 Args 请求构造。
- 双分支重新生成 minio-java 对标报告，确认 Args 无缺失。
- 双分支全量测试和 JDK21/JDK25 编译验证通过。
