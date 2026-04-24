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

阶段 17 起，仓库提供 `scripts/minio-lab/run-destructive-tests.sh` 作为 destructive lab 的统一入口。真实 config write + restore 测试还要求 `MINIO_LAB_TEST_CONFIG_KV` 和 `MINIO_LAB_RESTORE_CONFIG_KV`，否则只校验 lab 门禁并跳过写入。

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

## 发布口径

阶段 19 起，Admin 不再用“接口数量完成百分比”描述进度，而是拆成以下几类：

- `product-typed`：已经有明确请求/响应模型、中文错误语义或风险说明，适合用户直接集成。
- `advanced-compatible`：已保留可调用入口，但还没有足够产品化的 typed 模型。
- `encrypted-blocked`：服务端默认返回 madmin 加密载荷，Crypto Gate Pass 前只暴露 `EncryptedAdminResponse`。
- `destructive-blocked`：会修改服务端状态或需要独立实验环境的操作，只能通过 destructive lab 验证。

当前 Admin 口径以 `.omx/reports/capability-matrix.md` 为准：route-catalog 128，product-typed 43，advanced-compatible 128，encrypted-blocked 9，destructive-blocked 29。

阶段 22 起，pool、rebalance、tier、site replication、top locks、OBD、health info 等只读状态接口先进入 `AdminJsonResult` typed 入口。它们仍保留完整原始 JSON，后续确认稳定字段后再拆成更细摘要模型。
