# 阶段 98：provider 行为深化与 Crypto Gate 回归

## 目标

阶段 97 后，SDK 已经在静态对标口径上完成对象 API、Admin API、`*Args` 和 credentials provider 类名收口。阶段 98 不再新增重复名称，而是深化 provider 行为，并重新核对 Crypto Gate 风险边界没有被误降级。

## provider 行为深化

本阶段为 STS 身份类 provider 增加 `ReactiveMinioStsClient` 桥接工厂：

- `AssumeRoleProvider.fromStsClient(...)`
- `WebIdentityProvider.fromStsClient(...)`
- `ClientGrantsProvider.fromStsClient(...)`
- `LdapIdentityProvider.fromStsClient(...)`
- `CertificateIdentityProvider.fromStsClient(...)`

这些工厂会调用现有 `ReactiveMinioStsClient` 强类型方法换取临时凭证，再缓存到 provider 内部。这样用户既可以沿用 minio-java 风格 provider 名称，又不会在 provider 内部复制一套阻塞 HTTP 客户端。

## Crypto Gate 回归结论

重新生成能力矩阵后，Admin 风险边界仍保持：

- `encrypted-blocked = 11`
- `destructive-blocked = 29`
- `raw-fallback = 0`
- `Admin product-typed = 128 / 128`

这说明阶段 96/97/98 的 provider 工作没有把加密 Admin 响应伪装成明文，也没有降低破坏性操作门禁。

## 安全边界

- `IamAwsProvider` 仍不默认访问 IAM/IMDS/ECS 元数据地址。
- identity provider 需要真实网络换取凭证时，必须显式传入 `ReactiveMinioStsClient`。
- 测试只使用 mock STS XML，不使用真实凭证，不写入真实配置。
- Crypto Gate 未通过前，`EncryptedAdminResponse` 仍是加密 Admin 响应的公开边界。

## 验证

- `ReactiveCredentialsProvidersTest.shouldFetchIdentityCredentialsThroughReactiveStsClient` 覆盖 STS 桥接工厂。
- 重新生成 `capability-matrix-jdk8/jdk17`，确认 Admin `encrypted-blocked = 11`、`destructive-blocked = 29`。
- 双分支全量测试和 JDK21/JDK25 编译验证通过。
