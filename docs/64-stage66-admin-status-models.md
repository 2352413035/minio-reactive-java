# 阶段 66：Admin 状态类结果模型深化

## 1. 本阶段目标

阶段 66 选择一个非破坏性、可验证的结果模型深化点：后台 heal、rebalance 和 tier 统计。这三个接口都是只读状态查询或统计查询，不需要 Crypto Gate Pass，也不需要独立破坏性 lab，因此适合在外部门禁未放行前继续增强 SDK 易用性。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不降低风险 blocked 计数。

## 2. 新增模型

| 模型 | 对应专用方法 | 继续保留的通用方法 | 说明 |
| --- | --- | --- | --- |
| `AdminBackgroundHealStatus` | `getBackgroundHealStatusSummary()` | `getBackgroundHealStatus()` | 提取后台 heal 已扫描对象数、离线节点数、heal 磁盘数、set 数、MRF 节点数和存储级别 parity 数。 |
| `AdminRebalanceStatus` | `getRebalanceStatusSummary()` | `getRebalanceStatus()` | 提取 rebalance 任务 ID、停止时间、pool 数和活跃 pool 数。 |
| `AdminTierStatsSummary` | `getTierStatsSummary()` | `getTierStats()` | 汇总 tier 名称、总容量、对象数和版本数。 |

这些模型都继承 `AdminJsonResult`，仍保留 `rawJson()` 和 `values()`，所以字段漂移时调用方可以继续检查原始响应。

## 3. 为什么保留原通用方法

通用方法仍有价值：

1. MinIO Admin 响应字段可能随版本变化，通用 JSON 可以兜住未知字段。
2. 新摘要模型只提取稳定、常用的排障字段，不宣称完整替代 Go madmin 的全部结构。
3. 用户需要完整 JSON 或内部字段时，可以继续使用旧方法或 `ReactiveMinioRawClient`。

因此，本阶段采用“保留通用入口 + 新增摘要模型”的增量策略，而不是替换返回类型。

## 4. 顶层数组 JSON 的兼容修正

`tier-stats` 在 Go madmin 中返回顶层数组。阶段 66 同时修正了通用 `AdminJsonResult` 对顶层数组的处理：

- 顶层 object：继续按原字段放入 `values()`。
- 顶层 array：包装到 `values().get("items")`。

这样 `getTierStats()` 继续可用，不会因为响应是数组而误报“无法解析 MinIO JSON 响应”。

## 5. 使用示例

```java
AdminBackgroundHealStatus heal = admin.getBackgroundHealStatusSummary().block();
long scanned = heal.scannedItemsCount();
int healingDisks = heal.healDiskCount();

AdminRebalanceStatus rebalance = admin.getRebalanceStatusSummary().block();
int activePools = rebalance.activePoolCount();

AdminTierStatsSummary tiers = admin.getTierStatsSummary().block();
int tierCount = tiers.tierCount();
long totalObjects = tiers.totalObjects();
```

如果需要完整响应：

```java
String raw = admin.getTierStatsSummary().block().rawJson();
Object items = admin.getTierStats().block().values().get("items");
```

## 6. 验证口径

本阶段至少需要验证：

- 新模型解析大小写和 Go/json tag 混合字段。
- 专用客户端新方法仍命中原有 Admin 路由。
- `AdminJsonResult` 可以保留顶层数组响应。
- route parity 与 capability matrix 不发生退化。
- JDK8/JDK17 测试通过，JDK17+ 分支继续通过 JDK21/JDK25 `test-compile`。
- Crypto Gate 与破坏性 lab blocked 计数保持不变。
