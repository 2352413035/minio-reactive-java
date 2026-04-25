# 阶段 118 replication diff query 语义与 lab 门禁

## 背景

`ADMIN_REPLICATION_DIFF` 对应 madmin 的 `BucketReplicationDiff`。对照 `madmin-go` 后确认：该接口不通过请求体传递选项，而是使用 query 参数：

- `bucket`：必填。
- `verbose`：可选，值为 `true` 时包含更多状态。
- `prefix`：可选，只扫描指定对象前缀。
- `arn`：可选，只查看指定远端目标。

原有 SDK 已能调用该路由，但用户友好的 `runReplicationDiff(bucket, body)` 更像通用 body 写入接口。阶段 118 增加无请求体和 query 选项重载，让专用 Admin 客户端更贴近 minio-java / madmin 语义。

## 本阶段变化

- `runReplicationDiff(String bucket)`：按 madmin 默认选项调用。
- `runReplicationDiff(String bucket, boolean verbose, String prefix, String arn)`：显式传入 query 选项。
- `replicationDiff(String bucket, boolean verbose, String prefix, String arn)`：advanced 兼容入口。
- 原 `runReplicationDiff(String bucket, byte[] body, String contentType)` 保留为历史兼容入口。
- 破坏性 lab 新增 `MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE`，可以在具备真实 bucket replication 配置的独立 lab 中对比 typed/raw 输出。

## 为什么本阶段不降低 destructive 边界

replication diff 会扫描 bucket 的复制状态。没有真实 bucket replication 配置、远端 target 和可控数据集时，普通 Docker lab 返回错误是合理结果。阶段 118 只把 SDK 调用语义修正为 madmin query 形态，并提供 lab 门禁；只有后续拿到真实复制拓扑 typed/raw PASS 步骤文件后，才能把 `ADMIN_REPLICATION_DIFF` 从“拓扑或身份提供方”移动到“已有独立 lab 证据”。
