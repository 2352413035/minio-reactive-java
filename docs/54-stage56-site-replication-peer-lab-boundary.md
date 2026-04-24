# 阶段 56：站点复制 peer lab-only 边界

## 1. 本阶段目标

阶段 56 将站点复制 peer 写入类接口补充为明确的产品文本边界。它们会影响站点复制状态、bucket 元数据或 IAM 同步，因此只能在独立 lab 或维护窗口中真实执行。

## 2. 新增产品方法

| 方法 | 对应路由 | 返回 | 边界说明 |
| --- | --- | --- | --- |
| `joinSiteReplicationPeer(...)` | `ADMIN_SR_PEER_JOIN` | `AdminTextResult` | 加入 peer，固定 `api-version=1`。 |
| `applySiteReplicationPeerBucketOperation(...)` | `ADMIN_SR_PEER_BUCKET_OPS` | `AdminTextResult` | bucket 操作必须显式传入 bucket 与 operation。 |
| `applySiteReplicationPeerIamItem(...)` | `ADMIN_SR_PEER_IAM_ITEM` | `AdminTextResult` | IAM 同步请求体不解析、不记录。 |
| `applySiteReplicationPeerBucketMetadata(...)` | `ADMIN_SR_PEER_BUCKET_META` | `AdminTextResult` | bucket metadata 同步请求体由调用方审计。 |
| `runSiteReplicationResyncOperation(...)` | `ADMIN_SITE_REPLICATION_RESYNC_OP` | `AdminTextResult` | resync operation 必须显式传入。 |
| `editSiteReplicationState(...)` | `ADMIN_SR_STATE_EDIT` | `AdminTextResult` | lab-only 高风险状态写入。 |

## 3. 安全边界

- 所有新增方法要求非空请求体，避免误发空写入。
- SDK 固定 `api-version=1`，保持与 madmin-go 专用客户端协议一致。
- SDK 不解析、不保存、不记录请求体中的 IAM、bucket metadata 或站点复制状态内容。
- 本阶段只做 mock 和 raw 交叉验证，不在共享 MinIO 上真实执行站点复制写入。

## 4. 当前能力矩阵变化

- Admin product-typed：97 / 128 -> 103 / 128。
- `raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 保持不变。
- `destructive-blocked = 29` 保持不变。

## 5. 后续建议

阶段 57 已继续处理服务控制、服务端升级、token 吊销强破坏性入口。剩余 Admin 缺口主要集中在 Crypto Gate 和需要独立 lab 的强破坏性写入，后续仍应根据真实 lab 证据决定是否减少 blocked 计数。
