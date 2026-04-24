# 变更日志

本文件记录 SDK 里程碑级变化。当前项目仍处于 `0.1.0-SNAPSHOT`，阶段 26 是“对标 MinIO 路由完整、调用入口完整、风险边界明确”的发布候选收口，不等同于 1.0 稳定版。

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
- Admin、STS、S3 中仍有一批 advanced-compatible 能力尚未升级为最终产品级 typed 模型。

### 阶段 26 验证

- JDK8：单元测试、真实 MinIO 集成测试、route parity、capability matrix、crypto gate、`git diff --check`。
- JDK17+：JDK17 单元测试、真实 MinIO 集成测试、route parity、crypto gate、JDK21/JDK25 compile、`git diff --check`。
- 双分支：secret scan、阶段文件同步检查。
