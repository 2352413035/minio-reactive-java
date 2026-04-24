# 26 阶段 28 S3 notification 与 replication metrics typed 摘要

## 1. 目标

阶段 28 继续推进 S3 product-typed 成熟度，把 notification 配置和 replication metrics 从 advanced-compatible 推进到 typed 主路径。

## 2. 新增模型

- `BucketNotificationTarget`：表示 Queue、Topic、CloudFunction 三类 notification 目标，包含 ARN、事件列表和 prefix/suffix filter。
- `BucketNotificationConfiguration`：bucket notification 配置集合，保留原始 XML。
- `BucketReplicationMetrics`：replication metrics JSON 包装，保留版本、原始 JSON 和 Map 值。

## 3. 新增方法

| 方法 | 说明 |
| --- | --- |
| `getBucketNotificationConfiguration(bucket)` | 获取 notification XML 并解析为目标列表。 |
| `setBucketNotificationConfiguration(bucket, configuration)` | 根据 typed 配置生成 notification XML。 |
| `getBucketReplicationMetrics(bucket)` | 获取旧版 replication metrics JSON 包装。 |
| `getBucketReplicationMetricsV2(bucket)` | 获取新版 replication metrics JSON 包装，额外暴露 `uptime()`。 |

对应 advanced 方法 `s3GetBucketNotification`、`s3PutBucketNotification`、`s3GetBucketReplicationMetrics`、`s3GetBucketReplicationMetricsV2` 已标记为 `@Deprecated`，继续保留迁移入口。

## 4. 边界说明

- notification listen 路由仍是长连接/事件流能力，阶段 28 不把它伪装成普通一次性响应。后续需要单独设计 `Flux` 事件模型。
- replication metrics 响应是 MinIO JSON，字段可能随服务端版本变化；当前模型保留 raw JSON 和 Map，避免字段漂移时丢失信息。

## 5. 验证

- `S3XmlTest` 覆盖 notification XML 生成和解析。
- `ReactiveMinioSpecializedClientsTest` 覆盖 notification/replication metrics typed 方法暴露、query 构造和 deprecated 迁移标记。
- `scripts/report-capability-matrix.py` 显示 S3 product-typed 从 72 / 77 提升到 76 / 77。


## 阶段 33 补充

通知监听现在提供 `listenBucketNotification(...)` 和 `listenRootNotification(...)`，以 `Flux<byte[]>` 暴露长连接事件流。配置读写仍使用阶段 28 的 notification typed XML 模型。
