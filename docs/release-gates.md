# 发布门禁

本项目在对外标记里程碑或准备发布前，至少要通过以下门禁。

## 1. 基础验证

- JDK8：`mvn -q -DfailIfNoTests=true test`
- JDK17：`mvn -q -DfailIfNoTests=true test`
- JDK8 / JDK17 real MinIO：`LiveMinioIntegrationTest`
- JDK21 / JDK25：JDK17+ compile
- `git diff --check`
- secret scan

阶段 19 发布收口还要求 main examples 可编译，因为示例是用户理解 SDK 分层的第一入口。

## 2. Go 互操作门禁

如果当前里程碑涉及 madmin 加密兼容，必须额外通过：

- `scripts/madmin-fixtures/verify-fixtures.sh`
- `scripts/madmin-fixtures/crypto-gate-status.properties`
- `scripts/madmin-fixtures/check-crypto-gate.sh`
- 固定 `go version`
- 固定 `madmin-go` 版本
- 夹具元数据 与文档一致

`check-crypto-gate.sh` 的通过含义是：当前 Gate Fail 边界仍被正确执行。阶段 32 起，它还会校验 `scripts/madmin-fixtures/crypto-gate-status.properties` 中的三方批准状态。它会确认 夹具可用，并检查 `pom.xml` 与源码 import 没有引入未批准 crypto/native 依赖。它不代表默认 madmin 加密响应已经可以明文 typed 解析。

## 3. 破坏性 Admin 门禁

默认发布流程不执行 破坏性 Admin 测试。

只有满足以下条件时，才允许把 destructive 验证列为“已完成”：

- `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
- `scripts/minio-lab/verify-env.sh` 返回 0
- 使用独立、可回滚的实验环境
- 至少一条 config write + restore 流程成功
- 如果执行 tier 或 remote target 真实写入夹具，必须额外设置 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`，并在报告中留下恢复证据。

## 4. 路由对标（route parity）与能力总表门禁

发布或里程碑说明必须引用脚本产物，而不是手写统计值。

路由对标必须输出：

- `scripts/report-route-parity.py` 的 markdown 或 json 报告。
- 服务端 router 与 SDK catalog 的 family/method/path/query/auth 差异。
- `missingFromCatalog = 0` 且 `extraInCatalog = 0`，除非文档明确说明该路由是内部、dummy 或 rejected route。

能力总表至少输出：

- `route-catalog`
- `product-typed`
- `advanced-compatible`
- `raw-fallback`
- `encrypted-blocked`
- `destructive-blocked`

## 5. 文档门禁

- README 必须明确 typed 优先、advanced 过渡、raw 兜底。
- 中文文档必须明确当前 crypto 边界。
- 示例必须覆盖对象存储主路径、Admin typed 路径、raw 兜底路径、Metrics/Health 运维入口路径以及 KMS/STS 安全入口路径。
- 发布说明不得使用单一百分比宣称“完成 MinIO”，必须分 route parity、callability、typed maturity、live/destructive/crypto 边界说明。

## 6. 审查门禁

- `architect`：架构边界、双分支语义一致性
- `security-reviewer`：涉及 crypto 依赖或敏感响应时必须审查
- `verifier`：验证证据与能力总表闭环


## 破坏性 Admin 实验环境

破坏性 Admin 测试必须通过 `scripts/minio-lab/verify-env.sh`，并且只能由 `scripts/minio-lab/run-destructive-tests.sh` 在独立可回滚环境中启动。默认 `mvn test` 只会跳过 破坏性用例，不允许修改共享 MinIO。

真实 config write + restore 需要额外提供 `MINIO_LAB_TEST_CONFIG_KV` 与 `MINIO_LAB_RESTORE_CONFIG_KV`。这两个变量缺失时，测试只验证环境门禁，不执行配置写入。

阶段 24 起，破坏性实验环境推荐使用 `MINIO_LAB_CONFIG_FILE` 或本地 `scripts/minio-lab/lab.properties` 集中声明 lab 参数。仓库只提供 `lab.example.properties`，真实凭证不得提交。

可选 lab 夹具包括：

- `MINIO_LAB_TEST_BUCKET_QUOTA_JSON` / `MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON`
- `MINIO_LAB_TIER_NAME`
- `MINIO_LAB_EXPECT_TIER_IN_LIST`
- `MINIO_LAB_REMOTE_TARGET_TYPE`
- `MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN`
- `MINIO_LAB_ENABLE_BATCH_JOB_PROBES`
- `MINIO_LAB_BATCH_EXPECTED_JOB_ID`
- `MINIO_LAB_ALLOW_WRITE_FIXTURES`
- `MINIO_LAB_TIER_WRITE_NAME`
- `MINIO_LAB_ADD_TIER_BODY`
- `MINIO_LAB_EDIT_TIER_BODY`
- `MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET`
- `MINIO_LAB_SET_REMOTE_TARGET_BODY`
- `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN`
- `MINIO_LAB_BATCH_START_BODY` / `MINIO_LAB_BATCH_START_BODY_FILE`
- `MINIO_LAB_BATCH_CANCEL_BODY` / `MINIO_LAB_BATCH_CANCEL_BODY_FILE`
- `MINIO_LAB_SITE_REPLICATION_ADD_BODY` / `MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE`
- `MINIO_LAB_SITE_REPLICATION_EDIT_BODY` / `MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE`
- `MINIO_LAB_SITE_REPLICATION_REMOVE_BODY` / `MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE`

阶段 31 起，tier、remote target、batch job 夹具必须同时覆盖专用 typed 客户端和 `ReactiveMinioRawClient` catalog 兜底调用，证明“方便使用”和“灵活兜底”两条路径都可运行。这些夹具只能在独立 lab 中证明能力，不允许替代共享 live 门禁。

阶段 36 起，tier 与 remote target 写入夹具必须先通过写入总开关，并在 finally 或报告恢复提示中保留删除路径。报告只允许记录请求体是否设置，不允许记录请求体内容。

阶段 37 起，batch job 与 site replication 实验矩阵只能引用本机私有请求体或环境变量，发布证据必须包含 start/cancel 或 add/remove 的恢复路径；不能把站点复制拓扑或 batch job 真实参数写入仓库。

阶段 43 起，报告还会渲染 typed/raw 步骤状态文件和 `mc` 只读核验提示。步骤状态只允许记录范围、步骤名、PASS/FAIL 和异常类型，不允许记录请求体、凭证、token、签名或完整异常堆栈。

`run-destructive-tests.sh` 每次退出都会生成本机报告，默认写入 `target/minio-lab-reports/`，也可以通过 `MINIO_LAB_REPORT_DIR` 覆盖。报告只记录环境指纹、夹具开关、typed/raw 步骤结果和恢复提示，不得包含 access key、secret key、session token 或请求签名。

## 阶段 19 发布就绪检查清单

发布或里程碑说明至少要附上以下证据：

1. `.omx/reports/route-parity-jdk8.md` 与 `.omx/reports/route-parity-jdk17.md` 均显示 catalog 缺失 0、额外 0。
2. `.omx/reports/capability-matrix.md` 显示双分支能力矩阵一致。
3. `docs/17-release-readiness-report.md` 记录当前完成度、风险边界、验证命令和下一阶段计划入口。
4. README、`docs/09-minio-api-catalog.md`、`docs/13-admin-risk-levels.md`、`docs/14-typed-client-usage-guide.md`、`docs/16-crypto-boundary-map.md` 的口径一致。
5. 示例类全部位于 `io.minio.reactive.examples` 包下，并通过 `mvn test` 的 main compile 阶段。

## 阶段 26 发布候选检查清单

阶段 26 之后，如果要对外说明当前 SDK 状态，必须同时引用：

1. `CHANGELOG.md` 的 `0.1.0-SNAPSHOT 阶段 26 发布候选`。
2. `docs/24-stage26-release-closeout.md` 的能力快照和剩余风险。
3. 双分支 route parity 报告。
4. 双分支 capability matrix。
5. Crypto Gate Fail 仍被正确执行的证据。
6. 破坏性实验环境仍拒绝共享 MinIO 的证据。
7. 阶段 31 后还要附上 破坏性实验环境报告模板或本机报告生成证据，证明失败恢复提示可追溯。
