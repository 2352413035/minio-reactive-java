# 阶段 106：高风险 Admin 写入的 madmin 加密语义对齐

## 背景

阶段 105 补齐了高风险 lab 夹具模板。继续对照 MinIO 管理协议后，发现部分高风险写入接口不是直接发送明文请求体，而是由 madmin 客户端使用当前凭证的 `secretKey` 加密后再提交。

本阶段修正专用 Admin 客户端的写入语义，避免真实独立 lab 中出现“请求体结构正确但服务端拒绝”的问题。

## 调整范围

新增内部能力：

- `ReactiveMinioCatalogClientSupport.executeEncryptedBytesToString(...)`

切换为自动加密请求体的专用 Admin 方法：

| 方法 | 对应路由 | 调整后语义 |
| --- | --- | --- |
| `addTier(...)` | `ADMIN_ADD_TIER` | 使用当前凭证 secretKey 加密请求体，发送 `application/octet-stream` |
| `editTier(...)` | `ADMIN_EDIT_TIER` | 使用当前凭证 secretKey 加密请求体，发送 `application/octet-stream` |
| `setRemoteTarget(...)` | `ADMIN_SET_REMOTE_TARGET` | 使用当前凭证 secretKey 加密请求体，并保留服务端返回 ARN 文本 |
| `siteReplicationAdd(...)` | `ADMIN_SITE_REPLICATION_ADD` | 使用当前凭证 secretKey 加密请求体，保留 `api-version=1` |
| `siteReplicationEdit(...)` | `ADMIN_SITE_REPLICATION_EDIT` | 使用当前凭证 secretKey 加密请求体，保留 `api-version=1` |

不改变的语义：

- `ReactiveMinioRawClient` 仍然不自动判断业务接口是否需要 madmin 加密。调用 raw 时，用户或测试必须自己准备服务端期望的 body。
- `siteReplicationRemove(...)`、batch job start/cancel 等仍按 MinIO 管理协议的原始 body 语义发送，不在本阶段强行加密。

## remote target 夹具改进

remote target set 请求体必须包含合法 ARN；set 成功后服务端也会返回 ARN。破坏性 lab 测试现在优先使用这个返回 ARN 做恢复：

1. 专用客户端 set remote target。
2. 解析返回 ARN。
3. raw 删除刚写入的 target。
4. raw 使用显式加密 body 再次 set remote target。
5. finally 中用 raw set 返回的 ARN 或前一个 ARN 做专用客户端删除恢复。

因此 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` 从必填变成可选兜底。只有目标 MinIO 不返回 ARN 或响应无法解析时，才需要手工提供。

## 风险边界

## 本阶段 lab 证据

在一次性 Docker MinIO lab 中，JDK8 分支和 JDK17+ 分支均执行了 `DestructiveAdminIntegrationTest#shouldWriteAndRestoreTierAndRemoteTargetOnlyInsideVerifiedLab` 的 remote target 写入子矩阵。

已验证步骤：

1. source/target bucket 均开启版本控制。
2. 专用 Admin 客户端使用加密 body 执行 `ADMIN_SET_REMOTE_TARGET`。
3. typed `listRemoteTargetsInfo` 可读取写入结果。
4. raw `ADMIN_REMOVE_REMOTE_TARGET` 删除专用客户端写入的 target。
5. raw 使用显式加密 body 执行 `ADMIN_SET_REMOTE_TARGET`。
6. finally 中专用 Admin 客户端删除 raw 写入的 target。

临时容器、请求体和配置目录已清理；没有真实凭证或请求体进入仓库。

## 风险边界

- 本阶段只把 remote target set/remove 从“有夹具模板”推进到“双分支独立 lab 有 typed/raw 写入恢复证据”。
- tier 写入、batch job、site replication 仍需要额外私有夹具和真实恢复证据。
- 为保持既有风险口径，`destructive-blocked` 仍继续保持 `29`，表示这些接口仍属于只能在独立 lab 或维护窗口执行的破坏性能力。
- 所有真实请求体仍必须放在仓库外私有目录；仓库中只保留模板和中文说明。
