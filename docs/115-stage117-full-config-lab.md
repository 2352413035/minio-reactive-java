# 阶段 117 全量配置写入独立 lab 补证

## 目标

阶段 117 只处理 `ADMIN_SET_CONFIG` 这个“可回滚候选”破坏性边界。此前 SDK 已有 `setConfigText(...)` 和 raw `ADMIN_SET_CONFIG` 路径，但缺少独立 lab 的全量配置写入证据。

## 实现方式

新增 `DestructiveAdminIntegrationTest.shouldRewriteFullConfigOnlyInsideVerifiedLab()`：

1. 必须先通过 `verify-env.sh`，确保端点是独立 lab，不是共享 MinIO。
2. 必须显式设置 `MINIO_LAB_ALLOW_FULL_CONFIG_WRITE=true`。
3. 测试用 `getConfigDecrypted(MINIO_LAB_SECRET_KEY)` 读取独立 lab 的原始全量配置。
4. 专用 Admin 客户端执行 `setConfigText(originalConfig)` 原样写回。
5. raw 客户端执行 `ADMIN_SET_CONFIG` 原样写回，并显式构造 madmin 加密请求体。
6. finally 中继续使用原始全量配置文本恢复。

这个流程验证 typed/raw 调用链可用，但不改变配置语义，不把全量配置内容写入仓库、报告或终端。

## 门禁变化

- `scripts/minio-lab/audit-fixtures.sh` 新增 full config 原样写回准备度行。
- `scripts/minio-lab/write-report.sh` 新增 full config 开关和恢复提示。
- `scripts/report-destructive-boundary.py` 将 `ADMIN_SET_CONFIG` 从“可回滚候选”更新为“已有独立 lab 证据”。
- 聚合发布报告会从 11 个已有独立 lab 证据更新为 12 个，仍有 17 个破坏性边界需要更复杂 lab 或维护窗口。

## 仍未放行的范围

本阶段不处理 IDP 配置、站点复制 edit/peer、service restart/update、force-unlock 或 speedtest。那些操作仍可能影响认证、拓扑、可用性或资源消耗，必须单独准备 lab 或维护窗口。
