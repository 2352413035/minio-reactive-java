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


### 方式三：本机 Docker 一次性 lab

如果本机有 `docker`、`mc` 和 `minio/minio` 镜像，优先使用脚本启动一个不占用共享 `9000/9001` 的一次性 lab：

```bash
scripts/minio-lab/start-docker-lab.sh
MINIO_LAB_CONFIG_FILE=/tmp/minio-reactive-lab-xxxxxx/lab.properties \
  scripts/minio-lab/run-destructive-tests.sh
```

脚本会生成临时凭证、私有 `mc` 配置目录和 `lab.properties`，并只输出临时路径、endpoint 和清理命令；不会把 access key 或 secret key 输出到终端或写入仓库。执行完后删除容器和 `/tmp/minio-reactive-lab-*` 目录即可。

如果需要手工启动，也可以按下面结构运行；真实凭证应使用运行时变量或临时文件，不要写入仓库：

```bash
docker rm -f minio-reactive-destructive-lab 2>/dev/null || true
docker run -d --name minio-reactive-destructive-lab \
  -p 127.0.0.1:19000:9000 \
  -p 127.0.0.1:19001:9001 \
  -e MINIO_ROOT_USER='<运行时生成的 lab 用户>' \
  -e MINIO_ROOT_PASSWORD='<运行时生成的强密码>' \
  minio/minio server /data --console-address ':9001'
```

推荐再用独立 `mc` 配置目录创建测试 bucket，执行完后删除容器或整个临时配置目录。

### 非破坏性准备度审计

如果只是想确认当前 shell 或私有配置文件是否满足门禁，可以先运行：

```bash
scripts/minio-lab/audit-readiness.sh
```

这个脚本只读取配置并复用 `verify-env.sh`，不会连接 MinIO、不会执行写入测试，也不会输出 access key、secret key、请求体或签名。它通过时只表示“可以启动独立 lab 测试”，不代表 typed/raw 破坏性矩阵已经实际通过；真实证据仍然来自 `run-destructive-tests.sh` 生成的本机报告。

如果已经开始准备 tier、remote target、batch job 或 site replication 的本机私有请求体，可以再运行更细的夹具审计：

```bash
scripts/minio-lab/audit-fixtures.sh
```

`audit-fixtures.sh` 不连接 MinIO，只按矩阵列出缺少的变量、恢复开关和模板路径；它不会输出请求体、凭证、token 或签名。

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
4. 使用 `ReactiveMinioRawClient` 调用 `ADMIN_SET_CONFIG_KV` 和 `ADMIN_HELP_CONFIG_KV`，证明 raw 兜底也能处理同一接口；raw 写入体会在测试中显式构造 madmin 加密体。
5. finally 中先尝试 raw 恢复，再使用 `setConfigKvText(MINIO_LAB_RESTORE_CONFIG_KV)` 恢复。
6. 恢复后再次执行只读探测。

如果没有提供这两个变量，真实配置写入测试会跳过；默认 `mvn test` 永远不会修改 MinIO 服务端配置。

### full config 原样写回补证

`ADMIN_SET_CONFIG` 是比单条 KV 更高风险的全量配置接口。阶段 117 起，只有设置 `MINIO_LAB_ALLOW_FULL_CONFIG_WRITE=true` 时，破坏性 lab 才会执行以下闭环：

1. 使用 `getConfigDecrypted(MINIO_LAB_SECRET_KEY)` 读取并解密独立 lab 的原始全量配置。
2. 使用 `setConfigText(...)` 走专用 Admin 客户端原样写回。
3. 使用 raw `ADMIN_SET_CONFIG` 走 catalog 兜底路径原样写回。
4. finally 中继续用原始全量配置文本恢复。

该测试不要求用户提供全量配置文本，也不会把全量配置写入仓库或报告；如果服务端拒绝原样写回，就保持 `ADMIN_SET_CONFIG` 为未通过证据，不伪造成功。

## bucket quota / tier / remote target / batch job lab 夹具

阶段 24 起，破坏性实验环境支持更多可选夹具。它们默认全部跳过，只有在独立 lab 配置文件或环境变量中明确设置后才执行：

```properties
MINIO_LAB_BUCKET=lab-bucket
MINIO_LAB_TEST_BUCKET_QUOTA_JSON={"quota":1048576,"quotatype":"hard"}
MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON={"quota":0,"quotatype":"hard"}

MINIO_LAB_TIER_NAME=archive
MINIO_LAB_EXPECT_TIER_IN_LIST=false
MINIO_LAB_REMOTE_TARGET_TYPE=replication
MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN=
MINIO_LAB_ENABLE_BATCH_JOB_PROBES=false
MINIO_LAB_BATCH_EXPECTED_JOB_ID=
```

- bucket quota 写入必须同时设置测试值和恢复值，并且 finally 中总是尝试恢复。bucket quota 写入会同时覆盖 typed 与 raw：专用客户端负责用户友好入口，raw 负责 catalog 兜底入口。
- tier、remote target、batch job 只做仅实验环境 typed/raw 双路径探测，不进入共享 live 门禁。
- `MINIO_LAB_EXPECT_TIER_IN_LIST=true` 时，`listTiers()` typed 摘要必须能看到 `MINIO_LAB_TIER_NAME`。
- `MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN` 不为空时，`listRemoteTargetsInfo(...)` typed 摘要必须能看到该 ARN。
- `MINIO_LAB_BATCH_EXPECTED_JOB_ID` 不为空时，batch job typed 摘要或 raw JSON 中必须能看到该任务 ID。

### replication diff 探测

`ADMIN_REPLICATION_DIFF` 与 madmin 的 `BucketReplicationDiff` 一致：选项通过 query 传递，主要包括 `verbose`、`prefix` 和 `arn`，不需要请求体。阶段 118 起，SDK 的专用 Admin 客户端新增无请求体重载，并在 lab 中提供可选 typed/raw 探测：

```properties
MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE=true
MINIO_LAB_REPLICATION_DIFF_PREFIX=可选前缀
MINIO_LAB_REPLICATION_DIFF_ARN=可选目标ARN
```

这个探测要求 `MINIO_LAB_BUCKET` 已经具备真实复制配置。普通单节点 Docker lab 如果没有 bucket replication 规则，服务端可能返回错误；这种失败只能说明环境未满足复制差异扫描条件，不能用来降低 `destructive-blocked`。
- `verify-env.sh` 仍会拒绝共享端点 和常见本机默认端点。

## tier / remote target 可回滚写入夹具

阶段 36 起，独立 lab 可以额外开启真实写入夹具。它们比只读探测更危险，因此必须先打开总开关：

```properties
MINIO_LAB_ALLOW_WRITE_FIXTURES=true
```

### tier add/edit/remove

```properties
MINIO_LAB_TIER_WRITE_NAME=REACTIVE-LAB-TIER
MINIO_LAB_TIER_WRITE_CONTENT_TYPE=application/json
MINIO_LAB_ADD_TIER_BODY=<仅属于独立 lab 的 add tier 请求体>
MINIO_LAB_ADD_TIER_BODY_FILE=<可选，本机私有请求体文件>
MINIO_LAB_EDIT_TIER_BODY=<可选，仅属于独立 lab 的 edit tier 请求体>
MINIO_LAB_EDIT_TIER_BODY_FILE=<可选，本机私有请求体文件>
MINIO_LAB_REMOVE_TIER_AFTER_TEST=true
```

测试会先使用 `ReactiveMinioAdminClient` 写入，再使用 `ReactiveMinioRawClient` 的 catalog 路由交叉验证，最后删除该 tier。请求体可能包含远端存储信息，报告只记录是否设置，不输出内容。

可复制 `scripts/minio-lab/templates/tier-add-minio.json.example` 和 `scripts/minio-lab/templates/tier-edit-creds.json.example` 到仓库外私有目录后填写。tier 名称必须大写；MinIO 类型 tier 的 endpoint 要从源 MinIO 服务端视角填写可访问 URL，Docker 场景下通常不是宿主机映射端口。`edit` 请求体是可选项，按 madmin-go `TierCreds` 语义只写 `access` / `secret` 等凭据字段；阶段 110 已用同一组 lab 凭据验证 typed/raw edit 路径。

### remote target set/remove

```properties
MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET=lab-bucket
MINIO_LAB_REMOTE_TARGET_WRITE_CONTENT_TYPE=application/json
MINIO_LAB_SET_REMOTE_TARGET_BODY=<仅属于独立 lab 的 set remote target 请求体>
MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE=<可选，本机私有请求体文件>
MINIO_LAB_REMOVE_REMOTE_TARGET_ARN=<可选；set 响应无法解析 ARN 时才需要手工提供>
MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST=true
```

测试会先通过专用 Admin 客户端设置 target，再通过 raw catalog 验证同一路由的兜底可用性，最后使用 ARN 删除恢复。

可复制 `scripts/minio-lab/templates/remote-target-set-replication.json.example` 到仓库外私有目录后填写。请求体内必须包含只属于本次 lab 的 `arn`，并且 `endpoint` 按 madmin 语义填写 `host:port`，不要带协议前缀。SDK 会优先使用 set 响应返回的 ARN 做删除恢复；如果目标 MinIO 没有返回 ARN，才需要把可安全删除的独立 lab ARN 写入 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN`。

只要检测到写入请求体或 remote target 删除 ARN，但没有 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`，`verify-env.sh` 就会拒绝执行。

## batch job / site replication 实验矩阵

阶段 37 起，独立 lab 可以为 batch job 和 site replication 建立更完整的实验矩阵。它们默认跳过，仍然必须复用写入总开关。

### batch job start/status/cancel

```properties
MINIO_LAB_BATCH_JOB_CONTENT_TYPE=application/yaml
MINIO_LAB_BATCH_START_BODY_FILE=/secure/path/batch-start-job.yaml
MINIO_LAB_CANCEL_BATCH_AFTER_TEST=true
```

测试会使用专用 Admin 客户端启动任务并读取状态，再按 MinIO madmin 语义从 start 响应解析 jobId，然后使用 raw catalog 的 `ADMIN_CANCEL_BATCH_JOB?id=<jobId>` 取消任务；finally 中还会再次尝试专用客户端 `cancelBatchJob(jobId)`，降低残留任务风险。请求模板见 `scripts/minio-lab/templates/batch-start-job.yaml.example`；`batch-cancel-job.yaml.example` 仅保留为旧式/人工排错说明。

batch job 模板中的 bucket、prefix、target endpoint 和 target credentials 必须按独立 lab 的实际资源填写；如果当前镜像无法稳定启动或取消，不应伪造通过证据，应把错误写入阶段文档。

### site replication add/edit/remove

```properties
MINIO_LAB_SITE_REPLICATION_CONTENT_TYPE=application/json
MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE=/secure/path/site-replication-add.json
MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE=/secure/path/site-replication-edit.json
MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE=/secure/path/site-replication-remove.json
MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true
```

测试会使用专用 Admin 客户端新增站点复制配置，读取 info/status/metainfo 做 typed 复核，然后用 raw catalog 执行 remove；阶段 109 起还会在 raw remove 后再次用 raw add 复建一次拓扑，最后用专用客户端 remove 兜底恢复。请求模板见 `scripts/minio-lab/templates/site-replication-*.json.example`。

site replication add 请求体必须对齐 madmin-go 的 `PeerSite[]` 数组，字段名是 `endpoints`；两个站点都必须使用源 MinIO 服务端可访问的 endpoint。Docker 场景下通常使用同一 Docker 网络里的容器名，例如 `http://lab-site-b-minio:9000`，而不是宿主机映射端口。remove 的最小 lab 恢复体可使用 `{"all": true}`。

site replication 通常需要至少两个彼此隔离、可删除的 lab 站点；单节点临时容器只能准备模板，不能证明完整复制矩阵。

## 执行报告

`run-destructive-tests.sh` 每次退出都会生成一份本机报告，默认路径为：

```text
target/minio-lab-reports/destructive-lab-<UTC时间>.md
```

也可以通过 `MINIO_LAB_REPORT_DIR` 覆盖目录。报告只记录端点指纹、夹具开关和恢复提示，不写入 access key、secret key 或请求签名。模板见 `scripts/minio-lab/report-template.md`。

阶段 43 起，`run-destructive-tests.sh` 会为每次执行生成 typed/raw 步骤状态文件，并在报告中渲染“哪个专用客户端步骤通过、哪个 raw 兜底步骤通过或失败”。步骤文件只记录范围、步骤名、PASS/FAIL 和异常类型，不记录请求体、凭证、token 或签名。

如果系统安装了 `mc`，报告还会给出只读恢复核验提示。推荐在本机预先配置一个只指向独立 lab 的 alias，并通过 `MINIO_LAB_MC_ALIAS` 提供 alias 名称；不要把 `mc alias set` 命令或任何凭证写入仓库。
