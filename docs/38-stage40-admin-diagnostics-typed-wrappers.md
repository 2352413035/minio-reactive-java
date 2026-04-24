# 阶段 40：Admin 诊断类 typed/stream 包装

阶段 40 的目标是把低风险的 Admin 诊断接口从“只能拿到 `Mono<String>`”推进到更清楚的产品入口。它不新增破坏性能力，也不改变 Crypto Gate 边界。

## 1. 新增产品入口

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `scrapeAdminMetrics()` | `PrometheusMetrics` | 包装 `ADMIN_METRICS` 的 Prometheus 文本，保留原文并可解析普通样本行。 |
| `downloadInspectData()` | `AdminBinaryResult` | 对应 `ADMIN_INSPECT_DATA_GET`，明确这是诊断包/二进制边界。 |
| `downloadInspectData(body, contentType)` | `AdminBinaryResult` | 对应 `ADMIN_INSPECT_DATA_POST`，请求体由 MinIO 版本决定，响应保留原始字节。 |
| `startProfiling(profilerType)` | `AdminTextResult` | 对应 `ADMIN_PROFILING_START`，提前校验 profilerType 并保留文本响应。 |
| `downloadProfilingData()` | `AdminBinaryResult` | 对应 `ADMIN_PROFILING_DOWNLOAD`，不把 pprof/压缩内容误解码为字符串。 |
| `getProfileResult(...)` | `AdminTextResult` | 对应 `ADMIN_PROFILE`，保留 profile 文本诊断结果。 |

`traceStream()` 和 `logStream()` 已在阶段 34 固定为 `Flux<byte[]>`，阶段 40 继续沿用这个流式边界，不把长连接诊断流伪装成一次性文本。

## 2. 新增响应模型

- `AdminTextResult`：记录诊断来源和原始文本，适合 profile、profiling start 等文本响应。
- `AdminBinaryResult`：记录诊断来源、字节内容和大小，适合 inspect-data、profiling download 等二进制响应。

两个模型都保留原始内容，不猜测 MinIO 每个版本的诊断格式。

## 3. 对能力矩阵的影响

阶段 40 后，Admin product-typed 从 55 / 128 提升到 60 / 128。`encrypted-blocked = 9`、`destructive-blocked = 29` 不变。

## 4. 用户使用建议

1. 常规监控优先使用 `ReactiveMinioMetricsClient`；需要 Admin 专属 metrics 时使用 `scrapeAdminMetrics()`。
2. inspect-data 和 profiling download 可能包含诊断包，应写入本机安全目录，不要打印到日志。
3. profile、trace、log 属于排障入口，调用方应设置超时和取消策略。
4. 如果需要未产品化的诊断路由，仍可使用 `ReactiveMinioRawClient` 兜底。
