# 阶段 122：replication diff 双 MinIO 拓扑补证

## 背景

`ADMIN_REPLICATION_DIFF` 对齐 MinIO server 的 `/minio/admin/v3/replication/diff`，参数通过 query 传递：`bucket` 必填，`verbose`、`prefix`、`arn` 可选。阶段 118 已经修正 SDK 调用语义，但当时缺少真实 bucket replication 拓扑，因此没有降低破坏性边界。

阶段 122 使用两个一次性 Docker MinIO：source 与 target。脚本在 source bucket 上启用 bucket replication，写入一个测试对象并等待它出现在 target bucket，然后再运行 SDK 的 typed/raw `ADMIN_REPLICATION_DIFF` 探测。

## 本阶段调整

1. 新增 `scripts/minio-lab/start-replication-diff-lab.sh`：自动启动 source/target 两个 MinIO、创建 Docker 网络、启用 versioning、配置 bucket replication，并生成 source lab 配置。
2. `DestructiveAdminIntegrationTest` 复用阶段 118 的 typed/raw replication diff 探测：
   - typed：`runReplicationDiff(bucket, true, prefix, arn)`
   - raw：`ADMIN_REPLICATION_DIFF` + 显式 query
3. `report-destructive-boundary.py` 将 `ADMIN_REPLICATION_DIFF` 移入“已有独立 lab 证据”。
4. `scripts/minio-lab/README.md` 增加双 MinIO replication diff lab 使用说明。

## 独立 lab 证据

本次正式补证在 JDK8 与 JDK17+ 分支都执行：

- `typed runReplicationDiff`：PASS
- `raw ADMIN_REPLICATION_DIFF`：PASS

报告路径：

- JDK8：`.omx/reports/stage122-jdk8-replication-diff-lab.md`
- JDK17：`.omx/reports/stage122-jdk17-replication-diff-lab.md`

## 边界更新

`ADMIN_REPLICATION_DIFF` 已有独立双 MinIO lab typed/raw 证据，因此从“拓扑或身份提供方”移动到“已有独立 lab 证据”。

仍不降低以下复制相关边界：

- `ADMIN_SITE_REPLICATION_EDIT`
- `ADMIN_SR_PEER_EDIT`
- `ADMIN_SR_PEER_REMOVE`

这些接口涉及 site replication 或 peer 级拓扑变更，不等同于 bucket replication diff。
