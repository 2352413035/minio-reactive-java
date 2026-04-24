# 变更日志

本文件记录 SDK 里程碑级变化。当前项目仍处于 `0.1.0-SNAPSHOT`，阶段 26 是“对标 MinIO 路由完整、调用入口完整、风险边界明确”的发布候选收口，不等同于 1.0 稳定版。

## 阶段 70 Admin batch job 只读模型补充

- 新增 `AdminBatchJobStatusSummary` 与 `AdminBatchJobDescriptionSummary` 两个只读摘要模型。
- `ReactiveMinioAdminClient` 新增带 `jobId` 的 `batchJobStatus(jobId)`、`getBatchJobStatusInfo(jobId)`、`getBatchJobStatusSummary(jobId)`、`describeBatchJob(jobId)` 与 `describeBatchJobSummary(jobId)`。
- 原无参 `batchJobStatus()`、`describeBatchJob()` 与通用包装继续保留，避免破坏已有编译。
- 新增 `docs/68-stage70-admin-batch-job-readonly-models.md` 记录 jobId 重载、YAML 描述摘要和 start/cancel 写入不放行策略。

## 阶段 69 Admin pool 只读模型补充

- 新增 `AdminPoolListSummary` 与 `AdminPoolStatusSummary` 两个只读摘要模型。
- `ReactiveMinioAdminClient` 新增 `listPoolsSummary()` 与 `getPoolStatusSummary(pool)`。
- 原 `listPoolsInfo()` 与 `getPoolStatus(pool)` 继续保留通用 JSON 入口。
- 新增 `docs/67-stage69-admin-pool-readonly-models.md` 记录只读边界、decommission 写入不放行策略和验证口径。

## 阶段 68 站点复制只读模型补充

- 新增 `AdminSiteReplicationInfoSummary`、`AdminSiteReplicationStatusSummary`、`AdminSiteReplicationMetaInfoSummary` 三个只读摘要模型。
- `ReactiveMinioAdminClient` 新增 `getSiteReplicationInfoSummary()`、`getSiteReplicationStatusSummary()`、`getSiteReplicationMetainfoSummary()`。
- 原 `getSiteReplicationInfo()`、`getSiteReplicationStatus()`、`getSiteReplicationMetainfo()` 继续保留通用 JSON 入口。
- 新增 `docs/66-stage68-site-replication-readonly-models.md` 记录只读边界、服务账号 access key 不暴露策略和验证口径。

## 阶段 67 Admin 诊断模型补充

- 新增 `AdminTopLocksSummary`，并在 `ReactiveMinioAdminClient` 增加 `getTopLocksSummary()`。
- 新增 `AdminHealthInfoSummary`，并在 `ReactiveMinioAdminClient` 增加 `getObdInfoSummary()` 与 `getHealthInfoSummary()`。
- 原 `getTopLocksInfo()`、`getObdInfo()`、`getHealthInfo()` 继续保留通用 JSON 入口，方便读取完整响应。
- 新增 `docs/65-stage67-admin-diagnostic-models.md` 记录只读诊断模型的设计边界和验证口径。

## 阶段 66 Admin 状态模型补充

- 新增 `AdminBackgroundHealStatus`、`AdminRebalanceStatus`、`AdminTierStatsSummary` 三个只读状态/统计摘要模型。
- `ReactiveMinioAdminClient` 新增 `getBackgroundHealStatusSummary()`、`getRebalanceStatusSummary()`、`getTierStatsSummary()`，原通用 JSON 方法继续保留。
- 修正 `AdminJsonResult` 对顶层数组 JSON 的兼容能力，数组响应会放入 `values().get("items")`。
- 新增 `docs/64-stage66-admin-status-models.md` 说明本阶段设计边界和验证口径。

## 阶段 65 发布交接补充

- 新增 `docs/63-stage65-release-handoff.md`，把当前发布候选说明、外部门禁和正式发布前交接事项整理为可执行清单。
- 明确阶段 65 不新增 API、不改变版本号、不打 tag、不发布 Maven；当前仍是 `0.1.0-SNAPSHOT`。
- 继续保持 Crypto Gate Fail、独立破坏性 lab 未放行、`encrypted-blocked = 9`、`destructive-blocked = 29` 的边界。

## 0.1.0-SNAPSHOT 阶段 26 发布候选



### 阶段 36 补充

- 破坏性实验环境新增 tier add/edit/remove 与 remote target set/remove 的可回滚写入夹具。
- `verify-env.sh` 新增 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true` 门禁，检测到写入请求体或 remote target 删除 ARN 时会拒绝未确认的执行。
- `DestructiveAdminIntegrationTest` 对写入夹具同时覆盖 `ReactiveMinioAdminClient` 专用入口和 `ReactiveMinioRawClient` catalog 兜底入口。
- 本机 lab 报告新增写入夹具开关、请求体设置状态和失败恢复提示；报告仍不输出凭证、请求体或签名。

### 阶段 37 补充

- 破坏性实验环境新增 batch job start/status/cancel 与 site replication add/edit/remove 实验矩阵。
- 新增本机私有请求体文件变量，支持用 `*_BODY_FILE` 引用 YAML/JSON 模板，避免把多行请求体写入仓库。
- 报告新增 batch/site replication 矩阵开关、请求体设置状态和恢复提示。
- 新增 `scripts/minio-lab/templates/` 示例模板和 `docs/35-stage37-batch-site-replication-lab-matrix.md`。

### 阶段 38 补充

- 删除 `TestCreateBucket` 与 `TestGetBucketLocation` 两个临时示例类。
- 新增 `ReactiveMinioSecurityExample`，覆盖 KMS 状态检查和 STS 临时凭证申请。
- README 示例入口补齐对象存储、Admin typed、Raw 兜底、Metrics/Health、KMS/STS 五类正式示例。
- 新增 `docs/36-stage38-examples-ux-closeout.md` 记录示例矩阵和错误解释原则。

### 阶段 39 补充

- 新增 `docs/37-stage39-release-candidate-review.md`，复审阶段 26 之后的持续增强和剩余边界。
- 更新 `docs/17-release-readiness-report.md` 与 `docs/24-stage26-release-closeout.md`，把阶段 36-38 的 lab、示例和能力矩阵变化纳入发布口径。
- 当前复审结论仍保持：双分支 route parity 233 / 233，`raw-fallback = 0`，Admin 剩余重点是 typed 成熟度、独立 lab 真实证据和 Crypto Gate Pass。

### 阶段 40 补充

- `ReactiveMinioAdminClient` 新增 Admin 诊断类产品入口：`scrapeAdminMetrics()`、`downloadInspectData(...)`、`startProfiling(...)`、`downloadProfilingData()`、`getProfileResult(...)`。
- 新增 `AdminTextResult` 与 `AdminBinaryResult`，分别固定文本诊断和二进制诊断边界。
- Admin product-typed 口径从 55 / 128 提升到 60 / 128；加密和破坏性边界不变。
- 新增 `docs/38-stage40-admin-diagnostics-typed-wrappers.md` 记录使用建议和风险边界。

### 阶段 41 补充

- `ReactiveMinioAdminClient` 新增 LDAP 策略实体和 LDAP/OpenID access key 只读摘要入口。
- 新增 `AdminAccessKeySummary` 与 `AdminAccessKeySummaryList`，故意不保存 raw JSON，避免 secret、session token 或私钥泄漏到普通模型。
- Admin product-typed 口径从 60 / 128 提升到 63 / 128；加密和破坏性边界不变。
- 新增 `docs/39-stage41-admin-iam-idp-readonly.md` 记录敏感字段处理原则。

### 阶段 42 补充

- `ReactiveMinioAdminClient` 新增 `getSiteReplicationPeerIdpSettings()`，用于读取站点复制 peer 的 IDP 设置安全摘要。
- 新增 `AdminSiteReplicationPeerIdpSettings`，只暴露 LDAP/OpenID 是否启用、LDAP 搜索条件、OpenID 区域和角色数量，不保存 raw JSON，避免 OIDC 哈希密钥等字段进入普通模型。
- 对照 madmin-go，为 site replication 相关专用客户端入口补齐 `api-version=1` 查询参数。
- Admin product-typed 口径从 63 / 128 提升到 64 / 128；加密和破坏性边界不变。
- 新增 `docs/40-stage42-site-replication-peer-idp.md` 记录站点复制 peer 只读摘要边界。

### 阶段 43 补充

- 破坏性 lab 报告新增 typed/raw 步骤状态文件和执行明细表，能记录专用客户端步骤、raw 兜底步骤各自 PASS/FAIL。
- `run-destructive-tests.sh` 自动生成 `MINIO_LAB_RUN_ID` 与 `MINIO_LAB_STEP_STATUS_FILE`，`DestructiveAdminIntegrationTest` 在关键 lab 步骤写入状态。
- `write-report.sh` 新增 `mc` 恢复/核验提示，支持通过 `MINIO_LAB_MC_ALIAS` 显示本机私有 alias 命令；仍不保存凭证。
- 新增 `docs/41-stage43-destructive-lab-evidence.md` 记录真实 lab 证据增强边界。

### 阶段 44 补充

- `ReactiveMinioException` 的默认异常消息改为中文，继续保留协议、HTTP 状态、错误码、requestId、endpoint、method、path 和诊断建议字段。
- raw 兜底请求构造的本地校验错误改为中文，包括缺少 query、危险 header、路径变量非法等场景。
- `ReactiveMinioRawClientTest` 新增纯文本错误和本地校验中文断言，防止后续退回英文低上下文错误。
- 新增 `docs/42-stage44-error-experience.md` 记录异常体验边界。

### 阶段 45 补充

- Crypto Gate Pass 继续只做准备，不修改 `pom.xml`，不新增 crypto/native/provider 依赖。
- 新增 `docs/43-stage45-crypto-gate-pass-prep.md`，把候选依赖、三方批准材料、JDK8/JDK17/JDK21/JDK25 测试矩阵和失败回退语义写成后续放行清单。
- `crypto-gate-status.properties` 继续保持 `fail`，owner/security/architect 批准状态仍为 `false`。
- `encrypted-blocked = 9` 继续保留，默认 madmin 加密响应仍通过 `EncryptedAdminResponse` 暴露算法诊断边界。

### 阶段 46 补充

- 重新生成双分支 route parity 和 capability matrix 报告：路由对标仍为 233 / 233，catalog 缺失 0、额外 0。
- 当前能力矩阵保持 S3 77 / 77、Admin 64 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8，`raw-fallback = 0`。
- 新增 `docs/44-stage46-release-review-refresh.md`，汇总阶段 40-45 的真实增量、发布边界和验证命令。
- 发布口径继续禁止用单一百分比宣称完成，必须拆分 route parity、callability、typed maturity、live/destructive/crypto 边界。

### 阶段 47 补充

- `ReactiveMinioAdminClient` 新增 `exportIamData()` 与 `exportBucketMetadataData()`，返回 `AdminBinaryResult`。
- IAM 与 bucket metadata 导出不再建议走字符串产品路径，避免二进制备份包被错误解码或写入日志。
- 单元测试同时验证专用客户端和 raw `executeToBytes(...)` 路径。
- Admin product-typed 从 64 / 128 提升到 66 / 128；加密和破坏性边界不变。
- 新增 `docs/45-stage47-admin-sensitive-export.md` 记录敏感导出使用边界。

### 阶段 48 补充

- `ReactiveMinioAdminClient` 新增 client devnull、site replication devnull/netperf 与 speedtest 系列 `AdminTextResult` 产品入口。
- 这些入口只包装诊断文本，不在共享 live 测试中执行真实压测，并要求调用方自行控制维护窗口、超时和日志。
- 单元测试同时验证专用入口和 raw `executeToString(...)` 路径。
- Admin product-typed 从 66 / 128 提升到 75 / 128；加密和破坏性边界不变。
- 新增 `docs/46-stage48-admin-diagnostic-probes.md` 记录诊断/压测/探测接口边界。

### 阶段 49 补充

- 明确 KMS 普通业务优先使用 `ReactiveMinioKmsClient`，Admin KMS 只作为 `/minio/admin/v3/kms/...` 的 madmin 兼容桥接路径。
- `ReactiveMinioAdminClient` 新增 `getAdminKmsStatus()`、`createAdminKmsKey(...)`、`getAdminKmsKeyStatus(...)` typed 桥接方法。
- 旧的 Admin KMS `Mono<String>` advanced 入口标记 `@Deprecated`，保留二进制兼容但不再推荐。
- 单元测试同时验证 Admin KMS 桥接、专用 KMS 客户端和 raw 兜底路径。
- Admin product-typed 从 75 / 128 提升到 78 / 128。
- 新增 `docs/47-stage49-admin-kms-boundary.md` 记录 KMS 客户端选择边界。

### 阶段 50 补充

- `ReactiveMinioAdminClient` 新增 `importIamArchive(...)`、`importIamV2Archive(...)`、`importBucketMetadataArchive(...)`，返回 `AdminTextResult`。
- 导入类接口被明确为独立 lab/维护窗口能力，不在共享 live 测试中真实执行。
- 旧的 import IAM / import bucket metadata `Mono<String>` advanced 入口标记 `@Deprecated`，迁移到带 archive 命名的产品入口。
- 单元测试验证专用入口、content type、空 archive 拦截和 raw 兜底路径。
- Admin product-typed 从 78 / 128 提升到 81 / 128；破坏性边界不减少。
- 新增 `docs/48-stage50-sensitive-import-lab-boundary.md` 记录导入恢复边界。

### 阶段 51 补充

- 复核破坏性 Admin 独立 lab 执行窗口：当前没有本机 `lab.properties`，因此不执行真实写入矩阵。
- `verify-env.sh` 双分支确认缺少显式开关会失败，共享端点 `http://127.0.0.1:9000` 也会被拒绝。
- 确认 `mc` 已安装，可用于后续独立 lab 的只读恢复核验提示。
- `destructive-blocked = 29` 不减少；本阶段只证明门禁安全，不宣称真实破坏性能力通过。
- 新增 `docs/49-stage51-independent-lab-window.md` 记录本次安全复核。

### 阶段 52 补充

- 重新生成双分支 route parity 与 capability matrix 报告，route parity 仍为 233 / 233，catalog 缺失 0、额外 0。
- 当前产品 typed 成熟度刷新为 S3 77 / 77、Admin 81 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8，`raw-fallback = 0`。
- 版本管理口径继续保持 JDK8 `master` 与 JDK17+ `chore/jdk17-springboot3` 双线同步，Maven 版本仍为 `0.1.0-SNAPSHOT`。
- 不打正式 tag，不减少 `encrypted-blocked = 9` 或 `destructive-blocked = 29`。
- 新增 `docs/50-stage52-release-review-version-management.md` 记录发布复审、版本管理和验证证据。

### 阶段 53 补充

- `ReactiveMinioAdminClient` 新增 root/bucket/prefix heal 的 `AdminTextResult` 产品入口。
- 新增 pool decommission start/cancel 与 rebalance start/stop 的维护操作产品入口。
- 这些接口可能消耗资源或改变维护状态，因此共享 live 测试只保留普通集成验证，维护操作使用 mock/raw 交叉验证。
- Admin product-typed 从 81 / 128 提升到 88 / 128；Crypto Gate 与破坏性 lab 边界不变。
- 新增 `docs/51-stage53-admin-maintenance-boundary.md` 记录维护窗口和验证边界。

### 阶段 54 补充

- `ReactiveMinioAdminClient` 新增 `getReplicationMrfInfo(...)` 与 `verifyTierInfo(...)` 产品入口。
- 新增内置策略和 LDAP 策略 attach/detach 语义化入口，避免用户直接传底层 `operation` 字符串。
- 策略变更入口要求非空请求体，SDK 不保存请求体中的用户、组、策略或身份源内容。
- Admin product-typed 从 88 / 128 提升到 94 / 128；Crypto Gate 与破坏性 lab 边界不变。
- 新增 `docs/52-stage54-admin-policy-replication-boundary.md` 记录轻量写入和只读探测边界。

### 阶段 55 补充

- `ReactiveMinioAdminClient` 新增配置 KV 删除、配置历史清理和配置历史恢复的 `AdminTextResult` 产品入口。
- 配置删除入口要求非空请求体；配置历史入口要求明确 `restoreId`。
- 这些接口不读取、不保存真实配置值，不在共享 live 中真实执行。
- Admin product-typed 从 94 / 128 提升到 97 / 128；Crypto Gate 与破坏性 lab 边界不变。
- 新增 `docs/53-stage55-admin-config-risk-boundary.md` 记录配置高风险边界。

### 阶段 56 补充

- `ReactiveMinioAdminClient` 新增站点复制 peer join、bucket ops、IAM item、bucket metadata、resync、state edit 的 lab-only 产品入口。
- 这些入口固定 madmin `api-version=1`，要求非空请求体，不解析、不保存请求体内容。
- mock 测试同时验证 typed 方法和 raw catalog 兜底路径。
- Admin product-typed 从 97 / 128 提升到 103 / 128；Crypto Gate 与破坏性 lab 边界不变。
- 新增 `docs/54-stage56-site-replication-peer-lab-boundary.md` 记录站点复制 peer 写入边界。

### 阶段 57 补充

- `ReactiveMinioAdminClient` 新增服务控制、v2 服务控制、服务端升级、v2 服务端升级和 token 吊销的 `AdminTextResult` 产品入口。
- 这些入口属于强破坏性维护能力，只做 mock/raw 交叉验证，不在共享 live 中真实执行。
- Admin product-typed 从 103 / 128 提升到 108 / 128；Crypto Gate 与破坏性 lab 边界不变。
- 新增 `docs/55-stage57-service-update-token-boundary.md` 记录服务类高风险边界。

### 阶段 58 补充

- 复核 Crypto Gate 与独立 lab 阻塞状态，继续保持 Gate Fail 和共享端点拒绝。
- 能力矩阵把既有 `EncryptedAdminResponse` 产品边界纳入 product-typed 统计，包括配置、access key 和配置历史加密响应入口。
- Admin product-typed 从 108 / 128 提升到 113 / 128；`encrypted-blocked = 9` 与 `destructive-blocked = 29` 不减少。
- 新增 `docs/56-stage58-crypto-lab-blocker-review.md` 记录为什么这是统计修正而不是 Crypto Gate Pass。

### 阶段 59 补充

- `ReactiveMinioAdminClient` 新增剩余 Admin 高风险/lab-only 产品入口，覆盖 IDP 配置、LDAP service account、bucket quota、remote target、replication diff、batch job、tier、site replication peer 和 force-unlock。
- 这些入口与已有 typed 客户端平级，不依赖 raw 作为底层语义；raw 仍通过测试交叉佐证通用兜底能力。
- Admin product-typed 从 113 / 128 提升到 128 / 128；`raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 与 `destructive-blocked = 29` 不减少，真实破坏性写入仍必须走独立 lab。
- 新增 `docs/57-stage59-admin-lab-risk-boundaries.md` 记录边界、验证和后续深化方向。

### 阶段 60 补充

- 重新生成双分支 route parity 与 capability matrix，确认 route parity 233 / 233、Admin product-typed 128 / 128、`raw-fallback = 0`。
- 重新执行双分支单元测试、真实 MinIO smoke、Crypto Gate、破坏性 lab 拒绝、JDK21/JDK25 编译和凭证扫描。
- 明确当前发布候选状态：公开路由、调用入口、产品边界已闭环；Crypto Gate 与独立 lab 仍是外部门禁。
- 新增 `docs/58-stage60-release-candidate-final-review.md` 保存阶段 60 复审结论。

### 阶段 61 补充

- 使用系统已安装的 `mc` 执行只读命令，补充共享 MinIO 在线、healthy、根路径可列、Admin info 可读的外部旁证。
- `mc` 连接信息只通过运行时环境变量注入，仓库文档和报告不记录真实 access key、secret key 或页面登录密码。
- 新增 `docs/59-stage61-mc-readonly-evidence.md` 记录只读证据摘要和边界说明。
- `encrypted-blocked = 9`、`destructive-blocked = 29` 保持不变。

### 阶段 62 补充

- 新增 `docs/60-stage62-user-facing-release-guide.md`，把用户面使用路径收口为平级专用客户端优先、raw 兜底、风险边界不夸大。
- `docs/14-typed-client-usage-guide.md` 增加快速选择说明，让普通集成方先知道该用哪个客户端。
- 版本口径仍保持 `0.1.0-SNAPSHOT`，不因为 route/product-typed 满格就移除 Crypto Gate 或破坏性 lab 门禁。

### 阶段 63 补充

- 新增 `docs/61-stage63-final-gap-audit.md`，完成最终缺口审计。
- 审计结论：当前没有公开路由缺口、产品入口缺口或 raw-only 缺口；剩余工作是 Crypto Gate、独立破坏性 lab、结果模型深化和发布工程。
- 修正发布复审后续建议，避免继续把工作描述成“补 Admin 入口”。

### 阶段 64 补充

- 新增 `docs/62-stage64-release-engineering-gates.md`，明确正式发布前的 Crypto Gate、独立 lab、Maven/tag 发布工程清单。
- 更新 `docs/release-gates.md`，补充阶段 64 后发布候选与正式发布的区别。
- 继续保持 `0.1.0-SNAPSHOT`，本阶段不打 tag、不发布 Maven。


### 阶段 35 补充

- `ReactiveMinioAdminClient` 新增 `listBucketUsersInfo(...)` 和 `getTemporaryAccountInfo(...)`。
- 继续明确 access key / service account 加密响应只能走 `EncryptedAdminResponse` 边界。
- Admin product-typed 口径从 53 / 128 提升到 55 / 128。

### 阶段 34 补充

- `ReactiveMinioAdminClient` 新增 `getSiteReplicationMetainfo()`。
- 新增 `traceStream()` / `logStream()`，以 `Flux<byte[]>` 暴露 Admin 诊断流。
- Admin product-typed 口径从 50 / 128 提升到 53 / 128。

### 阶段 33 补充

- `ReactiveMinioClient` 新增 `listenBucketNotification(...)` 与 `listenRootNotification(...)`。
- S3 通知监听以 `Flux<byte[]>` 暴露长连接事件流，不再把产品入口包装成一次性字符串读取。
- S3 product-typed 口径从 76 / 77 提升到 77 / 77。

### 阶段 32 补充

- Crypto Gate 独立复审结论继续保持 Fail：没有 owner/security/architect 三方批准，不引入默认响应解密依赖。
- 新增 `scripts/madmin-fixtures/crypto-gate-status.properties`，把三方批准状态和决策文档纳入脚本门禁。
- `check-crypto-gate.sh` 会先校验状态文件，再检查 fixture、`pom.xml` 和源码 import。
- 新增 `docs/30-stage32-crypto-gate-independent-review.md` 记录 Gate Pass 前置条件和当前拒绝理由。

### 阶段 31 补充

- 破坏性实验环境的 tier、remote target、batch job 夹具改为 typed/raw 双路径校验。
- `run-destructive-tests.sh` 每次退出都会生成本机报告，记录夹具开关、端点指纹和失败恢复提示。
- 新增 `write-report.sh` 与 `report-template.md`，报告不写入 access key、secret key 或请求签名。
- `verify-env.sh` 继续拒绝共享 MinIO 和常见本机默认端点。

### 阶段 30 补充

- `ReactiveMinioAdminClient` 新增策略绑定实体、IDP 配置、remote target、batch job 只读摘要入口。
- 新增 `AdminPolicyEntities`、`AdminIdpConfigList`、`AdminRemoteTargetList`、`AdminBatchJobList`。
- Admin product-typed 口径从 43 / 128 提升到 50 / 128。

### 阶段 29 补充

- `ReactiveMinioStsClient` 新增 SSO、客户端证书、自定义 token 三类 typed 凭证入口。
- 新增 `AssumeRoleSsoRequest`、`AssumeRoleWithCertificateRequest`、`AssumeRoleWithCustomTokenRequest`。
- STS product-typed 口径从 4 / 7 提升到 7 / 7。

### 阶段 28 补充

- `ReactiveMinioClient` 新增 bucket notification typed 配置模型和 get/set 方法。
- 新增 `BucketNotificationTarget`、`BucketNotificationConfiguration`、`BucketReplicationMetrics`。
- 新增 `getBucketReplicationMetrics(...)` / `getBucketReplicationMetricsV2(...)` JSON 包装入口。
- S3 product-typed 口径从 72 / 77 提升到 76 / 77。

### 阶段 27 补充

- `ReactiveMinioClient` 新增对象/bucket ACL typed 方法和 canned ACL 便捷写入。
- 新增 `AccessControlPolicy`、`AccessControlOwner`、`AccessControlGrant`、`CannedAcl`。
- 新增 `SelectObjectContentRequest` 和 `SelectObjectContentResult`，先固定请求模型和原始事件流响应边界。
- S3 product-typed 口径从 67 / 77 提升到 72 / 77。

### 已完成

- 对照本地 `minio` 服务端公开路由，SDK catalog 覆盖 233 / 233，JDK8 与 JDK17+ 分支均无缺失、无额外 catalog。
- 所有 catalog 路由均有 typed 或 advanced 兼容入口，能力矩阵 `raw-fallback = 0`。
- `ReactiveMinioClient` 覆盖对象存储主流程、分片上传、版本列表、对象治理、bucket 子资源治理等常用路径。
- `ReactiveMinioAdminClient` 覆盖安全只读摘要、IAM、用户、用户组、策略、服务账号和风险分层入口。
- `ReactiveMinioKmsClient`、`ReactiveMinioStsClient`、`ReactiveMinioMetricsClient`、`ReactiveMinioHealthClient` 均作为平级专用客户端提供。
- `ReactiveMinioRawClient` 保留为新增接口和特殊接口的兜底入口。
- madmin PBKDF2 + AES-GCM 写入方向和 夹具解密已支持；默认 Argon2id / ChaCha20 加密响应保持 `EncryptedAdminResponse` 边界。
- 破坏性 Admin 测试已迁移到独立 lab 门禁和本机配置文件，默认共享 MinIO 测试不会执行破坏性写入。

### 仍需显式说明的边界

- Admin `encrypted-blocked = 9`：Crypto Gate Pass 前不提供默认 madmin 加密响应的明文 typed 解析。
- Admin `destructive-blocked = 29`：需要独立可回滚 lab，不能在共享 MinIO 环境默认执行。
- 部分 Admin 高风险能力虽然已有产品边界，但仍需要独立 lab 或 Crypto Gate 证据后才能升级为更细的明文/结果模型。

### 阶段 26 验证

- JDK8：单元测试、真实 MinIO 集成测试、route parity、capability matrix、crypto gate、`git diff --check`。
- JDK17+：JDK17 单元测试、真实 MinIO 集成测试、route parity、crypto gate、JDK21/JDK25 compile、`git diff --check`。
- 双分支：secret scan、阶段文件同步检查。
