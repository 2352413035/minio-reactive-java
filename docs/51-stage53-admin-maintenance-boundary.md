# 阶段 53：Admin 维护操作产品边界

## 1. 本阶段目标

阶段 53 将 heal、pool decommission、rebalance 这类维护操作从 `Mono<String>` advanced 入口补充为明确的 `AdminTextResult` 产品入口。这样用户不用直接记忆 MinIO Admin endpoint 名称，也能从方法名看出这些操作属于维护窗口能力。

## 2. 新增产品方法

| 方法 | 对应 MinIO Admin 路由 | 返回 | 风险说明 |
| --- | --- | --- | --- |
| `startRootHeal(...)` | `ADMIN_HEAL_ROOT` | `AdminTextResult` | 可能触发集群级 heal，建议维护窗口执行。 |
| `startBucketHeal(...)` | `ADMIN_HEAL_BUCKET` | `AdminTextResult` | 可能触发 bucket 级 heal，建议限制目标 bucket。 |
| `startPrefixHeal(...)` | `ADMIN_HEAL_PREFIX` | `AdminTextResult` | 可能触发 prefix 级 heal，prefix 不能为空。 |
| `startPoolDecommission(...)` | `ADMIN_START_DECOMMISSION` | `AdminTextResult` | 会改变 pool 维护状态，不在共享 live 中真实执行。 |
| `cancelPoolDecommission(...)` | `ADMIN_CANCEL_DECOMMISSION` | `AdminTextResult` | 需要调用方确认当前 decommission 状态。 |
| `startRebalance(...)` | `ADMIN_REBALANCE_START` | `AdminTextResult` | 可能消耗集群资源；状态读取继续使用 `getRebalanceStatus()`。 |
| `stopRebalance(...)` | `ADMIN_REBALANCE_STOP` | `AdminTextResult` | 需要调用方先确认当前 rebalance 状态。 |

## 3. 为什么不在共享 live 中真实执行

这些接口虽然不是删除 bucket、修改 IAM、变更配置那类强破坏性写入，但它们会改变维护任务状态或消耗集群资源。共享 MinIO 环境只能用于普通 live smoke；维护操作必须在独立 lab 或明确维护窗口中执行。

因此本阶段验证策略是：

1. mock WebClient 断言专用 typed 方法请求到了正确路径和 query。
2. raw catalog 对同一 endpoint 执行一次交叉验证，证明 raw 兜底仍可调用。
3. live 集成测试不执行 heal、decommission、rebalance 的真实动作。
4. capability matrix 只提升产品 typed 成熟度，不减少 `destructive-blocked`。

## 4. 当前能力矩阵变化

- Admin product-typed：81 / 128 -> 88 / 128。
- `raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 保持不变。
- `destructive-blocked = 29` 保持不变。

## 5. 后续建议

下一阶段优先继续处理非 Crypto Gate 的 Admin 缺口，例如 replication MRF、tier verify、policy attach/detach 的语义化入口；如果触及配置删除、服务控制或 token 吊销，仍必须保留高风险文档和独立 lab 门禁。
