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

`check-crypto-gate.sh` 的通过含义是：阶段 111 的 Crypto Gate Pass 边界仍被正确执行。它会校验 `scripts/madmin-fixtures/crypto-gate-status.properties`、`bcprov-jdk18on:1.82` 依赖、源码 Bouncy Castle import、committed Go fixture 与当前 Go 工具链新生成 fixture。它证明默认 madmin 加密响应可由合法调用方显式提供 `secretKey` 解密，但不代表 SDK 会自动保存或输出敏感明文。

## 3. 破坏性 Admin 门禁

默认发布流程不执行 破坏性 Admin 测试。

只有满足以下条件时，才允许把 destructive 验证列为“已完成”：

- `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
- `scripts/minio-lab/verify-env.sh` 返回 0
- 使用独立、可回滚的实验环境
- 至少一条 config write + restore 流程成功
- 如果执行 tier 或 remote target 真实写入夹具，必须额外设置 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`，并在报告中留下恢复证据。

## 4. 路由对标（route parity）与能力总表门禁

发布或里程碑说明必须引用脚本产物，而不是手写统计值。阶段 46 起，常规复审报告固定刷新 `.omx/reports/route-parity-jdk8.md`、`.omx/reports/route-parity-jdk17.md`、`.omx/reports/capability-matrix.md` 与对应 JSON 文件。阶段 52 起还要同步刷新 `docs/10-version-management.md` 和 `docs/50-stage52-release-review-version-management.md`，明确双分支版本号与是否打 tag。

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
- `scripts/report-destructive-boundary.py` 生成的破坏性边界分类报告

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

阶段 43 起，报告还会渲染 typed/raw 步骤状态文件和 `mc` 只读核验提示。步骤状态只允许记录范围、步骤名、PASS/FAIL 和异常类型，不允许记录请求体、凭证、token、签名或完整异常堆栈。阶段 51 复核确认：没有独立 `lab.properties` 时不能执行真实矩阵；共享端点必须继续被 `verify-env.sh` 拒绝。
阶段 84 起，独立 Docker lab 已成为推荐破坏性验证方式：使用非 9000/9001 端口启动一次性 MinIO，凭证只保存在运行时临时文件中，完成后可删除容器。`run-destructive-tests.sh` 不会再把 `MINIO_LAB_ENDPOINT` 复制到 `MINIO_ENDPOINT`，以免内部 `verify-env.sh` 失去“lab 与共享环境必须隔离”的判断能力。bucket quota 夹具必须使用 madmin-go 字段 `quotatype`，例如 `{"quota":1048576,"quotatype":"hard"}`。config KV 与 bucket quota 的正式 lab 证据必须同时包含 typed 与 raw 步骤 PASS，并保留恢复步骤。


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
5. Crypto Gate Pass 仍被正确执行的证据。
6. 破坏性实验环境仍拒绝共享 MinIO 的证据。
7. 阶段 31 后还要附上 破坏性实验环境报告模板或本机报告生成证据，证明失败恢复提示可追溯。


## 阶段 64 发布工程与外部门禁清单

阶段 64 后，发布候选与正式发布必须分开说明：

- 发布候选：route parity 233 / 233、product-typed 满格、`raw-fallback = 0`，并且 Crypto/lab 风险边界清楚。
- 正式发布：除发布候选证据外，当前还需要独立破坏性 lab 报告、Maven/tag 发布工程材料和回滚策略；Crypto Gate 已在阶段 111 Pass，但仍要保留回归证据。

正式发布前至少补齐：

1. Crypto Gate Pass 回归证据：依赖版本、fixture、JDK8/JDK17/JDK21/JDK25 验证。
2. 独立 lab typed/raw 破坏性矩阵报告。
3. JDK8/JDK17/JDK21/JDK25 构建与测试矩阵。
4. 源码包、javadoc 包、pom 元数据、签名、校验和、许可证/SBOM 或依赖清单。
5. 发布说明、升级指南、已知限制和撤回/修复策略。
6. 双分支工作区干净，提交信息符合 Lore Commit Protocol。

在这些材料齐全前，版本继续保持 `0.1.0-SNAPSHOT`，不得用 route/product-typed 满格替代外部门禁。

## 阶段 65 发布说明与阻塞交接

阶段 65 后，任何对外发布说明都必须同时引用 `docs/63-stage65-release-handoff.md`，并把以下状态分开说明：

- 已完成：route parity 233 / 233、产品化入口满格、专用客户端优先、raw 兜底可用。
- 未放行：独立破坏性 lab、Maven/tag 正式发布工程。Crypto Gate 已 Pass，但每次发布仍要回归。
- 可继续：结果模型深化、中文错误解释、示例和只读旁证。

发布负责人接手时，应先按阶段 65 的阻塞交接表确认 owner、安全、架构、运维或发布负责人是否已经提供外部证据；没有外部证据时，不能降低 `destructive-blocked`；`encrypted-blocked` 已由阶段 111 的 Crypto Gate Pass 证据降为 0。

## 阶段 77/78 破坏性 lab 与发布工程预检

阶段 77 后，发布负责人可以先运行 `scripts/minio-lab/audit-readiness.sh` 做非破坏性准备度审计。该脚本只说明当前配置是否满足启动独立 lab 的门禁，不连接 MinIO，不执行写入测试，也不输出 access key、secret key、请求体或签名。

阶段 78 后，正式 tag/Maven 发布必须继续暂缓，除非同时具备：

1. Crypto Gate Pass 的依赖审查、fixture 和四 JDK 验证证据。
2. 独立破坏性 lab 的 typed/raw 矩阵报告和恢复确认。
3. 发布工程材料：版本号、源码包、javadoc、pom 元数据、签名、校验和、许可证或 SBOM、发布说明、回滚策略。

在这些证据齐全前，版本继续保持 `0.1.0-SNAPSHOT`，不得用 route parity 或 product-typed 满格替代外部门禁。
阶段 80 起，发布负责人还应运行 `scripts/report-pom-release-metadata.py` 生成 POM 发布元数据报告。该报告为预检报告，只能说明哪些字段或插件缺失，不代表 Maven 发布已经放行。许可证、SCM、developers、distributionManagement、签名和 SBOM 策略必须由负责人确认后才能写入 POM。

## 阶段 113 当前发布状态

阶段 114 起，发布复审还必须运行 `scripts/report-destructive-boundary.py`，用机器报告解释 `destructive-blocked` 的每一项分类。

阶段 113 后，当前发布候选状态更新为：

- 已完成：minio-java 主体 API 对标、route parity、product-typed、raw 兜底、Crypto Gate Pass。
- 仍需负责人材料：许可证、SCM、developers、issueManagement、organization、distributionManagement、source/javadoc/sign/SBOM、发布仓库和回滚策略。
- 仍需运维/lab 证据：`destructive-blocked = 29` 中尚未在独立 lab 或维护窗口证明的全量配置、IDP、站点复制变更、服务控制、升级、force-unlock 和压测类操作。

因此正式发布不得再把 Crypto Gate 写成未完成阻塞项，但必须继续把 Crypto Gate Pass 作为回归证据；正式发布仍不能绕过破坏性运维证据和 Maven/tag 发布工程材料。

## 阶段 115 发布元数据负责人输入清单

阶段 115 后，正式 Maven/tag 发布除了引用阶段 113/114 的发布状态和破坏性边界报告，还必须引用 `docs/113-stage115-release-metadata-safe-prep.md`。

发布负责人确认前，以下规则必须保持：

1. 不把版本号改成正式版。
2. 不把猜测的许可证、SCM、developer、organization、issueManagement 或 distributionManagement 写入 `pom.xml`。
3. 不配置真实签名密钥，不在仓库中保存密钥 ID、passphrase、token 或发布仓库凭证。
4. 不执行 `mvn deploy`，不创建 release tag，不推送远端。
5. 每次发布复审都要重新生成 POM 元数据报告和破坏性边界报告。

负责人确认后，才允许按清单分步添加 POM 元数据、source/javadoc/sign/SBOM profile，并先做本地 dry-run；真实 deploy 必须另行取得发布授权。
