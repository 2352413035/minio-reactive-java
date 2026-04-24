# 20 阶段 22：Admin 只读 JSON 摘要 typed 深化

## 1. 背景

阶段 22 的目标是继续提升 Admin 客户端的用户友好程度，但不突破既有风险边界。Admin API 中大量接口返回 JSON，字段随 MinIO 版本和部署形态变化较大；在尚未稳定沉淀细粒度模型前，SDK 采用“通用 JSON 结果 + 原始 JSON”的方式提供 typed 主路径。

这不是 raw 薄包装：调用方不再需要记忆 catalog 名称，也不需要处理字符串响应；SDK 会统一解析 JSON，并为未来继续抽取稳定字段保留扩展空间。

## 2. 本阶段新增 typed 入口

| 方法 | 对应路由 | 风险口径 | 说明 |
| --- | --- | --- | --- |
| `getBackgroundHealStatus()` | `ADMIN_BACKGROUND_HEAL_STATUS` | L1/L2 只读状态 | 返回 `AdminJsonResult`。 |
| `listPoolsInfo()` | `ADMIN_LIST_POOLS` | L1 只读 | 返回 pool 列表 JSON。 |
| `getPoolStatus(pool)` | `ADMIN_POOL_STATUS` | L1 只读 | 校验 pool 参数并返回 JSON。 |
| `getRebalanceStatus()` | `ADMIN_REBALANCE_STATUS` | L1 只读 | 只读取 rebalance 状态，不启动/停止 rebalance。 |
| `getTierStats()` | `ADMIN_TIER_STATS` | L1 只读 | 返回 tier 统计 JSON。 |
| `getSiteReplicationInfo()` | `ADMIN_SITE_REPLICATION_INFO` | L1 只读 | 只读站点复制信息。 |
| `getSiteReplicationStatus()` | `ADMIN_SITE_REPLICATION_STATUS` | L1 只读 | 只读站点复制状态。 |
| `getTopLocksInfo()` | `ADMIN_TOP_LOCKS` | L1 只读 | 只读锁热点信息。 |
| `getObdInfo()` | `ADMIN_OBD_INFO` | L1 只读 | 只读诊断信息。 |
| `getHealthInfo()` | `ADMIN_HEALTH_INFO` | L1 只读 | 只读 Admin health info。 |

## 3. 保留边界

- `rebalanceStart()` / `rebalanceStop()`、`startDecommission()` / `cancelDecommission()`、service restart/update、speedtest 等仍属于高风险或破坏性入口，不进入共享环境 typed 完成口径。
- 本阶段只为读状态类接口补充 typed 主路径，不改变 destructive lab 门禁。
- 这些方法返回 `AdminJsonResult`，后续如果确认稳定字段，再追加专门摘要模型。

## 4. 验证要求

- `ReactiveMinioSpecializedClientsTest` 覆盖 path/query 构造、JSON 解析和参数校验。
- 能力矩阵 Admin `product-typed` 从 33 提升到 43。
- 双分支保持相同 API 与相同验证结果。
