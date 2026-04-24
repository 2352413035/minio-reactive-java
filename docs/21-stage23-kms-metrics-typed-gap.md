# 21 阶段 23：KMS 与 Metrics 低风险 typed 补缺

## 1. 背景

阶段 23 优先处理低风险、可直接模型化的非 Admin 缺口。KMS 和 Metrics 的剩余缺口主要是指标文本入口：它们不修改服务端状态，也不涉及 madmin 加密边界，适合先进入 typed 主路径。

## 2. 新增能力

| 客户端 | 方法 | 对应路由 | 说明 |
| --- | --- | --- | --- |
| `ReactiveMinioKmsClient` | `scrapeMetrics()` | `KMS_METRICS` | 返回 `PrometheusMetrics("kms", text)`，保留原始文本并支持样本解析。 |
| `ReactiveMinioMetricsClient` | `scrapeLegacyMetrics(bearerToken)` | `METRICS_PROMETHEUS_LEGACY` | 返回 `PrometheusMetrics("legacy", text)`，与 v2/v3 指标入口口径一致。 |

## 3. 边界

- Metrics 是否需要 bearer token 由 MinIO 服务端配置决定；SDK 不默认伪造 token。
- KMS metrics 是只读指标入口，不等同于 KMS key 管理。
- STS 剩余证书、自定义 token、SSO 入口仍需要独立安全环境或外部身份源验证，本阶段只记录边界，不把它们伪装成已完成 typed 主路径。

## 4. 验证要求

- `ReactiveMinioSpecializedClientsTest` 覆盖方法存在、请求 path 和 typed wrapper scope。
- 能力矩阵 KMS 从 6 / 7 提升到 7 / 7，Metrics 从 5 / 6 提升到 6 / 6。
- 双分支保持相同 API 与相同验证结果。
