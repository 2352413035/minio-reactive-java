# 阶段 110：破坏性 lab 缺口再审计与 tier edit 补证

## 目标

阶段 104 到 109 已经把主要可回滚高风险路径补到独立 Docker lab。本阶段继续做两件事：

1. 补齐 tier edit 的 typed/raw 证据，确认 `ADMIN_EDIT_TIER` 的请求体与 madmin-go `TierCreds` 一致。
2. 重新解释 `destructive-blocked = 29`：它是风险分类计数，不是“SDK 缺 29 个功能”。这些接口已经有产品入口和 raw 兜底；是否能在共享环境执行，取决于独立 lab 或维护窗口。

## 本阶段改动

- 修正 `tier-edit-creds.json.example`：字段改为 madmin-go 使用的 `access` / `secret`，不再使用旧式 `AccessKey` / `SecretKey`。
- 更新 lab 模板说明，明确 tier edit 请求体不是完整 tier 配置，而是 `TierCreds` 凭据片段。
- 记录 tier edit 独立 Docker lab 证据：typed edit 与 raw edit 都通过，并通过 finally remove 恢复。

## 独立 Docker lab 证据

本阶段使用两个一次性 MinIO 容器：

- source：SDK 连接的 Admin 入口。
- target：作为 MinIO 类型 tier 目标，source 通过 Docker 网络容器名访问。
- target 中创建仅属于本次 lab 的 bucket。
- add/edit 请求体、凭据和配置均保存在 `/tmp` 私有目录，运行后删除。

JDK8 与 JDK17+ 本阶段均得到以下 tier edit 证据：

| 步骤 | 结果 |
| --- | --- |
| typed `addTier` | PASS |
| typed `listTiers` after add | PASS |
| raw `ADMIN_REMOVE_TIER` | PASS |
| raw `ADMIN_ADD_TIER` | PASS |
| typed `listTiers` after raw add | PASS |
| typed `editTier` | PASS |
| raw `ADMIN_EDIT_TIER` | PASS |
| typed `listTiers` after edit | PASS |
| finally typed `removeTier` | PASS |
| typed `listTiers` after restore | PASS |

两次运行结束后均确认没有遗留 `minio-reactive-tieredit-*` 或 `minio-reactive-destructive-lab` 容器。

## `destructive-blocked = 29` 的解释

当前 capability matrix 仍显示 Admin `destructive-blocked = 29`。这不是因为这些接口没有实现，而是因为这些路由属于高风险 Admin 操作：

- 已有独立 lab typed/raw 证据的可回滚路径：config KV、bucket quota、remote target set/remove、tier add/edit/remove、batch job start/status/cancel、site replication add/remove。
- 仍应保持维护窗口边界的强操作：service restart/update、decommission、rebalance、force-unlock、speedtest 系列等。
- 仍需具体 deploymentID/真实拓扑才值得执行的变体：site replication edit endpoint/sync/bandwidth 等。

因此，项目完成度口径仍应表述为：minio-java 主体 API、Args、credentials、route catalog 和产品入口已完成；高风险操作必须通过独立 lab 或维护窗口显式执行，不能在共享环境默认运行。

## 仍保留的边界

- Crypto Gate 默认 Argon2id / ChaCha20-Poly1305 解密仍未放行。
- site replication edit 的真实 endpoint/deploymentID 变更尚未执行；它需要更复杂的拓扑和明确变更目标。
- 正式 Maven/tag 发布仍需要 license、SCM、developers、source/javadoc、sign、SBOM 等负责人材料。
