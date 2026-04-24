# 阶段 69：Admin pool 只读摘要模型深化

## 1. 本阶段目标

阶段 69 继续做非破坏性成熟度提升，聚焦 `pools/list` 和 `pools/status` 两个只读入口。它们用于查看 pool 与 decommission 状态，不启动、取消或修改 decommission 任务，因此不需要独立破坏性 lab，也不能降低 `destructive-blocked` 计数。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag。

## 2. 新增模型与方法

| 模型 | 新方法 | 保留的通用方法 | 提取字段 |
| --- | --- | --- | --- |
| `AdminPoolListSummary` | `listPoolsSummary()` | `listPoolsInfo()` | pool 数、包含 decommission 信息的 pool 数、活跃/完成/失败/取消 decommission 数、容量汇总。 |
| `AdminPoolStatusSummary` | `getPoolStatusSummary(pool)` | `getPoolStatus(pool)` | pool id、cmdline、lastUpdate、decommission 是否存在、完成/失败/取消状态、容量和对象/字节迁移计数。 |

这两个模型都继承 `AdminJsonResult`，继续保留 `rawJson()` 和 `values()`。如果 MinIO 后续增加新的 pool 字段，调用方仍可通过通用 JSON 或 raw JSON 读取。

## 3. 安全边界

1. `listPoolsSummary()` 和 `getPoolStatusSummary(pool)` 只调用已有只读路由。
2. 本阶段不触碰 `startPoolDecommission()`、`cancelPoolDecommission()` 或任何维护窗口写操作。
3. 摘要模型只能作为排障和看板辅助，不代表 decommission 真实写入矩阵已经在独立 lab 放行。
4. 如果调用方需要完整 pool metadata，应继续使用原通用方法或 `rawJson()`。

## 4. 使用示例

```java
AdminPoolListSummary pools = admin.listPoolsSummary().block();
int poolCount = pools.poolCount();
int activeDecommission = pools.activeDecommissionCount();

AdminPoolStatusSummary pool = admin.getPoolStatusSummary("pool-0").block();
long currentSize = pool.currentSize();
boolean failed = pool.decommissionFailed();
```

如果需要完整响应：

```java
String rawList = admin.listPoolsSummary().block().rawJson();
Map<String, Object> fullStatus = admin.getPoolStatus("pool-0").block().values();
```

## 5. 验证口径

阶段 69 至少需要验证：

- 新模型能解析 MinIO `PoolStatus` 与 `PoolDecommissionInfo` 常见字段。
- 新方法命中原有 `pools/list`、`pools/status` 路由，并保留 `pool` 查询参数校验。
- 原通用 JSON 方法继续可用。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭证扫描继续通过。
