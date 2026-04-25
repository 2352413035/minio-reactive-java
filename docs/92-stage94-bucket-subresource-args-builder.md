# 阶段 94：bucket 子资源 Args builder 补齐

## 目标

阶段 93 已覆盖对象存储高频 Args，但 bucket 子资源配置仍主要依赖直接参数方法。阶段 94 把 tags、CORS、policy、lifecycle、versioning、notification、encryption、object-lock、replication 这些常见 bucket 子资源迁移到 Args builder 入口。

## 新增 Args 类

本阶段新增 26 个 `*Args` 类：

- tags：`GetBucketTagsArgs`、`SetBucketTagsArgs`、`DeleteBucketTagsArgs`。
- CORS：`GetBucketCorsArgs`、`SetBucketCorsArgs`、`DeleteBucketCorsArgs`。
- policy：`GetBucketPolicyArgs`、`SetBucketPolicyArgs`、`DeleteBucketPolicyArgs`。
- lifecycle：`GetBucketLifecycleArgs`、`SetBucketLifecycleArgs`、`DeleteBucketLifecycleArgs`。
- versioning：`GetBucketVersioningArgs`、`SetBucketVersioningArgs`。
- notification：`GetBucketNotificationArgs`、`SetBucketNotificationArgs`、`DeleteBucketNotificationArgs`。
- encryption：`GetBucketEncryptionArgs`、`SetBucketEncryptionArgs`、`DeleteBucketEncryptionArgs`。
- object-lock：`GetObjectLockConfigurationArgs`、`SetObjectLockConfigurationArgs`、`DeleteObjectLockConfigurationArgs`。
- replication：`GetBucketReplicationArgs`、`SetBucketReplicationArgs`、`DeleteBucketReplicationArgs`。

## 客户端重载

`ReactiveMinioClient` 对这些子资源增加 Args 重载。set 类 Args 继续落到现有强类型或 XML/JSON 字符串方法上，例如：

```java
client.setBucketPolicy(
    SetBucketPolicyArgs.builder()
        .bucket("bucket1")
        .policyJson("{\"Version\":\"2012-10-17\"}")
        .build());
```

## 设计边界

- 本阶段不新增协议实现，只把已经存在的 bucket 子资源强类型方法补上迁移友好的请求对象。
- `SetBucketVersioningArgs` 与 `SetBucketNotificationArgs` 同时支持 XML 字符串和已有配置对象。
- 空 XML/JSON、空 tags、空 key/value 会在 builder 阶段给出中文异常。
- CORS、notification、versioning 的更完整 builder 语义留到后续“配置对象深水区”阶段继续补。

## 当前 minio-java 对标报告

阶段 94 后重新生成报告：

- 对象存储核心 API：`59 / 59` 精确同名。
- Admin 核心 API：`24 / 24` 精确同名。
- `*Args` builder：从 `22 / 86` 提升到 `48 / 86`。
- credentials provider：仍未补齐，是后续阶段重点。

## 验证

- 新增 `shouldBuildStage94BucketSubresourceArgsRequests`，覆盖 tags、policy、lifecycle、versioning、notification、encryption、object-lock、replication 的 query/contentType 构造和中文校验。
- 双分支聚焦测试、全量测试和 JDK21/JDK25 编译验证通过。
