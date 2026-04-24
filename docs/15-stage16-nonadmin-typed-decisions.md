# 15 阶段 16 非 Admin typed 收口决策

阶段 16 的目标不是把所有低频接口都强行解析成 Java 类，而是把用户高频、结构稳定、不会隐藏风险的能力产品化；对协议复杂或强环境依赖的接口，保留 advanced/raw 并写清原因。

## S3 typed 增量

| 能力 | 新入口 | 返回模型 | live 策略 |
| --- | --- | --- | --- |
| 对象版本分页 | `listObjectVersionsPage(...)`、`listObjectVersions(...)` | `ListObjectVersionsResult`、`ObjectVersionInfo`、`DeleteMarkerInfo` | 默认单元/XML fixture；共享 live 不默认启用 bucket versioning，避免版本残留导致 bucket 清理失败。 |
| 未完成分片上传列表 | `listMultipartUploadsPage(...)`、`listMultipartUploads(...)` | `ListMultipartUploadsResult`、`MultipartUploadInfo` | 安全 live：创建临时 multipart upload、查询、完成或 abort 后清理。 |

`listObjectVersions(...)` 只展开真实对象版本；删除标记通过分页结果 `deleteMarkers()` 获取，避免业务遍历对象版本时误把删除标记当作可读对象。

## STS 决策表

| 路由 | 当前策略 | 原因 |
| --- | --- | --- |
| `STS_ASSUME_ROLE_FORM` | 新增 typed：`assumeRoleCredentials(...)` | 这是标准签名凭证换临时凭证流程，请求体稳定，可用 `AssumeRoleRequest` 表达。 |
| `STS_ASSUME_ROLE_WITH_WEB_IDENTITY` | 已有 typed | WebIdentity token 是常见 OIDC 集成主路径。 |
| `STS_ASSUME_ROLE_WITH_CLIENT_GRANTS` | 已有 typed | ClientGrants token 是 MinIO 支持的 OAuth2 扩展主路径。 |
| `STS_ASSUME_ROLE_WITH_LDAP_IDENTITY` | 已有 typed | LDAP 用户名密码结构稳定。 |
| `STS_ASSUME_ROLE_WITH_CERTIFICATE` | 暂保留 advanced | 需要 TLS 双向证书和服务端 STS TLS 配置，普通单元/live 无法代表真实安全边界。 |
| `STS_ASSUME_ROLE_WITH_CUSTOM_TOKEN` | 暂保留 advanced | 依赖外部 Identity Management Plugin 和 RoleArn/Token 策略，后续需要独立环境 fixture。 |
| `STS_ASSUME_ROLE_SSO_FORM` | 暂保留 advanced | 服务端会按参数回落到 LDAP/WebIdentity 等实现，适合作为兼容入口保留。 |

## Metrics 决策

Metrics 返回 Prometheus 文本协议。SDK 继续提供 `PrometheusMetrics` 和 `PrometheusMetricSample`：

- 解析 metric 名称、label 和 value，满足告警、筛选和测试断言。
- 不把每个 MinIO 指标名都生成 Java 字段，因为指标集合会随 MinIO 版本、配置和子系统变化。
- 原始文本继续通过 `rawText()` 保留，便于调用方接入 Micrometer、Prometheus Java parser 或自有管道。

## Health 决策

Health 同时保留两类入口：

- 业务判断：`isLive()`、`isReady()`、`checkCluster()` 等返回 boolean/状态对象。
- 路由级状态：`liveGet()`、`readyHead()`、`clusterReadGet()` 等返回 HTTP status，便于网关或探针检查。

这种拆分避免把 GET/HEAD 细节暴露给普通业务，又能在运维场景保留精确状态码。

## 后续留口

- object retention / legal hold：已有 advanced 兼容入口，后续在 object lock live 条件明确后再产品化请求/响应模型。
- object attributes / select / restore：响应和流式语义复杂，阶段 16 只保留 advanced，并在发布说明里明确不是 raw fallback 缺口。
