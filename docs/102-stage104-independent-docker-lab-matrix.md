# 阶段 104：独立 Docker lab 破坏性矩阵补证

## 目标

阶段 104 使用本机 Docker 启动不占用共享 `9000/9001` 的独立 MinIO，继续补充破坏性 Admin typed/raw 双路径证据。该阶段仍不允许在共享 MinIO 上执行破坏性写入，也不把未覆盖矩阵从 `destructive-blocked` 中移除。

## 新增脚本

新增：`scripts/minio-lab/start-docker-lab.sh`

脚本会：

1. 删除同名旧 lab 容器。
2. 使用 `minio/minio` 镜像启动一次性容器。
3. 默认映射到 `127.0.0.1:19000/19001`，避免占用共享 `9000/9001`。
4. 在 `/tmp/minio-reactive-lab-*` 生成临时凭证、`lab.properties`、`mc` 配置目录和报告目录。
5. 创建独立测试 bucket。
6. 只输出 endpoint、临时配置路径和清理命令，不输出 access key 或 secret key。

示例：

```bash
scripts/minio-lab/start-docker-lab.sh
MINIO_LAB_CONFIG_FILE=/tmp/minio-reactive-lab-xxxxxx/lab.properties \
  scripts/minio-lab/run-destructive-tests.sh
```

## 本次执行证据

本次独立 Docker lab 使用 `http://127.0.0.1:19000`，与共享 `http://127.0.0.1:9000` 隔离。双分支均执行 `DestructiveAdminIntegrationTest` 并通过。

已通过的 typed/raw 步骤包括：

| 范围 | 证据 |
| --- | --- |
| config KV | typed 写入测试值、typed 只读探测、raw `ADMIN_SET_CONFIG_KV` 写入、raw `ADMIN_HELP_CONFIG_KV` 探测、raw/typed 恢复 |
| bucket quota | typed 写入、typed 读取、raw `ADMIN_SET_BUCKET_QUOTA` 写入、raw `ADMIN_GET_BUCKET_QUOTA` 读取、raw/typed 恢复 |
| remote target | typed `listRemoteTargetsInfo` 与 raw `ADMIN_LIST_REMOTE_TARGETS` 只读探测 |

恢复核验：

- `mc admin config get <lab> api` 显示 `requests_max=0`，说明 config KV 已恢复。
- `mc quota info <lab>/<bucket>` 显示 quota 为 `0 B`，说明 bucket quota 已恢复。

## 仍未放行的破坏性矩阵

本次仍未启用以下高风险写入夹具：

- tier add/edit/remove 写入。
- remote target set/remove 写入。
- batch job start/status/cancel。
- site replication add/edit/remove。

因此 `destructive-blocked` 仍保持 `29`。本阶段的意义是把独立 Docker lab 启动与 config/quota/remote-target-readonly 证据固化为可重复流程，不是宣称所有破坏性 Admin 能力都已通过。

## 安全边界

- 临时 lab 凭证只写入 `/tmp` 私有目录，不写入仓库。
- 报告不包含 access key、secret key、token 或签名。
- 共享 MinIO 继续只允许只读 smoke，不参与破坏性写入。
