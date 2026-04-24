# MinIO 破坏性实验环境说明

这里的脚本只服务于 **独立、可回滚** 的 Admin 实验环境。共享 MinIO（例如 `http://127.0.0.1:9000`）只允许安全 live 测试，不能执行 破坏性 Admin 测试。

## 硬门禁

只有满足以下条件时，才允许 破坏性 Admin 测试开始：

- `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
- `MINIO_LAB_ENDPOINT`、`MINIO_LAB_ACCESS_KEY`、`MINIO_LAB_SECRET_KEY` 已设置
- `MINIO_LAB_CAN_RESTORE=true`
- `MINIO_LAB_ENDPOINT` 不能等于共享环境或常见本机默认环境：`http://127.0.0.1:9000`、`http://localhost:9000`、`http://0.0.0.0:9000`
- 如果同时设置了 `MINIO_ENDPOINT`，`MINIO_LAB_ENDPOINT` 不能与它相同
- `scripts/minio-lab/verify-env.sh` 返回 0

## 运行方式

### 方式一：使用环境变量

```bash
export MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true
export MINIO_LAB_ENDPOINT=http://独立-lab:9000
export MINIO_LAB_ACCESS_KEY=...
export MINIO_LAB_SECRET_KEY=...
export MINIO_LAB_CAN_RESTORE=true

scripts/minio-lab/run-destructive-tests.sh
```

### 方式二：使用独立 lab 配置文件

阶段 24 起，推荐把 lab 参数集中放入本机私有配置文件，而不是把一长串可选变量散落在 shell 历史中：

```bash
cp scripts/minio-lab/lab.example.properties scripts/minio-lab/lab.properties
# 编辑 scripts/minio-lab/lab.properties，填入独立 lab 端点 和凭证。
scripts/minio-lab/run-destructive-tests.sh
```

也可以把配置文件放到仓库外：

```bash
export MINIO_LAB_CONFIG_FILE=/secure/path/minio-lab.properties
scripts/minio-lab/run-destructive-tests.sh
```

配置文件采用简单 `KEY=VALUE` 格式。脚本只读取键值，不执行配置文件中的 shell 代码。真实凭证和真实端点 不要提交到仓库。

## config write + restore 用例

如果要执行真实配置写入与恢复，还必须提供一组可回滚配置：

```bash
export MINIO_LAB_TEST_CONFIG_KV='api requests_max=10'
export MINIO_LAB_RESTORE_CONFIG_KV='api requests_max=0'
```

测试流程是：

1. 运行 `verify-env.sh`，证明这是独立 lab。
2. 使用 `setConfigKvText(MINIO_LAB_TEST_CONFIG_KV)` 写入测试配置。
3. 通过 `getConfigHelp(...)` 做只读探测，证明服务端仍可响应。
4. finally 中使用 `setConfigKvText(MINIO_LAB_RESTORE_CONFIG_KV)` 恢复。
5. 恢复后再次执行只读探测。

如果没有提供这两个变量，真实配置写入测试会跳过；默认 `mvn test` 永远不会修改 MinIO 服务端配置。

## bucket quota / tier / remote target / batch job lab 夹具

阶段 24 起，破坏性实验环境支持更多可选夹具。它们默认全部跳过，只有在独立 lab 配置文件或环境变量中明确设置后才执行：

```properties
MINIO_LAB_BUCKET=lab-bucket
MINIO_LAB_TEST_BUCKET_QUOTA_JSON={"quota":1048576,"type":"hard"}
MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON={"quota":0,"type":"hard"}

MINIO_LAB_TIER_NAME=archive
MINIO_LAB_EXPECT_TIER_IN_LIST=false
MINIO_LAB_REMOTE_TARGET_TYPE=replication
MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN=
MINIO_LAB_ENABLE_BATCH_JOB_PROBES=false
MINIO_LAB_BATCH_EXPECTED_JOB_ID=
```

- bucket quota 写入必须同时设置测试值和恢复值，并且 finally 中总是尝试恢复。
- tier、remote target、batch job 只做仅实验环境 typed/raw 双路径探测，不进入共享 live 门禁。
- `MINIO_LAB_EXPECT_TIER_IN_LIST=true` 时，`listTiers()` typed 摘要必须能看到 `MINIO_LAB_TIER_NAME`。
- `MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN` 不为空时，`listRemoteTargetsInfo(...)` typed 摘要必须能看到该 ARN。
- `MINIO_LAB_BATCH_EXPECTED_JOB_ID` 不为空时，batch job typed 摘要或 raw JSON 中必须能看到该任务 ID。
- `verify-env.sh` 仍会拒绝共享端点 和常见本机默认端点。

## tier / remote target 可回滚写入夹具

阶段 36 起，独立 lab 可以额外开启真实写入夹具。它们比只读探测更危险，因此必须先打开总开关：

```properties
MINIO_LAB_ALLOW_WRITE_FIXTURES=true
```

### tier add/edit/remove

```properties
MINIO_LAB_TIER_WRITE_NAME=reactive-lab-tier
MINIO_LAB_TIER_WRITE_CONTENT_TYPE=application/json
MINIO_LAB_ADD_TIER_BODY=<仅属于独立 lab 的 add tier 请求体>
MINIO_LAB_EDIT_TIER_BODY=<可选，仅属于独立 lab 的 edit tier 请求体>
MINIO_LAB_REMOVE_TIER_AFTER_TEST=true
```

测试会先使用 `ReactiveMinioAdminClient` 写入，再使用 `ReactiveMinioRawClient` 的 catalog 路由交叉验证，最后删除该 tier。请求体可能包含远端存储信息，报告只记录是否设置，不输出内容。

### remote target set/remove

```properties
MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET=lab-bucket
MINIO_LAB_REMOTE_TARGET_WRITE_CONTENT_TYPE=application/json
MINIO_LAB_SET_REMOTE_TARGET_BODY=<仅属于独立 lab 的 set remote target 请求体>
MINIO_LAB_REMOVE_REMOTE_TARGET_ARN=<刚写入 target 的 ARN>
MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST=true
```

测试会先通过专用 Admin 客户端设置 target，再通过 raw catalog 验证同一路由的兜底可用性，最后使用 ARN 删除恢复。

只要检测到写入请求体或 remote target 删除 ARN，但没有 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`，`verify-env.sh` 就会拒绝执行。

## 执行报告

`run-destructive-tests.sh` 每次退出都会生成一份本机报告，默认路径为：

```text
target/minio-lab-reports/destructive-lab-<UTC时间>.md
```

也可以通过 `MINIO_LAB_REPORT_DIR` 覆盖目录。报告只记录端点指纹、夹具开关和恢复提示，不写入 access key、secret key 或请求签名。模板见 `scripts/minio-lab/report-template.md`。
