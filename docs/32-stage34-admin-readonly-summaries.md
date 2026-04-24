# 阶段 34：Admin 只读状态摘要扩展

阶段 34 继续扩展 Admin 只读能力，但仍坚持两个边界：不触碰破坏性写操作，不绕过 Crypto Gate。

## 1. 新增入口

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `getSiteReplicationMetainfo()` | `AdminJsonResult` | 读取站点复制元信息，保留完整 raw JSON 和 Map。 |
| `traceStream()` | `Flux<byte[]>` | 读取 Admin trace 输出流，调用方负责超时、过滤和取消。 |
| `logStream()` | `Flux<byte[]>` | 读取 Admin 日志输出流，调用方负责订阅生命周期。 |

## 2. 设计说明

- site replication metainfo 是只读 JSON，先用 `AdminJsonResult` 保留全部字段，避免 MinIO 字段漂移时丢失信息。
- trace/log 可能是持续输出的诊断流，不能包装成普通一次性摘要。
- 旧的 `trace()` / `log()` 仍保留为 advanced 兼容入口；新业务代码优先使用 `traceStream()` / `logStream()`。

## 3. 对完成度的影响

阶段 34 后，Admin product-typed 口径从 50 / 128 提升到 53 / 128。这个数字表示用户友好产品入口增加，不改变 encrypted-blocked 和 destructive-blocked 边界。
