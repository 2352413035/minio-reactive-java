# 阶段 70：Admin batch job 只读状态/描述模型深化

## 1. 本阶段目标

阶段 70 继续做非破坏性成熟度提升，聚焦 `batch job status` 和 `batch job describe` 两个只读入口。MinIO 服务端实际调用这两个接口时需要 `jobId` 查询参数，因此本阶段在保留旧兼容方法的同时，新增带 `jobId` 的专用重载和更易读的摘要模型。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag。`startBatchJob`、`cancelBatchJob` 仍属于需要独立 lab/维护窗口的写入能力，本阶段不放行共享环境写入。

## 2. 新增模型与方法

| 模型 | 新方法 | 保留的通用方法 | 提取字段 |
| --- | --- | --- | --- |
| `AdminBatchJobStatusSummary` | `getBatchJobStatusSummary(jobId)` | `getBatchJobStatusInfo()`、`getBatchJobStatusInfo(jobId)` | jobId、jobType、status、startTime、lastUpdate、retryAttempts、complete、failed。 |
| `AdminBatchJobDescriptionSummary` | `describeBatchJobSummary(jobId)` | `describeBatchJob()`、`describeBatchJobInfo()` | id、user、started、jobType，并保留原始脱敏描述文本。 |

`AdminBatchJobStatusSummary` 继承 `AdminJsonResult`，继续保留 `rawJson()` 和 `values()`。它兼容 MinIO `LastMetric` 包装结构，也兼容旧测试或模拟环境里的扁平 JSON。

`AdminBatchJobDescriptionSummary` 面向 `describe-job` 常见 YAML 文本响应，也兼容 JSON 模拟响应。它只提取安全元信息，不把描述文本解释成可直接执行的请求体。

## 3. 为什么保留旧方法

早期 SDK 已经暴露了无参 `batchJobStatus()`、`describeBatchJob()` 和对应通用包装。为了不破坏已有编译，本阶段没有删除这些方法，只把无参原始调用继续标记为兼容入口。新代码应优先使用带 `jobId` 的方法：

```java
AdminBatchJobStatusSummary status = admin.getBatchJobStatusSummary("job-id").block();
AdminBatchJobDescriptionSummary description = admin.describeBatchJobSummary("job-id").block();
```

如果调用方确实需要完整响应：

```java
String rawStatus = admin.batchJobStatus("job-id").block();
String rawDescription = admin.describeBatchJob("job-id").block();
```

## 4. 安全边界

1. `getBatchJobStatusSummary(jobId)` 和 `describeBatchJobSummary(jobId)` 只读取已有 batch job 的状态或描述。
2. 本阶段不启动、不取消 batch job，不执行共享 MinIO 上的破坏性任务。
3. `describe-job` 的服务端返回通常已做脱敏处理，但 SDK 仍只在摘要字段中暴露 id/user/started/type 等安全元信息。
4. raw client 仍可按 catalog 调用同一路由，用于 SDK 尚未覆盖的新字段或排障，但业务项目优先使用专用 Admin 方法。

## 5. 验证口径

阶段 70 至少需要验证：

- `AdminBatchJobStatusSummary` 能解析 `LastMetric` 包装与扁平 JSON。
- `AdminBatchJobDescriptionSummary` 能解析 YAML 和 JSON 描述文本。
- 带 `jobId` 的状态/描述方法确实把 `jobId` 查询参数带到请求中。
- 原无参兼容入口和通用 JSON 包装继续可用。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭证扫描继续通过。
