# 阶段 61：mc 只读运维证据

## 1. 本阶段结论

阶段 61 使用系统已安装的 `mc` 命令补充真实 MinIO 的只读运维证据。该证据不替代 SDK 单元测试和 live 测试，而是从 MinIO 官方 CLI 侧再次确认：当前共享 MinIO 端点在线、区域为 `us-east-1`、可列根路径、可读取 Admin info 摘要。

本阶段没有新增 SDK API，没有降低 Crypto Gate 或破坏性 lab blocked 计数，也没有把真实 access key、secret key 或页面登录密码写入仓库。

## 2. 执行方式

`mc` 通过运行时环境变量注入连接信息，例如 `MC_HOST_<alias>`。该方式不需要在仓库中保存 alias 配置，也不会把真实凭证写入文档、报告或提交信息。

本阶段只执行只读命令：

- `mc --version`
- `mc --json ready <alias>`
- `mc --json ls <alias>`
- `mc --json admin info <alias>`

完整本机输出保存到 `.omx/reports/mc-readonly-stage61.md`，该文件不进入 git 提交，并已执行真实凭证扫描。

## 3. 证据摘要

| 检查项 | 结果 |
| --- | --- |
| `mc` 版本 | `RELEASE.2025-08-13T08-35-41Z` |
| 端点 | `http://127.0.0.1:9000` |
| ready | `healthy = true`，`maintenanceMode = false`，`writeQuorum = 1` |
| 根路径 list | 可读取根路径，当前至少存在一个 bucket 条目。 |
| admin info | `mode = online`，`region = us-east-1`，bucket 数量为 1，对象数量为 362。 |
| 后端 | Erasure，在线磁盘 1，离线磁盘 0。 |

这些结果说明共享 MinIO 当前可用于安全 smoke 和只读观测；它仍不能作为破坏性 Admin 写入通过的证据。

## 4. 与 SDK 验证的关系

阶段 60 已经证明：

- route parity：233 / 233。
- Admin product-typed：128 / 128。
- `raw-fallback = 0`。
- JDK8/JDK17 单元测试与真实 MinIO live 测试通过。

阶段 61 的 `mc` 证据只增加外部只读旁证：MinIO 服务端本身在线，并且 CLI 从同一端点读到的健康和 Admin info 摘要与 SDK 文档口径一致。

## 5. 继续保留的边界

- `encrypted-blocked = 9`：`mc admin info` 不等于 madmin 默认加密响应解密能力放行。
- `destructive-blocked = 29`：`mc` 只读成功不等于 tier、remote target、batch job、site replication、force-unlock 等破坏性写入已在独立 lab 通过。
- 真实凭证只能作为运行时输入，不允许写入仓库、文档、报告或提交信息。

## 6. 下一阶段建议

下一阶段可以继续整理发布说明和用户指南：

1. 把“普通用户如何选 `ReactiveMinioClient` / Admin / KMS / STS / Metrics / Health / Raw”的路径写得更短。
2. 把 Crypto Gate 和破坏性 lab 的未放行原因放到 release notes 显眼位置。
3. 如果用户提供独立 lab，再执行破坏性写入矩阵；否则不要降低 blocked 计数。
