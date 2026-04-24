# 阶段 37：batch job / site replication 实验矩阵

阶段 37 的目标是把 batch job 与 site replication 这两类高风险 Admin 能力纳入独立 lab 的实验矩阵。它不是共享环境默认验证，也不代表可以在普通业务测试里启动任务或修改站点复制拓扑。

## 1. batch job 矩阵

| 步骤 | 专用客户端 | raw 兜底 | 恢复要求 |
| --- | --- | --- | --- |
| 启动任务 | `ReactiveMinioAdminClient.startBatchJob(...)` | 通过 catalog 可构造 `ADMIN_START_BATCH_JOB` | 必须使用独立 lab 的可取消任务请求体。 |
| 状态读取 | `listBatchJobsInfo()`、`getBatchJobStatusInfo()` | 既有 raw 探测保留在阶段 31 夹具中 | 状态只作为证据，不修改共享环境。 |
| 取消任务 | `cancelBatchJob(...)` 作为 finally 兜底 | `ReactiveMinioRawClient` 执行 `ADMIN_CANCEL_BATCH_JOB` | 必须提供 `MINIO_LAB_BATCH_CANCEL_BODY` 或对应文件。 |

推荐使用本机私有文件：

```properties
MINIO_LAB_ALLOW_WRITE_FIXTURES=true
MINIO_LAB_BATCH_JOB_CONTENT_TYPE=application/yaml
MINIO_LAB_BATCH_START_BODY_FILE=/secure/path/batch-start-job.yaml
MINIO_LAB_BATCH_CANCEL_BODY_FILE=/secure/path/batch-cancel-job.yaml
MINIO_LAB_CANCEL_BATCH_AFTER_TEST=true
```

模板位于：

- `scripts/minio-lab/templates/batch-start-job.yaml.example`
- `scripts/minio-lab/templates/batch-cancel-job.yaml.example`

## 2. site replication 矩阵

| 步骤 | 专用客户端 | raw 兜底 | 恢复要求 |
| --- | --- | --- | --- |
| 新增站点复制配置 | `siteReplicationAdd(...)` | catalog 可构造 `ADMIN_SITE_REPLICATION_ADD` | 请求体只能引用独立 lab 的 peer。 |
| 编辑站点复制配置 | 可通过专用兼容入口执行 | `ReactiveMinioRawClient` 执行 `ADMIN_SITE_REPLICATION_EDIT` | edit 是可选项，缺少请求体时跳过。 |
| 移除站点复制配置 | `siteReplicationRemove(...)` 作为 finally 兜底 | `ReactiveMinioRawClient` 执行 `ADMIN_SITE_REPLICATION_REMOVE` | 必须提供 remove 请求体。 |

推荐使用本机私有文件：

```properties
MINIO_LAB_ALLOW_WRITE_FIXTURES=true
MINIO_LAB_SITE_REPLICATION_CONTENT_TYPE=application/json
MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE=/secure/path/site-replication-add.json
MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE=/secure/path/site-replication-edit.json
MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE=/secure/path/site-replication-remove.json
MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true
```

模板位于：

- `scripts/minio-lab/templates/site-replication-add.json.example`
- `scripts/minio-lab/templates/site-replication-edit.json.example`
- `scripts/minio-lab/templates/site-replication-remove.json.example`

## 3. 门禁与报告

`verify-env.sh` 会把 batch job 与 site replication 的请求体变量、请求体文件变量都视为危险写入夹具。只要检测到这些变量，但没有 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`，脚本就会拒绝执行。

`write-report.sh` 只记录请求体是否设置，不输出请求体内容，也不输出请求体文件路径中的敏感内容。报告新增恢复提示：batch job 失败时先 cancel，site replication 失败时先 remove。

## 4. 当前完成度口径

阶段 37 完成后，我们可以说 SDK 已经为 batch job 和 site replication 准备好独立 lab 级别的 typed/raw 实验矩阵；不能说共享 live 环境已经覆盖这些破坏性写入。真正的能力证明必须来自用户提供的独立可回滚 lab 报告。
