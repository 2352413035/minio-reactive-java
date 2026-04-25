# 阶段 97：Provider 与客户端 builder 桥接

## 目标

阶段 96 已经补齐 minio-java 同名 credentials provider 类，但用户仍需要手动把 `Provider` 转成 `ReactiveCredentialsProvider`。阶段 97 把这个桥接下沉到所有客户端 builder，让迁移路径更直接。

## 新增能力

以下客户端 builder 现在都支持：

```java
Provider provider = new StaticProvider("accessKey", "secretKey");
ReactiveMinioClient client =
    ReactiveMinioClient.builder()
        .endpoint("http://localhost:9000")
        .region("us-east-1")
        .credentialsProvider(provider)
        .build();
```

覆盖客户端：

- `ReactiveMinioClient`
- `ReactiveMinioAdminClient`
- `ReactiveMinioRawClient`
- `ReactiveMinioKmsClient`
- `ReactiveMinioStsClient`
- `ReactiveMinioMetricsClient`
- `ReactiveMinioHealthClient`

## 设计边界

- `Provider` 仍是 minio-java 风格同步接口。
- builder 内部使用 `ReactiveCredentialsProvider.from(provider)` 桥接，客户端内部仍只依赖响应式 provider。
- 这只是接入体验增强，不改变签名流程、HTTP 流程，也不引入真实凭证到文档或测试。

## 验证

- `ReactiveCredentialsProvidersTest.shouldUseMinioJavaStyleProviderInAllClientBuilders` 覆盖所有 builder。
- 双分支聚焦测试、全量测试和 JDK21/JDK25 编译验证通过。
