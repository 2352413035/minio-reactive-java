# 28 阶段 30 Admin L1/L2 摘要模型深化

## 1. 目标

阶段 30 继续把 Admin 中“只读或低风险、字段可能漂移”的接口从 advanced-compatible 推进到 typed 摘要入口。原则是：

1. 不绕过 encrypted-blocked 和 destructive-blocked。
2. 对字段稳定性不足的响应保留 raw JSON 和 Map。
3. 对用户最需要判断的数量、名称、状态、目标 ARN 等字段提供便捷 getter。

## 2. 新增模型

- `AdminPolicyEntities`：内置策略绑定实体摘要，统计用户、用户组、策略映射数量。
- `AdminIdpConfigList`：IDP 配置名称列表摘要，支持 openid/ldap 等类型。
- `AdminRemoteTargetList`：bucket remote target 只读摘要，暴露 ARN、类型、endpoint、安全开关，不暴露凭据字段。
- `AdminBatchJobList`：batch job 列表摘要，暴露任务 ID、类型、状态、用户。

## 3. 新增方法

| 方法 | 说明 |
| --- | --- |
| `listPolicyEntities()` | 读取内置策略绑定实体摘要。 |
| `listIdpConfigs(type)` | 列出指定类型 IDP 配置名称。 |
| `getIdpConfigInfo(type, name)` | 获取单个 IDP 配置的通用 JSON 包装。 |
| `listRemoteTargetsInfo(bucket, type)` | 列出 bucket remote target 摘要。 |
| `listBatchJobsInfo()` | 列出 batch job 摘要。 |
| `getBatchJobStatusInfo()` | 获取 batch job 状态 JSON 包装。 |
| `describeBatchJobInfo()` | 获取 batch job 详情 JSON 包装。 |

对应 advanced 方法 `listBuiltinPolicyEntities`、`listIdpConfig`、`getIdpConfig`、`listRemoteTargets`、`listBatchJobs`、`batchJobStatus`、`describeBatchJob` 已标记为 `@Deprecated`，保留迁移入口。

## 4. 风险边界

- `addIdpConfig` / `updateIdpConfig` / `deleteIdpConfig` 仍属于 L3 destructive，不进入共享 live 默认验证。
- `setRemoteTarget` / `removeRemoteTarget` 仍属于 L3 destructive。
- `startBatchJob` / `cancelBatchJob` 仍属于 L3 destructive。
- 当前阶段只把只读摘要做成 typed，不改变写操作门禁。

## 5. 验证

- `ReactiveMinioSpecializedClientsTest` 覆盖模型解析、方法暴露、请求路径/query、deprecated 迁移标记。
- `scripts/report-capability-matrix.py` 显示 Admin product-typed 从 43 / 128 提升到 50 / 128。
- 双分支继续通过单元测试、真实 MinIO smoke、route parity、crypto gate、JDK21/JDK25 compile 和 secret scan。
