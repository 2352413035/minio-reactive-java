# minio-reactive-java

这是一个面向学习和实现的响应式 MinIO Java SDK 原型项目。

## 项目目标

这个仓库同时承担两件事：

1. 梳理 `minio-java` 当前的实现方式。
2. 设计并实现一个基于 WebFlux/Reactor 的响应式 MinIO SDK。

## 本地配置文件

Windows 下更方便的方式是直接使用项目内配置文件：

- `src/main/resources/minio-local.properties`

示例会优先从类路径资源 `minio-local.properties` 读取配置，然后再读取环境变量。

```properties
minio.endpoint=http://127.0.0.1:9000
minio.access-key=your-access-key
minio.secret-key=your-secret-key
minio.region=us-east-1
minio.bucket=
minio.object=hello.txt
minio.content=hello from reactive minio sdk
```

如果 `minio.bucket` 留空，示例会自动生成一个测试 bucket 名称。

## 关于官方 minio-java 的一个关键认识

`minio-java` 当前不是“纯阻塞 SDK”。

它更准确地说是两层：

1. `MinioAsyncClient`
   - 基于 `OkHttp` 异步调用
   - 对外返回 `CompletableFuture<T>`
2. `MinioClient`
   - 在异步客户端之上用 `.join()` 暴露同步风格 API

所以它不是 Reactor/WebFlux 风格的端到端响应式实现。

## 当前原型已实现的能力

- SigV4 普通请求签名与 presigned URL 签名。
- 对象存储主流程：`listBuckets`、`bucketExists`、`makeBucket`、`removeBucket`、`getBucketLocation`。
- 对象读写：`listObjects` / `listObjectsPage`、`putObject`、`getObject`、`getObjectRange`、`getObjectAsBytes`、`getObjectAsString`、`statObject`、`copyObject`、`removeObject`、`removeObjects`。
- 对象和桶子资源：object/bucket tagging、bucket policy/lifecycle/versioning/notification/encryption/object-lock/replication 等 XML/JSON 入口，其中 bucket versioning 已提供 typed 配置对象。
- 对象治理：object attributes、object retention、legal hold、restore 已提供 typed 请求/响应模型。
- bucket 子资源治理：CORS、website、logging、policy status、accelerate、request payment 已提供 typed 或摘要模型。
- ACL、notification 与 S3 Select：对象/bucket ACL 已提供 Owner/Grant 模型和 canned ACL 便捷写入；notification 配置已提供目标模型；SelectObjectContent 已提供请求模型和原始事件流边界。
- 分片上传：create/uploadPart/listParts/complete/abort 基础流程，以及 `listMultipartUploads` / `listMultipartUploadsPage` typed 分页模型。
- 版本能力：`listObjectVersions` / `listObjectVersionsPage` typed 分页模型。
- Admin：server/storage/data-usage/account/config-help、pool/rebalance/tier/site-replication/peer-idp/top-locks/obd/health 等 L1 只读摘要模型，用户、用户组、策略、服务账号等 typed 或风险分层入口。
- KMS、STS、Metrics、Health：均有独立专用客户端；KMS/Metrics 指标入口保留 Prometheus 文本和样本解析，STS 普通 AssumeRole 已有 typed 请求对象，Health 已有布尔检查。
- madmin 加密边界：配置、服务账号、access key 等默认加密响应显式返回 `EncryptedAdminResponse`，并暴露算法诊断信息，不伪装成明文模型。
- 破坏性 Admin 实验环境：破坏性 Admin 测试只允许在独立可回滚环境中运行，支持独立 lab 配置文件；默认共享 MinIO 集成测试不会修改危险配置。

这些能力已经通过 JDK8 单元测试以及真实 MinIO 集成测试进行了验证。

## 当前完成度口径

阶段 19 之后，项目不再用单一百分比宣称“完成 MinIO”。当前使用四个口径说明进度：

| 口径 | 当前状态 |
| --- | --- |
| 路由对标 | 233 / 233，JDK8 与 JDK17+ 两个分支均无缺失、无额外 catalog。 |
| 可调用覆盖 | `raw-fallback = 0`，所有公开 catalog 路由至少有 typed 或 advanced 兼容入口。 |
| 产品强类型成熟度 | S3 77 / 77、Admin 64 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8。这个数字表示用户友好 typed 成熟度，不表示路由是否能调用。 |
| 风险边界 | Admin 仍有 9 个加密响应边界、29 个破坏性操作边界；破坏性写入只允许在独立 lab 通过显式写入夹具证明，不能在共享环境中伪装成“普通已完成”。 |

机器报告统一见 `.omx/reports/route-parity-jdk8.md`、`.omx/reports/route-parity-jdk17.md` 和 `.omx/reports/capability-matrix.md`。

阶段 26 已整理为 `0.1.0-SNAPSHOT` 发布候选口径：路由和调用入口闭环，常用 typed 客户端可用，Crypto/破坏性风险边界明确，阶段 32 已把 Crypto Gate Fail 做成状态文件门禁，后续继续按 product-typed 成熟度推进。详见 `CHANGELOG.md` 和 `docs/24-stage26-release-closeout.md`。

## 运行真实 MinIO 示例

示例入口：

- `io.minio.reactive.examples.ReactiveMinioLiveExample`
- `io.minio.reactive.examples.ReactiveMinioTypedAdminExample`
- `io.minio.reactive.examples.ReactiveMinioRawFallbackExample`
- `io.minio.reactive.examples.ReactiveMinioOpsExample`
- `io.minio.reactive.examples.ReactiveMinioSecurityExample`

运行命令：

```powershell
mvn compile "exec:java" "-Dexec.mainClass=io.minio.reactive.examples.ReactiveMinioLiveExample"
```

## 真实 MinIO 集成测试

集成测试类：

- `io.minio.reactive.integration.LiveMinioIntegrationTest`

如果你仍然想用环境变量，也可以：

```powershell
$env:MINIO_ENDPOINT='http://127.0.0.1:9000'
$env:MINIO_ACCESS_KEY='your-access-key'
$env:MINIO_SECRET_KEY='your-secret-key'
$env:MINIO_REGION='us-east-1'
mvn -Dtest=LiveMinioIntegrationTest test
```


## 推荐入口矩阵

| 场景 | 首选客户端 | 说明 |
| --- | --- | --- |
| 普通对象存储（上传、下载、列对象、分片） | `ReactiveMinioClient` | 业务项目默认先用它。 |
| 管理端 IAM / 用户 / 用户组 / 策略 | `ReactiveMinioAdminClient` | typed 优先；默认加密响应未解锁时会明确返回加密边界对象。 |
| KMS | `ReactiveMinioKmsClient` | 适合 status / key 生命周期场景。 |
| STS | `ReactiveMinioStsClient` | 适合临时凭证与 credentials provider 场景。 |
| Metrics / Health | `ReactiveMinioMetricsClient` / `ReactiveMinioHealthClient` | 监控与健康检查分离。 |
| SDK 尚未及时跟上新增接口 | `ReactiveMinioRawClient` | raw 是兜底，不是普通业务主路径。 |

## SDK 分层理念

本 SDK 的设计理念是：先把 MinIO 的公开接口统一登记到目录，再在目录之上提供不同层次的调用器。

- `MinioApiCatalog`：统一接口目录，汇总本地 `minio` 公开路由文件中的 S3/Admin/KMS/STS/Health/Metrics 233 个 HTTP 接口。
- `ReactiveMinioRawClient`：兜底原始调用器。SDK 如果暂时没有跟上某个新增 API，用户仍然可以通过目录和 raw client 自己发起请求。
- `ReactiveMinioClient`：对象存储专用客户端，适合日常项目集成时上传、下载、列对象、分片上传等常用场景。
- `ReactiveMinioAdminClient`：管理端专用客户端，例如用户、策略、服务信息、配置、批处理等接口入口。
- `ReactiveMinioKmsClient`：KMS 专用客户端。
- `ReactiveMinioStsClient`：临时凭证 STS 专用客户端。
- `ReactiveMinioMetricsClient`：监控指标专用客户端。
- `ReactiveMinioHealthClient`：健康检查专用客户端。

一般业务项目优先直接创建并使用 `ReactiveMinioClient`。只有需要管理端、KMS、STS、监控、健康检查等能力时，才直接创建对应专用客户端。所有客户端都是平级入口；`ReactiveMinioRawClient` 是最后的兜底层，用于尚未封装成专用方法的新接口或特殊接口。

当前已开始补充强业务方法：Health 提供 `isLive()` / `isReady()` 等布尔检查；Metrics/KMS 提供 Prometheus 文本包装和样本解析；STS 提供普通 AssumeRole / WebIdentity / ClientGrants / LDAP / SSO / 证书 / 自定义 token 临时凭证解析入口；KMS、Admin IAM、用户组、服务账号、Admin 只读状态摘要、策略绑定实体、IDP 配置、remote target、batch job 摘要以及 S3 版本/分片列表、对象治理、bucket CORS/website/logging/policy status、ACL/Select 等子资源提供 typed 模型。其它大量高级管理接口仍保留兼容入口和 raw 兜底，后续按高价值子集逐步增强。

阶段 27 起，S3 ACL 与 SelectObjectContent 也进入 typed 主路径：ACL 返回 Owner/Grant 模型，canned ACL 通过便捷方法写入；Select 先提供请求模型和原始事件流边界，后续再升级完整事件解码。阶段 28 继续补充 notification 配置模型和 replication metrics JSON 包装。阶段 31 把破坏性实验环境升级为 typed/raw 双路径校验：tier、remote target、batch job 夹具会先验证专用客户端摘要，再用 raw catalog 调用交叉佐证，同时生成本机执行报告。阶段 33 新增 S3 通知监听流式入口，避免把长连接事件流误包装成一次性字符串。阶段 34 继续补 Admin 站点复制元信息和 trace/log 流式诊断入口。阶段 35 补充 bucket 用户和临时账号只读摘要，同时继续保持 access key/service account 加密边界。阶段 36 新增 tier 与 remote target 的可回滚写入夹具，要求 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true` 才能执行，并同时验证专用 Admin 客户端与 raw 兜底调用。阶段 37 新增 batch job 与 site replication 实验矩阵，用本机私有请求体模板证明 start/cancel、add/remove 的恢复路径。阶段 38 删除临时 Test 示例类，补齐 KMS/STS 中文示例，并把 README 示例入口收口为正式用户路径。阶段 40 补充 Admin metrics、inspect-data、profiling/profile 的文本或二进制诊断包装。阶段 41 补充 LDAP/OpenID access key 只读摘要，并明确不保留 secret/token 原始响应。阶段 42 补充站点复制 peer IDP 设置安全摘要，并按 madmin-go 在专用客户端调用中补齐 site replication 的 api-version 查询参数。阶段 43 增强破坏性 lab 报告，记录 typed/raw 步骤 PASS/FAIL，并加入 `mc` 只读恢复核验提示。阶段 44 统一异常体验，将协议错误和 raw 本地校验改为中文诊断。阶段 45 完成 Crypto Gate Pass 前置清单整理，继续保持 Gate Fail，不新增密码学依赖，只明确候选方案、批准材料、测试矩阵和失败回退。

详见 `docs/04-minio-reactive-java-design.md` 和 `docs/09-minio-api-catalog.md`。

## 文档与脚本目录

- `docs/01-minio-s3-basics.md`
- `docs/02-minio-java-architecture.md`
- `docs/03-reactive-programming-basics.md`
- `docs/04-minio-reactive-java-design.md`
- `docs/05-implementation-roadmap.md`
- `docs/06-current-implementation-notes.md`
- `docs/07-debugging-notes.md`
- `docs/08-s3-method-protocol.md`

- `docs/09-minio-api-catalog.md`
- `docs/10-version-management.md`
- `docs/11-api-migration-and-advanced-compat.md`
- `docs/12-madmin-encryption-compat.md`
- `docs/13-admin-risk-levels.md`
- `docs/14-typed-client-usage-guide.md`
- `docs/15-stage16-nonadmin-typed-decisions.md`
- `docs/16-crypto-boundary-map.md`
- `docs/17-release-readiness-report.md`
- `docs/18-stage20-s3-object-governance.md`
- `docs/19-stage21-s3-bucket-subresources.md`
- `docs/20-stage22-admin-readonly-summaries.md`
- `docs/21-stage23-kms-metrics-typed-gap.md`
- `docs/22-stage24-destructive-lab-expansion.md`
- `docs/23-stage25-crypto-gate-review.md`
- `docs/24-stage26-release-closeout.md`
- `docs/25-stage27-s3-acl-select.md`
- `docs/26-stage28-s3-notification-replication.md`
- `docs/27-stage29-sts-advanced-identity.md`
- `docs/28-stage30-admin-l1-l2-summaries.md`
- `docs/29-stage31-destructive-lab-fixtures.md`
- `docs/30-stage32-crypto-gate-independent-review.md`
- `docs/31-stage33-s3-notification-listen.md`
- `docs/32-stage34-admin-readonly-summaries.md`
- `docs/33-stage35-admin-iam-boundary.md`
- `docs/34-stage36-destructive-lab-write-fixtures.md`
- `docs/35-stage37-batch-site-replication-lab-matrix.md`
- `docs/36-stage38-examples-ux-closeout.md`
- `docs/37-stage39-release-candidate-review.md`
- `docs/38-stage40-admin-diagnostics-typed-wrappers.md`
- `docs/39-stage41-admin-iam-idp-readonly.md`
- `docs/40-stage42-site-replication-peer-idp.md`
- `docs/41-stage43-destructive-lab-evidence.md`
- `docs/42-stage44-error-experience.md`
- `docs/43-stage45-crypto-gate-pass-prep.md`
- `docs/release-gates.md`
- `CHANGELOG.md`
- `scripts/madmin-fixtures/`
- `scripts/minio-lab/`
- `scripts/report-route-parity.py`
- `scripts/report-capability-matrix.py`
