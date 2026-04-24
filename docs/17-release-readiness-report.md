# 17 阶段 26 发布就绪报告

## 1. 结论

阶段 19 的目标不是宣称“所有 MinIO 能力都已经产品化”，而是给出可信、可复查、可继续推进的发布口径。当前结论如下：

- 路由目录已经对齐本地 `minio` 公开 router：JDK8 分支与 JDK17+ 分支均为 233 / 233，catalog 缺失 0、额外 0。
- 所有 catalog 路由都有 typed 或 advanced 兼容入口，能力矩阵里的 `raw-fallback` 为 0。
- SDK 的用户友好 typed 成熟度仍在继续推进，不能用 route parity 代替产品成熟度。
- Admin 加密响应和破坏性操作已经明确建模为风险边界，不会在共享环境中伪装成普通成功能力。
- 阶段 26 已把该口径整理为 `0.1.0-SNAPSHOT` 发布候选：当前可发布的是“路由/入口/门禁闭环的候选 SDK”，不是“所有接口都已最终产品 typed 化”的 1.0。

## 2. 当前能力矩阵

当前机器报告来自 `.omx/reports/capability-matrix.md`：

| family | route-catalog | product-typed | advanced-compatible | raw-fallback | encrypted-blocked | destructive-blocked |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| s3 | 77 | 77 | 77 | 0 | 0 | 0 |
| admin | 128 | 64 | 128 | 0 | 9 | 29 |
| kms | 7 | 7 | 7 | 0 | 0 | 0 |
| sts | 7 | 7 | 7 | 0 | 0 | 0 |
| metrics | 6 | 6 | 6 | 0 | 0 | 0 |
| health | 8 | 8 | 0 | 0 | 0 | 0 |

这张表的解释：

- `route-catalog` 表示 MinIO 公开 HTTP 路由已经登记到 SDK catalog。
- `product-typed` 表示有更适合用户直接集成的请求/响应模型、错误说明或风险边界。
- `advanced-compatible` 表示 SDK 保留了可调用入口，但还不一定是最终产品化模型。
- `raw-fallback` 表示只有 raw 能调用、没有专用入口的路由；当前为 0。
- `encrypted-blocked` 表示服务端默认返回 madmin 加密载荷，当前只暴露边界对象。
- `destructive-blocked` 表示需要独立实验环境验证，不能在共享 MinIO 默认执行。

## 3. 对外推荐入口

| 场景 | 推荐入口 | 说明 |
| --- | --- | --- |
| 对象存储 | `ReactiveMinioClient` | 上传、下载、列对象、版本列表、分片上传等主路径。 |
| 管理端 | `ReactiveMinioAdminClient` | 优先使用 L1/L2 typed 方法；L3/L4 按风险门禁执行。 |
| KMS | `ReactiveMinioKmsClient` | key 状态和生命周期接口。 |
| STS | `ReactiveMinioStsClient` | 普通 AssumeRole、WebIdentity、ClientGrants、LDAP typed 凭证解析。 |
| Metrics | `ReactiveMinioMetricsClient` | Prometheus 文本包装和样本解析。 |
| Health | `ReactiveMinioHealthClient` | `isLive()` / `isReady()` 等布尔探针和状态码入口。 |
| 新增或特殊接口 | `ReactiveMinioRawClient` | 兜底调用，不是普通业务主路径。 |

## 4. 当前风险边界

### 4.1 madmin 加密响应

以下接口仍属于 `encrypted-blocked`：

- `ADMIN_GET_CONFIG`
- `ADMIN_GET_CONFIG_KV`
- `ADMIN_LIST_CONFIG_HISTORY_KV`
- `ADMIN_LIST_USERS`
- `ADMIN_ADD_SERVICE_ACCOUNT`
- `ADMIN_INFO_SERVICE_ACCOUNT`
- `ADMIN_LIST_SERVICE_ACCOUNTS`
- `ADMIN_INFO_ACCESS_KEY`
- `ADMIN_LIST_ACCESS_KEYS_BULK`

SDK 会返回 `EncryptedAdminResponse`，并通过 `algorithm()` / `algorithmName()` 暴露诊断信息。只有 Crypto Gate Pass 后，才能把这些响应升级为明文 typed 模型。

### 4.2 破坏性 Admin

配置写入、站点复制、tier、批处理、远端 target、service restart/update 等高风险接口不能在共享 MinIO 默认验证。必须通过：

1. `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
2. `scripts/minio-lab/verify-env.sh`
3. `scripts/minio-lab/run-destructive-tests.sh`
4. 独立可回滚 MinIO 环境
5. 必要时提供 `MINIO_LAB_TEST_CONFIG_KV` 与 `MINIO_LAB_RESTORE_CONFIG_KV`

## 5. 本阶段补齐内容

- 更新 README 的能力口径、入口矩阵、示例列表和完成度说明。
- 更新 `docs/09-minio-api-catalog.md`，把两层 API 说明修正为领域 typed、专用 typed、raw 兜底三类入口。
- 更新 `docs/13-admin-risk-levels.md`、`docs/14-typed-client-usage-guide.md`、`docs/release-gates.md`，保持 Admin 风险、Crypto 边界、发布门禁口径一致。
- 修正 `ReactiveMinioTypedAdminExample`，不再示范会修改共享环境的用户组写操作，也不再调用已明确为加密边界的明文 access-key typed 方法。
- 新增 `ReactiveMinioRawFallbackExample` 和 `ReactiveMinioOpsExample`，分别展示 raw 兜底和运维客户端。

## 5.1 阶段 20 补充

阶段 20 已开始按后续计划提升 S3 product-typed 成熟度，并新增：

- `ObjectAttributes` 与 `getObjectAttributes(...)`。
- `ObjectRetentionConfiguration` 与 `getObjectRetention(...)` / `setObjectRetention(...)`。
- `ObjectLegalHoldConfiguration` 与 `getObjectLegalHold(...)` / `setObjectLegalHold(...)`。
- `RestoreObjectRequest` 与 `restoreObject(...)`。

对应 advanced 兼容入口已标记为 `@Deprecated`，详见 `docs/18-stage20-s3-object-governance.md`。

## 5.2 阶段 21 补充

阶段 21 继续把 bucket 子资源推进到 typed 主路径，并新增：

- `BucketCorsConfiguration` / `BucketCorsRule` 与 `getBucketCorsConfiguration(...)`、`setBucketCorsConfiguration(...)`、`deleteBucketCorsConfiguration(...)`。
- `BucketWebsiteConfiguration` 与 `getBucketWebsiteConfiguration(...)`、`deleteBucketWebsiteConfiguration(...)`。
- `BucketLoggingConfiguration`、`BucketPolicyStatus`、`BucketAccelerateConfiguration`、`BucketRequestPaymentConfiguration` 摘要模型。

对应 advanced 兼容入口已标记为 `@Deprecated`，详见 `docs/19-stage21-s3-bucket-subresources.md`。

## 5.3 阶段 22 补充

阶段 22 按风险分级补充 Admin 只读 JSON typed 入口，并新增：

- `getBackgroundHealStatus()`
- `listPoolsInfo()` / `getPoolStatus(...)`
- `getRebalanceStatus()`
- `getTierStats()`
- `getSiteReplicationInfo()` / `getSiteReplicationStatus()`
- `getTopLocksInfo()`
- `getObdInfo()` / `getHealthInfo()`

这些方法统一返回 `AdminJsonResult`，先解决调用入口、JSON 解析、中文风险说明和 raw JSON 保留问题。详见 `docs/20-stage22-admin-readonly-summaries.md`。

## 5.4 阶段 23 补充

阶段 23 处理 KMS 与 Metrics 的低风险 typed 缺口，并新增：

- `ReactiveMinioKmsClient.scrapeMetrics()`
- `ReactiveMinioMetricsClient.scrapeLegacyMetrics(...)`

这两个入口统一返回 `PrometheusMetrics`，详见 `docs/21-stage23-kms-metrics-typed-gap.md`。

## 5.5 阶段 24 补充

阶段 24 扩展 破坏性实验环境：

- 新增 `scripts/minio-lab/load-config.sh` 和 `scripts/minio-lab/lab.example.properties`。
- `verify-env.sh` / `run-destructive-tests.sh` 支持 `MINIO_LAB_CONFIG_FILE` 和本机未提交的 `scripts/minio-lab/lab.properties`。
- `DestructiveAdminIntegrationTest` 支持从 lab 配置文件读取参数。
- 新增 bucket quota write + restore、tier verify、remote target list、batch job probe 的可选 仅实验环境 夹具。

这些能力只在独立可回滚 lab 中执行，详见 `docs/22-stage24-destructive-lab-expansion.md`。

## 5.6 阶段 25 补充

阶段 25 复核 Crypto Gate 后继续保持 Gate Fail：

- 不新增 Bouncy Castle、argon2-jvm、Tink、libsodium、JNA 等 crypto/native 依赖。
- `scripts/madmin-fixtures/check-crypto-gate.sh` 已扩展为同时检查 `pom.xml` 与源码 import。
- `MadminEncryptionSupportTest` 明确 Gate Pass 前只有 PBKDF2 + AES-GCM 标记为 Java 端可解密。
- `EncryptedAdminResponse` 仍是 madmin-go 默认加密响应的发布边界。

这意味着 release readiness 可以声明“加密边界已被验证并固定”，但不能声明“默认 madmin 加密响应已完成明文 typed 解析”。详见 `docs/23-stage25-crypto-gate-review.md`。

## 5.7 阶段 26 补充

阶段 26 完成发布收口：

- 新增 `CHANGELOG.md`，记录 `0.1.0-SNAPSHOT` 阶段 26 发布候选的已完成能力和剩余边界。
- 新增 `docs/24-stage26-release-closeout.md`，把 route parity、callability、typed maturity、Crypto Gate、破坏性实验环境和后续迭代方向集中成发布说明。
- README 增加阶段 26 发布候选口径，避免用户把 route parity 误解为所有接口都已最终强类型化。
- 后续计划应从“补目录”转为“提升产品 typed 成熟度和高风险真实验证”。

## 5.8 阶段 27 补充

阶段 27 继续提升 S3 typed 成熟度：

- 新增 ACL Owner/Grant/Policy 模型和 object/bucket ACL typed 获取方法。
- 新增 canned ACL 便捷写入方法，避免用户手写 `x-amz-acl` header。
- 新增 SelectObjectContent 请求模型和响应边界对象，明确当前暂不承诺完整事件流 typed 解码。
- S3 product-typed 从 67 / 77 提升到 72 / 77，阶段 28 继续提升到 76 / 77。


## 5.9 阶段 28 补充

阶段 28 继续提升 S3 typed 成熟度：

- 新增 bucket notification 目标模型和 get/set typed 方法。
- 新增 replication metrics v1/v2 JSON 包装，保留 raw JSON 和 Map 以兼容字段漂移。
- notification listen 仍保持事件流边界，不伪装成普通一次性响应。
- S3 product-typed 从 72 / 77 提升到 76 / 77。


## 5.10 阶段 29 补充

阶段 29 补齐 STS 高级身份源 typed 请求模型：

- SSO 表单入口支持 WebIdentity / ClientGrants action、token、RoleArn 和 DurationSeconds。
- 客户端证书入口支持 DurationSeconds，真实证书由调用方配置到 WebClient TLS 层。
- 自定义 token 入口支持 Token、RoleArn 和 DurationSeconds。
- STS product-typed 从 4 / 7 提升到 7 / 7。


## 5.11 阶段 30 补充

阶段 30 深化 Admin L1/L2 typed 摘要：

- 新增策略绑定实体摘要。
- 新增 IDP 配置列表与单配置 JSON 包装。
- 新增 remote target 只读摘要，不暴露凭据字段。
- 新增 batch job 列表、状态和详情摘要。
- Admin product-typed 从 43 / 128 提升到 50 / 128。

## 5.12 阶段 31 补充

阶段 31 不提升接口数量，而是提升 破坏性实验环境的可信度：

- tier、remote target、batch job 可选 夹具同时走专用 typed 客户端和 `ReactiveMinioRawClient` catalog 调用。
- remote target 可用 `MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN` 做 typed 摘要断言。
- batch job 可用 `MINIO_LAB_BATCH_EXPECTED_JOB_ID` 做 typed/raw 摘要断言。
- `run-destructive-tests.sh` 退出时生成本机报告，记录夹具开关、端点指纹和失败恢复提示，且不记录凭证。
- `verify-env.sh` 仍拒绝共享 MinIO 与常见本机默认端点。

## 5.13 阶段 32 补充

阶段 32 完成 Crypto Gate 独立复审：

- 继续保持 Crypto Gate Fail，不引入默认 madmin 响应解密依赖。
- 新增 `scripts/madmin-fixtures/crypto-gate-status.properties`，记录 owner/security/architect 三方批准状态。
- `check-crypto-gate.sh` 会校验状态文件、fixture、`pom.xml` 和源码 import。
- 新增 `docs/30-stage32-crypto-gate-independent-review.md` 作为阶段 32 决策记录。

## 5.14 阶段 33 补充

阶段 33 补齐 S3 通知监听产品化边界：

- 新增 `listenBucketNotification(...)` 和 `listenRootNotification(...)`。
- 返回 `Flux<byte[]>`，明确这是长连接事件流。
- advanced 的 `s3Listen*` 方法继续保留兼容，但不作为推荐业务入口。
- S3 product-typed 从 76 / 77 提升到 77 / 77。

## 5.15 阶段 34 补充

阶段 34 继续扩展 Admin 只读摘要和诊断流边界：

- 新增 `getSiteReplicationMetainfo()`，保留站点复制元信息 raw JSON。
- 新增 `traceStream()` 和 `logStream()`，以响应式字节流暴露 Admin trace/log。
- Admin product-typed 从 50 / 128 提升到 53 / 128。
- encrypted-blocked 仍为 9，destructive-blocked 仍为 29。

## 5.16 阶段 35 补充

阶段 35 整理 Admin IAM / Access Key 边界：

- 新增 `listBucketUsersInfo(...)`，返回 bucket 相关用户摘要。
- 新增 `getTemporaryAccountInfo(...)`，返回临时账号只读摘要。
- encrypted-blocked 的 access key / service account 接口继续只暴露 `EncryptedAdminResponse`。
- Admin product-typed 从 53 / 128 提升到 55 / 128。

## 5.17 阶段 36 补充

阶段 36 深化破坏性实验写入夹具：

- 新增 tier add/edit/remove 可回滚写入夹具。
- 新增 remote target set/remove 可回滚写入夹具。
- `verify-env.sh` 检测写入请求体或删除 ARN 时，必须要求 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`。
- 写入夹具同时覆盖 `ReactiveMinioAdminClient` 和 `ReactiveMinioRawClient`，报告只记录设置状态，不记录请求体或凭证。

## 5.18 阶段 37 补充

阶段 37 为更高风险的 Admin 能力建立实验矩阵：

- batch job start/status/cancel 支持本机私有 YAML 请求体文件。
- site replication add/edit/remove 支持本机私有 JSON 请求体文件。
- 报告新增 batch/site replication 矩阵开关、请求体设置状态和恢复提示。
- 新增 `scripts/minio-lab/templates/` 示例模板，但真实拓扑、endpoint 和凭证不得提交。

## 5.19 阶段 38 补充

阶段 38 收口用户示例体验：

- 删除临时 `TestCreateBucket` 与 `TestGetBucketLocation` 示例类。
- 新增 `ReactiveMinioSecurityExample`，覆盖 KMS 状态检查和 STS 临时凭证申请。
- README 示例入口覆盖对象存储、Admin typed、Raw 兜底、Metrics/Health、KMS/STS 五条正式路径。
- 新增 `docs/36-stage38-examples-ux-closeout.md` 记录示例矩阵和错误解释原则。

## 5.20 阶段 39 复审结论

阶段 39 重新复审当前发布候选：

- 双分支 route parity 仍为 233 / 233，catalog 缺失 0、额外 0。
- 能力矩阵仍为 S3 77 / 77、Admin 64 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8。
- `raw-fallback = 0`，说明所有 catalog 路由至少有专用 typed 或 advanced 兼容入口。
- 剩余主要是 Admin typed 成熟度、独立 lab 真实执行证据和 Crypto Gate Pass 准备。

详见 `docs/37-stage39-release-candidate-review.md`。

## 5.21 阶段 40 补充

阶段 40 补充 Admin 诊断类 typed/stream 包装：

- `scrapeAdminMetrics()` 返回 `PrometheusMetrics`。
- `downloadInspectData(...)` 和 `downloadProfilingData()` 返回 `AdminBinaryResult`，避免二进制诊断包被误解码。
- `startProfiling(...)` 和 `getProfileResult(...)` 返回 `AdminTextResult`。
- Admin product-typed 从 55 / 128 提升到 60 / 128。

详见 `docs/38-stage40-admin-diagnostics-typed-wrappers.md`。

## 5.22 阶段 41 补充

阶段 41 继续深化 Admin IAM / IDP 只读摘要：

- `listLdapPolicyEntities()` 返回 LDAP 策略绑定实体摘要。
- `listLdapAccessKeySummaries(...)` 和 `listOpenidAccessKeySummaries(...)` 返回只读 access key 摘要。
- `AdminAccessKeySummaryList` 不保存 raw JSON，避免 secret、session token 或私钥进入普通模型。
- Admin product-typed 从 60 / 128 提升到 63 / 128。

详见 `docs/39-stage41-admin-iam-idp-readonly.md`。

## 5.23 阶段 42 补充

阶段 42 继续处理 site replication 中可安全产品化的只读能力：

- `getSiteReplicationPeerIdpSettings()` 返回站点复制 peer 的 IDP 设置摘要。
- `AdminSiteReplicationPeerIdpSettings` 不保存 raw JSON，只暴露 LDAP/OpenID 是否启用、LDAP 搜索条件、OpenID 区域和角色数量。
- site replication 相关专用客户端入口按 madmin-go 补齐 `api-version=1` 查询参数，避免调用方手动补协议版本。
- Admin product-typed 从 63 / 128 提升到 64 / 128。

详见 `docs/40-stage42-site-replication-peer-idp.md`。

## 5.24 阶段 43 补充

阶段 43 增强破坏性 lab 的真实证据产物：

- `run-destructive-tests.sh` 自动生成 `MINIO_LAB_RUN_ID` 和 typed/raw 步骤状态文件。
- `DestructiveAdminIntegrationTest` 在 config、bucket quota、tier、remote target、batch job、site replication 的关键 typed/raw 步骤写入 PASS/FAIL。
- `write-report.sh` 在报告中渲染 typed/raw 执行明细，并增加 `mc` 只读恢复核验提示。
- 报告仍不写入请求体、凭证、token、签名或完整异常堆栈。

详见 `docs/41-stage43-destructive-lab-evidence.md`。

## 5.25 阶段 44 补充

阶段 44 统一错误解释与异常体验：

- `ReactiveMinioException` 的默认消息改为中文诊断。
- Admin/KMS/STS/Metrics/Health 的结构化异常字段继续保留，业务代码不需要解析中文文本。
- raw 请求构造阶段的本地失败改为中文说明，覆盖缺少 query、危险 header、路径变量非法等场景。
- 纯文本服务端错误会在消息中显示 `响应体片段=`，避免空 code/message 时没有排障线索。

详见 `docs/42-stage44-error-experience.md`。

## 5.26 阶段 45 补充

阶段 45 只做 Crypto Gate Pass 准备，不直接放行：

- `crypto-gate-status.properties` 继续保持 `fail`，三方批准仍为 `false`。
- 不新增 crypto/native/provider 依赖，不修改 `pom.xml`。
- 新增候选方案、批准材料、JDK8/JDK17/JDK21/JDK25 测试矩阵和失败回退清单。
- `encrypted-blocked = 9` 仍是发布边界，默认 madmin 加密响应继续返回 `EncryptedAdminResponse`。

详见 `docs/43-stage45-crypto-gate-pass-prep.md`。

## 5.27 阶段 46 补充

阶段 46 重新刷新发布复审：

- 双分支 route parity 报告重新生成，仍为 233 / 233，catalog 缺失 0、额外 0。
- 双分支 capability matrix 重新生成，Admin product-typed 保持 64 / 128，`raw-fallback = 0`。
- 阶段 40-45 的增量已经整理到 `docs/44-stage46-release-review-refresh.md`。
- 发布说明继续按 route parity、callability、typed maturity、Crypto Gate、破坏性 lab 分层描述，不能用单一百分比替代。

详见 `docs/44-stage46-release-review-refresh.md`。

## 6. 验证命令

阶段 19 发布就绪至少应重新执行以下命令，并把输出作为最终证据：

```bash
cd /dxl/minio-project/minio-reactive-java
mvn -q -DfailIfNoTests=true test
MINIO_ENDPOINT=... MINIO_ACCESS_KEY=... MINIO_SECRET_KEY=... mvn -q -Dtest=LiveMinioIntegrationTest test
python3 scripts/report-route-parity.py --minio-root /dxl/minio-project/minio --worktree /dxl/minio-project/minio-reactive-java --format markdown --output /dxl/minio-project/.omx/reports/route-parity-jdk8.md
python3 scripts/report-capability-matrix.py --worktree /dxl/minio-project/minio-reactive-java --worktree /dxl/minio-project/minio-reactive-java-jdk17 --format markdown --output /dxl/minio-project/.omx/reports/capability-matrix.md
scripts/madmin-fixtures/check-crypto-gate.sh

git diff --check
```

JDK17+ 分支还需要额外执行 JDK21/JDK25 compile，确保高版本分支没有语言或依赖退化。

## 7. 下一阶段方向

阶段 20 之后应继续提升 `product-typed`，优先顺序为：

1. S3 剩余高频 typed 子资源：select、ACL、object lambda/extract、replication metrics、listen notification 等模型化或明确边界化。
2. Admin L1/L2 继续 typed 化：policy attachment、batch job 只读查询、remote target 只读列表、IDP 只读配置、LDAP/OpenID 只读列表。
3. STS 剩余证书、自定义 token、SSO 入口按独立身份源环境补 typed 请求模型和边界说明。
4. 破坏性实验环境后续继续补 site replication、remote target 写入/移除、batch job 启停/取消等更重夹具；阶段 31 已沉淀 typed/raw 双路径和报告模板。
5. Crypto Gate 的依赖和安全设计评审；阶段 25 已复核但未通过，后续只有三方批准后才进入默认响应解密原型。
