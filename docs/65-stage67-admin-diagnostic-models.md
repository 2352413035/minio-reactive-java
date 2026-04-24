# 阶段 67：Admin 诊断摘要模型深化

## 1. 本阶段目标

阶段 67 继续做非破坏性结果模型深化，聚焦锁热点、OBD 和 healthinfo 三类诊断入口。这些入口都是只读诊断能力，不改变服务端状态，不需要独立破坏性 lab，也不降低 Crypto Gate 边界。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag。

## 2. 新增模型与方法

| 模型 | 新方法 | 保留的通用方法 | 提取字段 |
| --- | --- | --- | --- |
| `AdminTopLocksSummary` | `getTopLocksSummary()` | `getTopLocksInfo()` | 锁数量、读锁数量、写锁数量、疑似 quorum 不足锁数量、最长持有时间。 |
| `AdminHealthInfoSummary` | `getObdInfoSummary()` | `getObdInfo()` | health/OBD payload 中的状态、版本、时间戳、部署 ID、region、server/bucket/object 数量。 |
| `AdminHealthInfoSummary` | `getHealthInfoSummary()` | `getHealthInfo()` | 同上；OBD 路由是 healthinfo 的兼容入口，因此复用同一摘要模型。 |

`getTopLocksInfo()` 仍然返回通用 JSON 包装，适合读取完整锁条目；`getTopLocksSummary()` 只提取排障看板常用字段。`getObdInfoSummary()` 和 `getHealthInfoSummary()` 只提取稳定摘要，不尝试完整复刻 Go madmin 的 health 结构。

## 3. 为什么这是安全增量

1. 三个新方法都只调用已有只读路由。
2. 原方法继续保留，返回类型不变，不破坏已有用户代码。
3. 新模型都保留 `rawJson()` 和 `values()`，字段漂移时仍能回退检查原始响应。
4. 没有新增依赖，没有引入 Crypto 解密能力，没有执行破坏性 Admin 写操作。
5. `AdminTopLocksSummary` 依赖阶段 66 的顶层数组兼容能力，因此 `top/locks` 的数组响应可以同时被通用结果和摘要模型使用。

## 4. 使用示例

```java
AdminTopLocksSummary locks = admin.getTopLocksSummary().block();
int writeLocks = locks.writeLockCount();
long maxElapsed = locks.maxElapsedNanos();

AdminHealthInfoSummary health = admin.getHealthInfoSummary().block();
String status = health.status();
int servers = health.serverCount();

AdminHealthInfoSummary obd = admin.getObdInfoSummary().block();
String deploymentId = obd.deploymentId();
```

如果调用方需要完整锁条目或 health 细节，应继续读取：

```java
String rawLocks = admin.getTopLocksInfo().block().rawJson();
String rawHealth = admin.getHealthInfoSummary().block().rawJson();
```

## 5. 验证口径

阶段 67 至少需要验证：

- 新模型能解析 MinIO madmin 的顶层数组锁列表和 health/OBD JSON。
- 新方法命中原有 `top/locks`、`obdinfo`、`healthinfo` 路由。
- 原通用方法继续可用。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭证扫描继续通过。
