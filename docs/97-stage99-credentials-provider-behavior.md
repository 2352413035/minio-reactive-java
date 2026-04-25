# 阶段 99：credentials provider 行为细化

## 目标

阶段 99 继续对照 `minio-java` 的 credentials provider 行为，不再只看类名是否存在，而是补齐两个会影响用户迁移体验的细节：AWS 环境变量空字符串处理，以及 OIDC/JWT JSON 字段名兼容。

## AWS 环境变量空字符串语义

`AwsEnvironmentProvider` 现在按 minio-java 的语义处理主变量和次级变量：

- `AWS_ACCESS_KEY_ID` 存在但为空时，直接抛出中文 `ProviderException`，不会悄悄回退到 `AWS_ACCESS_KEY`。
- `AWS_SECRET_ACCESS_KEY` 存在但为空时，直接抛出中文 `ProviderException`，不会悄悄回退到 `AWS_SECRET_KEY`。
- 变量不存在时仍按原顺序回退到次级变量。
- 报错只说明哪个变量为空，不输出 access key、secret key 或 session token。

这样可以避免部署环境里主变量被错误写成空字符串时，SDK 静默使用另一组旧凭证，导致排障困难。

## JWT JSON 字段名兼容

`Jwt` 新增 Jackson 构造和字段注解：

- `access_token` -> `token`
- `expires_in` -> `expiry`

用户从 OIDC/ClientGrants 响应 JSON 反序列化 `Jwt` 时，可以继续使用 minio-java 风格字段名，不需要手写中间 DTO。

## 验证

- `ReactiveCredentialsProvidersTest.shouldRejectEmptyAwsPrimaryEnvironmentValue`
- `ReactiveCredentialsProvidersTest.shouldDeserializeJwtWithMinioJavaFieldNames`
- 双分支聚焦 credentials provider 测试。
- 双分支全量测试。
- JDK21/JDK25 `test-compile`。

## 边界

- 本阶段不启用 `IamAwsProvider` 的自动元数据服务访问；该功能仍需要单独设计 SSRF 防护、超时、代理和网络边界。
- 本阶段不改变 Crypto Gate 和破坏性操作门禁。
- 本阶段不写入真实凭证。
