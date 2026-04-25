# 阶段 105：高风险 lab 夹具模板与准备度审计

## 背景

阶段 104 已经用独立 Docker MinIO 证明：config KV、bucket quota 的 typed/raw 写入恢复，以及 remote target typed/raw 只读探测可以在隔离端口执行并恢复。剩余 `destructive-blocked = 29` 主要集中在更高风险的 lab 矩阵：tier 写入、remote target 写入、batch job 和 site replication。

本阶段不宣称这些矩阵已经通过，而是把“用户要准备什么、SDK 会怎么拒绝不完整夹具、哪些请求体必须放在仓库外”固化下来。这样后续真正执行矩阵时，可以直接补证，而不是再次猜请求体结构。

## 新增内容

### 1. 高风险请求体模板

新增目录说明：`scripts/minio-lab/templates/README.md`

新增模板：

| 模板 | 对应变量 | 用途 | 说明 |
| --- | --- | --- | --- |
| `tier-add-minio.json.example` | `MINIO_LAB_ADD_TIER_BODY_FILE` | 新增 MinIO 类型 tier | endpoint、bucket、凭证必须替换为独立 lab 资源 |
| `tier-edit-creds.json.example` | `MINIO_LAB_EDIT_TIER_BODY_FILE` | 可选：编辑 tier 凭证 | 不是最小闭环必需项 |
| `remote-target-set-replication.json.example` | `MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE` | 新增 bucket replication target | 请求体内必须有 lab ARN；删除时优先使用 set 响应 ARN，必要时提供兜底 ARN |

已有模板继续使用：

- `batch-start-job.yaml.example`
- `batch-cancel-job.yaml.example`
- `site-replication-add.json.example`
- `site-replication-edit.json.example`
- `site-replication-remove.json.example`

所有模板都只放占位值。填写后的请求体必须复制到仓库外的私有目录，不能提交到 git。

### 2. 夹具准备度审计脚本

新增：`scripts/minio-lab/audit-fixtures.sh`

它只读取环境变量或 `MINIO_LAB_CONFIG_FILE` 指向的私有配置文件，不连接 MinIO，不执行任何写入。输出内容包括：

- 基础 lab 硬门禁是否通过。
- 每个请求体变量是否已设置。
- 每个矩阵缺少哪些最小前置条件。
- 对应的恢复变量和模板路径。

它不会输出：

- access key
- secret key
- token
- 签名
- 请求体内容

### 3. 准备度与硬门禁关系

- `audit-fixtures.sh` 是“准备清单”，即使硬门禁未通过也会继续打印缺口，方便先补变量。
- `audit-readiness.sh` 是“硬门禁检查”，失败时会拒绝启动破坏性测试，并提示使用 `audit-fixtures.sh` 查看逐项缺口。
- `run-destructive-tests.sh` 仍然只在 `verify-env.sh` 通过后才执行真实测试。

## 各矩阵最小条件

| 矩阵 | 最小请求体/变量 | 恢复要求 | 当前阶段状态 |
| --- | --- | --- | --- |
| config KV | `MINIO_LAB_TEST_CONFIG_KV` + `MINIO_LAB_RESTORE_CONFIG_KV` | finally 恢复配置值 | 阶段 104 已有真实 typed/raw 证据 |
| bucket quota | bucket + test quota JSON + restore quota JSON | finally 恢复 quota | 阶段 104 已有真实 typed/raw 证据 |
| tier 只读探测 | `MINIO_LAB_TIER_NAME` | 无写入恢复 | 有准备度提示，真实执行取决于 lab 是否存在 tier |
| remote target 只读探测 | bucket，可选预期 ARN | 无写入恢复 | 阶段 104 已有 typed/raw 只读探测证据 |
| batch job 只读探测 | `MINIO_LAB_ENABLE_BATCH_JOB_PROBES=true`，可选 jobID | 无写入恢复 | 仍需真实 job 夹具 |
| tier add/edit/remove | 写入总开关 + tier 名称 + add 请求体 | `MINIO_LAB_REMOVE_TIER_AFTER_TEST=true` | 已有模板，未执行高风险矩阵 |
| remote target set/remove | 写入总开关 + bucket + set 请求体（含 lab ARN，endpoint 为源 MinIO 服务端视角的 host:port）；删除 ARN 可由 set 响应解析 | set 响应 ARN；可选手工 ARN 兜底 + remove 开关 | 已有模板，未执行高风险矩阵 |
| batch job start/status/cancel | 写入总开关 + start YAML + cancel YAML | cancel 开关 | 已有模板，未执行高风险矩阵 |
| site replication add/edit/remove | 写入总开关 + add JSON + remove JSON | remove 开关 | 已有模板，通常需要多节点 lab，未执行高风险矩阵 |

## 为什么仍不降低 blocked 计数

本阶段只补齐模板和准备度审计，没有执行 tier、remote target set/remove、batch job 或 site replication 的真实 typed/raw 写入恢复矩阵。按照当前风险口径：

- `destructive-blocked` 继续保持 `29`。
- 只有后续在独立 lab 中拿到 typed/raw PASS、恢复成功和报告证据后，才能逐项降低阻塞计数。
- 单节点 Docker lab 可以覆盖一部分矩阵，但 site replication 通常需要多个独立 MinIO 站点。

## 后续阶段建议

阶段 106 可以在本机 Docker lab 上继续尝试低耦合的高风险矩阵：

1. 优先 remote target set/remove，因为可以使用同一个独立 lab 或另一个临时 bucket 构造最小 replication target。
2. 其次 tier add/remove，如果能准备只属于 lab 的目标 bucket 和凭证。
3. batch job 与 site replication 需要更严格的可取消/多站点前置条件，必须在 `audit-fixtures.sh` 显示“可执行”后再进入真实测试。
