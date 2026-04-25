# 阶段 109：site replication 多站点 lab 矩阵补证

## 目标

本阶段补齐 site replication 在独立多站点 Docker lab 中的 typed/raw 证据，确认 SDK 对 MinIO madmin 语义的处理是正确的：

1. `site-replication/add` 请求体必须是 madmin-go 的 `PeerSite[]` 数组，字段名为 `endpoints`。
2. `add` 与 `edit` 属于 madmin 加密请求体，专用 Admin 客户端自动加密；raw 路径必须显式传入 `MadminEncryptionSupport.encryptData(...)` 后的字节。
3. `site-replication/remove` 请求体是明文 `SRRemoveReq`，独立 lab 的最小恢复体可用 `{"all": true}`。
4. 专用客户端和 raw catalog 都能在同一套隔离站点中执行并恢复。

## 本阶段改动

- 修正 `site-replication-add.json.example`：从旧的 `{ "sites": [...] }` 改为 madmin-go 实际使用的 `PeerSite[]` 数组，并使用 `endpoints` 字段。
- 修正 `site-replication-remove.json.example`：给出 `{ "all": true }` 作为独立 lab 最小清理体。
- 补充 site replication lab 文档：Docker 网络中 endpoint 应使用容器名和服务端视角 URL，不能使用宿主机映射端口。
- 增强 `DestructiveAdminIntegrationTest`：site replication 写入矩阵现在覆盖：
  - typed add；
  - typed info/status/metainfo；
  - raw remove；
  - raw add after remove；
  - typed info after raw add；
  - finally typed remove；
  - restore 后 typed status。

## 独立 Docker lab 证据

本阶段使用两个一次性 MinIO 容器：

- `site-a`：作为测试入口，SDK 连接宿主机隔离端口。
- `site-b`：作为 peer，`site-a` 通过 Docker 网络容器名访问。
- 两个站点均为空 bucket 状态，满足 MinIO site replication 初始配置要求。
- add/remove 请求体、root 凭证和 lab 配置均写在 `/tmp` 私有目录，运行后删除。

JDK8 与 JDK17+ 本阶段均得到以下步骤证据：

| 步骤 | 结果 |
| --- | --- |
| typed `siteReplicationAdd` | PASS |
| typed `getSiteReplicationInfo` after add | PASS |
| typed `getSiteReplicationStatus` after add | PASS |
| typed `getSiteReplicationMetainfo` after add | PASS |
| raw `ADMIN_SITE_REPLICATION_REMOVE` | PASS |
| raw `ADMIN_SITE_REPLICATION_ADD` after remove | PASS |
| typed `getSiteReplicationInfo` after raw add | PASS |
| finally typed `siteReplicationRemove` | PASS |
| typed `getSiteReplicationStatus` after restore | PASS |

两次运行结束后均确认没有遗留 `minio-reactive-site-*` 或 `minio-reactive-destructive-lab` 容器。

## 仍保留的边界

- site replication 即使 lab 通过，仍属于高风险 Admin 操作；共享 MinIO 和生产环境不能默认执行。
- `destructive-blocked` 当前仍按风险路由分类统计，不因某次私有 lab 通过直接清零。
- Crypto Gate 默认 Argon2id / ChaCha20-Poly1305 解密和正式 Maven/tag 发布材料仍是独立门禁。
