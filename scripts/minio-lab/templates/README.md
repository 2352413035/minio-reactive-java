# 高风险 lab 请求体模板

这些模板只用于 **独立、可删除、可回滚的 MinIO lab**。请先复制到仓库外的私有目录，再填入真实 endpoint、bucket、access key、secret key、ARN 或 jobID。

## 使用边界

- 不要在仓库内填写真实凭证、token、签名或生产端点。
- 不要把填写后的请求体提交到 git。
- 模板中的资源名只是占位，执行前必须改成只属于本次 lab 的资源；tier 名称必须大写；remote target 的 `endpoint` 按 madmin 语义填写源 MinIO 服务端视角可访问的 `host:port`，不要带 `http://` 或 `https://`；Docker 端口映射场景下通常不是宿主机映射端口。
- 未在独立 lab 执行并恢复前，不能把对应 `destructive-blocked` 计数移除。

## 模板对应关系

| 模板 | 变量 | 用途 | 恢复要求 |
| --- | --- | --- | --- |
| `tier-add-minio.json.example` | `MINIO_LAB_ADD_TIER_BODY_FILE` | 新增 MinIO 类型 tier；名称必须大写 | `MINIO_LAB_REMOVE_TIER_AFTER_TEST=true` |
| `tier-edit-creds.json.example` | `MINIO_LAB_EDIT_TIER_BODY_FILE` | 可选：编辑 tier 凭证 | 最终仍通过 remove tier 恢复 |
| `remote-target-set-replication.json.example` | `MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE` | 新增 bucket replication target | 请求体内必须有 `arn`；删除优先使用 set 响应 ARN，必要时提供 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` |
| `batch-start-job.yaml.example` | `MINIO_LAB_BATCH_START_BODY_FILE` | 启动 batch job | 必须提供 cancel 请求体 |
| `batch-cancel-job.yaml.example` | `MINIO_LAB_BATCH_CANCEL_BODY_FILE` | 取消刚启动的 batch job | `MINIO_LAB_CANCEL_BATCH_AFTER_TEST=true` |
| `site-replication-add.json.example` | `MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE` | 新增站点复制配置 | 必须提供 remove 请求体 |
| `site-replication-edit.json.example` | `MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE` | 可选：编辑站点复制配置 | 最终仍通过 remove 请求体恢复 |
| `site-replication-remove.json.example` | `MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE` | 移除刚新增的站点复制配置 | `MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true` |

## 推荐流程

```bash
scripts/minio-lab/start-docker-lab.sh
mkdir -p /secure/minio-reactive-lab-fixtures
cp scripts/minio-lab/templates/*.example /secure/minio-reactive-lab-fixtures/
# 在 /secure 目录中填写请求体，不要在仓库中填写。
MINIO_LAB_CONFIG_FILE=/tmp/minio-reactive-lab-xxxxxx/lab.properties \
  scripts/minio-lab/audit-fixtures.sh
```

确认 `audit-fixtures.sh` 显示对应矩阵“可执行”后，再把 `*_BODY_FILE` 指向私有请求体文件，并运行 `run-destructive-tests.sh` 收集 typed/raw 证据。
