# 阶段 96：credentials provider 类名覆盖收口

## 目标

阶段 95 后，`minio-java` 的对象 API、Admin API 与 `*Args` 类名已经收口。阶段 96 继续补齐官方 Java SDK 的 credentials provider 体系，让用户迁移时能找到熟悉的 provider 名称，并能把这些 provider 桥接到响应式客户端。

## 新增或增强内容

本阶段在 `io.minio.reactive.credentials` 下补齐以下同名类：

- `Credentials`：minio-java 风格凭证对象，可转换为 `ReactiveCredentials`，`toString()` 默认脱敏。
- `Provider`：minio-java 风格同步 provider 接口，可通过 `asReactiveProvider()` 桥接到响应式客户端。
- `StaticProvider`、`ChainedProvider`：静态凭证与链式回退。
- `EnvironmentProvider`、`MinioEnvironmentProvider`、`AwsEnvironmentProvider`：读取系统属性/环境变量中的 MinIO 或 AWS 凭证。
- `AwsConfigProvider`：读取 AWS credentials INI 文件。
- `MinioClientConfigProvider`：读取 mc / MinIO 客户端 `config.json`。
- `Jwt`、`BaseIdentityProvider`、`AssumeRoleProvider`、`WebIdentityClientGrantsProvider`、`WebIdentityProvider`、`ClientGrantsProvider`、`LdapIdentityProvider`、`CertificateIdentityProvider`：STS/identity provider 的响应式迁移边界。
- `IamAwsProvider`：先提供安全失败边界，不自动访问 IMDS/ECS 元数据服务。

同时，`ReactiveCredentialsProvider.from(Provider)` 允许把 minio-java 风格 provider 直接桥接到现有响应式客户端 builder。

## 设计边界

- provider 类名已经同名收口，但这不等于所有外部身份流都已经在 provider 内部重新实现。
- STS/OIDC/LDAP/证书类 provider 当前定位是响应式边界：真实 STS HTTP 交换继续优先使用 `ReactiveMinioStsClient`，然后把返回的临时凭证接入 provider。
- `IamAwsProvider` 不会默认访问 `169.254.169.254` 等元数据地址，避免在用户环境中产生隐式网络访问和 SSRF 风险。
- 配置文件读取测试只使用临时文件和假凭证，不写入真实凭证，也不在文档中输出 secret。

## 当前 minio-java 对标报告

阶段 96 后重新生成报告：

- 对象存储核心 API：`59 / 59` 精确同名。
- Admin 核心 API：`24 / 24` 精确同名。
- `*Args` builder：`86 / 86` 同名收口。
- credentials provider 类名：缺失 `0` 个。

## 验证

- 新增 `ReactiveCredentialsProvidersTest`，覆盖静态 provider、响应式桥接、环境变量读取、链式回退、AWS INI 配置、MinIO config.json、identity provider 边界和 IAM 安全失败。
- 双分支重新生成 minio-java 对标报告，确认 credentials missingByName 为 `无`。
- 双分支全量测试与 JDK21/JDK25 编译验证通过。
