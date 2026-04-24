# 13 Admin 接口风险分级

MinIO Admin API 覆盖用户、策略、配置、站点复制、tier、批处理、性能测试等能力。SDK 不能把所有接口都包装成“低成本便捷方法”，否则会让调用方误触发高风险操作。

## 风险等级

| 等级 | 类型 | 示例 | SDK 策略 | 测试策略 |
| --- | --- | --- | --- | --- |
| L1 | 安全只读 | server info、storage info、account info、list/info 类 | 优先提供 typed 方法 | 可默认单元测试和安全 live 测试 |
| L2 | 可回滚写 | add/delete user、policy、group、service account | 提供 typed request/response，必须清楚命名 | live 测试要创建临时资源并清理 |
| L3 | 需要隔离环境 | set config、IDP、bucket quota、remote target、replication、tier、batch job | 需要显式 request 对象和中文风险注释 | 默认跳过，只在 `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true` 时运行 |
| L4 | 不默认便捷化 | server update、service restart/stop、site replication destructive op、force unlock、speedtest | 保留 advanced/raw，除非有明确需求 | 不在共享环境默认执行 |

## 当前配置写接口边界

`setConfigKvText(...)` 和 `setConfigText(...)` 已经能生成 madmin 兼容加密请求体，但它们会修改服务端配置。当前集成测试不会默认执行真实配置写入。后续如果要测试这些接口，必须满足：

1. 使用独立、可回滚的 MinIO 环境。
2. 测试前读取原配置。
3. 写入测试配置后立即恢复原配置。
4. 测试失败也要执行恢复逻辑。
5. 通过 `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true` 显式开启。
6. 执行前运行 `scripts/minio-lab/verify-env.sh`。

## 阶段 15 已产品化的明文安全入口

阶段 15 的原则是：只把 MinIO 服务端明文返回、且不会修改共享环境的能力做成用户日常可用的 typed 入口；需要加密解密或会修改状态的能力继续保持边界。

| 方法 | 对应 MinIO 路由 | 风险等级 | 共享 live | 说明 |
| --- | --- | --- | --- | --- |
| `getConfigHelp(subSys[, key])` | `ADMIN_HELP_CONFIG_KV` | L1 | 允许 | 只读取配置帮助和字段说明，不返回真实配置值。 |
| `getStorageSummary()` | `ADMIN_STORAGE_INFO` | L1 | 允许 | 提取磁盘数、在线/离线磁盘数、修复中磁盘数，同时保留完整 JSON。 |
| `getDataUsageSummary()` | `ADMIN_DATA_USAGE_INFO` | L1 | 允许 | 提取对象数、桶数和容量字段，同时保留完整 JSON。 |
| `getAccountSummary()` | `ADMIN_ACCOUNT_INFO` | L1 | 允许 | 提取账号名、bucket 数、可读写 bucket 数和策略原文。 |
| `getBucketQuotaInfo(bucket)` | `ADMIN_GET_BUCKET_QUOTA` | L3 只读候选 | 默认不跑 | 只读查询，但依赖 bucket quota 配置和环境权限；普通 shared live 不把它作为完成门禁。 |
| `listTiers()` | `ADMIN_LIST_TIER` | L3 只读候选 | 默认不跑 | 只提取 tier 名称/类型/版本，不暴露凭据字段。 |

## 加密响应边界

对照 MinIO 服务端实现后确认，以下接口返回的是 madmin 加密响应，当前不能把它们当成明文 JSON 模型：

- `ADMIN_GET_CONFIG`
- `ADMIN_GET_CONFIG_KV`
- `ADMIN_LIST_CONFIG_HISTORY_KV`
- `ADMIN_LIST_USERS`
- `ADMIN_ADD_SERVICE_ACCOUNT`
- `ADMIN_INFO_SERVICE_ACCOUNT`
- `ADMIN_LIST_SERVICE_ACCOUNTS`
- `ADMIN_INFO_ACCESS_KEY`
- `ADMIN_LIST_ACCESS_KEYS_BULK`

因此能力矩阵里的 `encrypted-blocked` 当前为 9。SDK 只提供 `EncryptedAdminResponse` 边界方法（例如 `getConfigEncrypted()`、`getConfigKvEncrypted(...)`、`listConfigHistoryKvEncrypted(...)`、`getAccessKeyInfoEncrypted(...)`、`listAccessKeysEncrypted(...)`），不会在 Crypto Gate Pass 前伪装成已解密 typed 结果。

## 使用建议

- 业务项目优先使用 L1/L2 typed 方法。
- L3/L4 场景应先在测试环境验证，再迁移到生产。
- 如果 SDK 尚未提供 typed 方法，可以使用 `ReactiveMinioRawClient` 兜底，但调用方需要自行承担协议和风险确认。
