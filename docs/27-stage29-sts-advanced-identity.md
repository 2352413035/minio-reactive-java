# 27 阶段 29 STS 高级身份源 typed 请求模型

## 1. 目标

阶段 29 把 STS 剩余高级身份源从 advanced-compatible 推进到 typed 主路径。重点不是伪造真实身份源环境，而是让请求模型、参数校验和错误边界清晰可用。

## 2. 新增请求模型

- `AssumeRoleSsoRequest`：SSO 表单入口，支持 WebIdentity / ClientGrants action、token、RoleArn 和 DurationSeconds。
- `AssumeRoleWithCertificateRequest`：客户端证书入口，支持 DurationSeconds；真实证书由调用方配置到 `WebClient` TLS 层。
- `AssumeRoleWithCustomTokenRequest`：自定义身份插件 token 入口，支持 Token、RoleArn 和 DurationSeconds。

## 3. 新增方法

| 方法 | 说明 |
| --- | --- |
| `assumeRoleSsoCredentials(request)` | 使用 SSO 表单请求换取 `AssumeRoleResult`。 |
| `assumeRoleWithCertificateCredentials(request)` | 使用客户端证书身份源换取 `AssumeRoleResult`。 |
| `assumeRoleWithCustomTokenCredentials(request)` | 使用自定义身份插件 token 换取 `AssumeRoleResult`。 |

对应 advanced 方法 `assumeRoleSsoForm`、`assumeRoleWithCertificate`、`assumeRoleWithCustomToken` 已标记为 `@Deprecated`，保留兼容迁移入口。

## 4. 环境边界

- WebIdentity、ClientGrants、LDAP 仍可在有对应身份源时真实验证。
- SSO、自定义 token、证书依赖外部身份源或 TLS 客户端证书配置；共享 MinIO 环境不把这些身份源当成默认 live 完成门禁。
- 证书入口不会在 SDK 内部生成或持有私钥，调用方必须在 `WebClient`/Reactor Netty 层配置 TLS 证书。

## 5. 验证

- `ReactiveMinioSpecializedClientsTest` 覆盖方法暴露、请求参数拼装、返回 XML 解析和参数校验。
- `scripts/report-capability-matrix.py` 显示 STS product-typed 从 4 / 7 提升到 7 / 7。
- 双分支继续执行单元测试、真实 MinIO smoke、route parity、crypto gate、JDK21/JDK25 compile 和 secret scan。
