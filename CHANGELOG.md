# 变更日志

本文件记录 SDK 里程碑级变化。当前项目仍处于 `0.1.0-SNAPSHOT`，阶段 26 是“对标 MinIO 路由完整、调用入口完整、风险边界明确”的发布候选收口，不等同于 1.0 稳定版。

## 阶段 118 replication diff query 语义

- `runReplicationDiff` 新增无请求体和 query 选项重载，对齐 madmin `BucketReplicationDiff` 的 `verbose`、`prefix`、`arn` 参数语义。
- 破坏性 lab 新增 `MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE`，只有具备真实 bucket replication 配置的独立 lab 才执行 typed/raw 探测。
- 本阶段不降低 `ADMIN_REPLICATION_DIFF` 的破坏性边界；它仍需要复制拓扑证据后才能从剩余 17 个风险项中移除。

## 阶段 117 全量配置写入 lab 补证

- 新增 `MINIO_LAB_ALLOW_FULL_CONFIG_WRITE` 门禁，只在独立 lab 中执行 `ADMIN_SET_CONFIG` 全量配置原样写回。
- `DestructiveAdminIntegrationTest` 同时覆盖专用 Admin `setConfigText` 与 raw `ADMIN_SET_CONFIG`，并在 finally 中恢复原始全量配置文本。
- `scripts/report-destructive-boundary.py` 将 `ADMIN_SET_CONFIG` 更新为已有独立 lab 证据；剩余破坏性边界继续要求更复杂 lab 或维护窗口。

## 阶段 116 发布就绪总览机器报告

- 新增 `scripts/report-release-readiness.py`，把 minio-java 对标、签名级差异、route parity、能力矩阵、POM 元数据、Crypto Gate 和破坏性边界聚合成一份发布就绪总览。
- 新增 `docs/114-stage116-release-readiness-aggregator.md`，说明脚本用途、双分支使用方式和当前“发布候选就绪、正式发布未就绪”的判定。
- 聚合报告继续不连接 MinIO、不执行写入、不修改 POM、不发布 Maven。

## 阶段 115 发布元数据安全准备清单

- 新增 `docs/113-stage115-release-metadata-safe-prep.md`，把正式 Maven/tag 发布前必须由负责人确认的许可证、SCM、developers、发布仓库、签名、SBOM、Javadoc 和回滚输入整理成中文清单。
- 明确本阶段不修改版本号、不写入猜测的 POM 元数据、不配置签名密钥、不发布 Maven、不创建 tag。
- 发布门禁继续要求引用 POM 元数据报告和 destructive boundary 报告；Crypto Gate 已 Pass，但仍是每次发布的回归项。

## 阶段 114 破坏性边界机器报告

- 新增 `scripts/report-destructive-boundary.py`，支持 Markdown/JSON 输出 29 个高风险 Admin 路由的分类报告。
- 报告区分已有独立 lab 证据、可回滚候选、拓扑或身份提供方、维护窗口和资源压测五类，避免把 `destructive-blocked` 误解成功能缺口。
- 发布门禁要求后续复审同时引用 capability matrix 与 destructive boundary 报告。

## 阶段 113 发布就绪与破坏性边界再审计

- 重新运行 POM 发布元数据预检，确认基础坐标齐全，但正式 Maven 发布仍缺 URL、许可证、SCM、developers、issueManagement、organization、distributionManagement、source/javadoc/sign/SBOM 等负责人材料。
- 重新分类 `destructive-blocked = 29`：其中 11 个可回滚路径已有独立 Docker lab typed/raw 证据，其余属于全量配置、IDP、站点复制变更、服务控制、升级、force-unlock 和压测等维护窗口边界。
- 更新发布门禁口径：Crypto Gate 已 Pass 并转为每次发布必须回归的证据项；正式发布仍阻塞于破坏性运维证据和发布工程材料。

## 阶段 112 加密 Admin 响应显式解密便捷入口

- 在 Crypto Gate Pass 基础上，为配置、用户、IDP、服务账号和 access key 等加密 Admin 响应补充显式 `secretKey` 解密入口。
- `getAccessKeyInfoTyped(accessKey, secretKey)` 与 `listAccessKeysTyped(listType, secretKey)` 可直接返回明文业务模型。
- `createServiceAccount(request, secretKey)` / `addServiceAccount(request, secretKey)` 可直接解析创建后的服务账号凭证结果。
- 原 `*Encrypted` 方法继续保留，作为延迟解密和排障边界；SDK 仍不保存、不输出 `secretKey`。

## 阶段 111 Crypto Gate Pass 与默认 madmin 解密放行

- 对齐同目录 `minio-java` 的 adminapi crypto 方案，引入 `org.bouncycastle:bcprov-jdk18on:1.82`。
- `MadminEncryptionSupport` 现在支持 Argon2id + AES-GCM、Argon2id + ChaCha20-Poly1305 与 PBKDF2 + AES-GCM 三类 madmin 载荷解密。
- `EncryptedAdminResponse` 仍要求调用方显式提供对应账号的 `secretKey`；SDK 不保存、不猜测、不输出敏感明文。
- `scripts/madmin-fixtures/check-crypto-gate.sh` 从 Fail 门禁升级为 Pass 门禁，校验状态文件、bcprov 依赖、源码 import 和 Go fixture 互操作。
- capability matrix 中 Admin `encrypted-blocked` 降为 0；`destructive-blocked = 29` 不变，破坏性操作仍需独立 lab 或维护窗口。

## 阶段 110 破坏性 lab 缺口再审计与 tier edit 补证

- 修正 tier edit 模板：按 madmin-go `TierCreds` 使用 `access` / `secret` 字段，不再使用旧式 `AccessKey` / `SecretKey`。
- JDK8/JDK17+ 均在双容器 Docker lab 中通过 tier add/edit/remove typed/raw 矩阵，包含 typed edit 与 raw `ADMIN_EDIT_TIER`。
- 新增阶段110缺口审计文档，说明 `destructive-blocked = 29` 是风险分类计数，不是 SDK 功能缺口。
- service restart/update、decommission、rebalance、force-unlock、speedtest 等仍保留维护窗口边界；site replication edit 真实 endpoint/deploymentID 变更后续单独判断。

## 阶段 109 site replication 多站点 lab 矩阵补证

- 修正 site replication add 模板：请求体对齐 madmin-go 的 `PeerSite[]` 数组，字段名使用 `endpoints`，不再使用旧式 `{"sites": [...]}` 包装。
- 修正 site replication remove 模板：独立 lab 最小恢复体使用 `{"all": true}`，用于清理本次多站点拓扑。
- `DestructiveAdminIntegrationTest` 的 site replication 矩阵扩展为 typed add + typed info/status/metainfo + raw remove + raw add + finally typed remove。
- JDK8/JDK17+ 均在双容器 Docker lab 中通过 site replication typed/raw add/remove 恢复矩阵，并确认没有 lab 容器残留。
- site replication 仍是高风险 Admin 操作，不能在共享环境执行；`destructive-blocked` 风险分类口径不在本阶段清零。

## 阶段 108 batch job 独立 lab 矩阵补证

- 新增 `AdminBatchJobStartResult`，强类型解析 `start-job` 返回的 `jobId`、类型、用户和启动时间。
- `ReactiveMinioAdminClient` 新增 `startBatchJobInfo(...)` 和 `cancelBatchJobRequest(String jobId)`；`cancelBatchJob(String jobId)` 对齐 MinIO madmin，改为 `DELETE /cancel-job?id=<jobId>`。
- 破坏性 lab 的 batch job 矩阵不再要求旧式 cancel YAML，请求模板、准备度审计和报告提示均改为从 start 响应解析 jobId 后取消。
- JDK8/JDK17+ 均在双容器 Docker lab 中通过 batch start/status/cancel 的 typed/raw 写入恢复矩阵，并确认没有 lab 容器残留。
- site replication 多站点矩阵、Crypto Gate 和正式发布工程材料仍是后续外部门禁；`destructive-blocked` 风险分类口径不在本阶段清零。

## 阶段 107 tier 独立 lab 写入恢复补证

- 修正 tier MinIO 请求体模板：tier 名称必须大写，bucket 保持小写，endpoint 必须从源 MinIO 服务端进程视角填写可访问 URL。
- JDK8/JDK17+ 均在双容器 Docker lab 中通过 tier add/remove typed/raw 写入恢复矩阵。
- raw 路径继续显式传入 madmin 加密 body，专用 Admin 客户端继续自动加密。
- tier edit、batch job、site replication 仍待私有夹具和真实恢复证据；`destructive-blocked` 仍为 `29`。

## 阶段 106 高风险 Admin 写入加密语义对齐

- `ReactiveMinioAdminClient` 的 tier add/edit、remote target set、site replication add/edit 现在按 MinIO madmin 语义自动加密请求体，并使用 `application/octet-stream` 发送。
- 新增内部 `executeEncryptedBytesToString(...)`，用于既需要 madmin 加密又需要读取文本响应的接口。
- 破坏性 lab 的 raw 路径继续显式构造加密体，证明 raw 是兜底调用器而不是业务语义包装器。
- remote target 写入夹具优先使用 set 响应 ARN 恢复，`MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` 变为可选兜底；本阶段已在 JDK8/JDK17+ 一次性 Docker lab 通过 remote target set/remove typed/raw 写入恢复矩阵。
- tier、batch job、site replication 仍未执行真实高风险矩阵；为保持风险分类口径，`destructive-blocked` 仍为 `29`。

## 阶段 105 高风险 lab 夹具模板与准备度审计

- 新增 `scripts/minio-lab/audit-fixtures.sh`，逐项审计 tier、remote target、batch job、site replication 高风险夹具缺口；脚本不连接 MinIO、不输出凭证或请求体。
- 新增 tier 与 remote target 请求体模板，以及 `scripts/minio-lab/templates/README.md`，明确填写后的请求体必须放在仓库外私有目录。
- 更新 lab 配置示例、lab README 和硬门禁提示，使用户能先准备私有夹具，再进入真实 typed/raw 破坏性矩阵。
- 本阶段没有执行新的高风险写入矩阵，`destructive-blocked` 继续保持 `29`。

## 阶段 104 独立 Docker lab 破坏性矩阵补证

- 新增 `scripts/minio-lab/start-docker-lab.sh`，可启动不占用共享 `9000/9001` 的一次性 Docker MinIO lab，并在 `/tmp` 生成私有配置，不输出凭证。
- 新增 `docs/102-stage104-independent-docker-lab-matrix.md`，记录本次独立 lab typed/raw 证据和仍未放行的高风险矩阵。
- JDK8/JDK17+ 分支均在独立 `127.0.0.1:19000` lab 中通过 config KV、bucket quota 写入恢复，以及 remote target typed/raw 只读探测。
- tier 写入、remote target 写入、batch job 和 site replication 高风险矩阵仍未放行，`destructive-blocked` 继续保持 `29`。

## 阶段 103 Crypto Gate 自动解密准备

- `EncryptedAdminResponse` 新增 `requiresSecretKey()`、`decrypt(secretKey)`、`decryptAsUtf8(secretKey)`，调用方可显式提供 secret key 解密已放行算法。
- 当前只支持无需新增依赖的 PBKDF2 + AES-GCM madmin 载荷；MinIO 默认 Argon2id / ChaCha20-Poly1305 仍保持 Crypto Gate 边界。
- 新增 `docs/101-stage103-crypto-gate-decryption-prep.md`，说明涉及 Crypto Gate 的接口、madmin 格式、解密条件、失败语义和仍未放行的原因。
- 新增测试覆盖 PBKDF2 Go fixture 通过 `EncryptedAdminResponse` 解密，以及默认 Argon2id 响应继续保留加密边界。

## 阶段 102 发布完成度与剩余门禁审计

- 新增 `docs/100-stage102-release-completion-gate-audit.md`，汇总名称级对标、签名级对标、capability matrix 和多 JDK 验证证据。
- 明确 SDK 功能覆盖层面已经完成 minio-java 主体 API 对标，且签名级报告没有未解释缺口。
- 明确正式发布层面仍阻塞于 Crypto Gate 自动解密、高风险破坏性 lab 全矩阵和 Maven/tag/sign/SBOM 等发布工程材料。
- 后续开发方向从重复补 API 名称转为外部门禁执行、真实 lab 补证、Crypto Gate 设计实现和 release engineering。

## 阶段 101 PutObjectAPIArgs builder 边界判定

- 对照 minio-java 上传参数层级后，确认 `PutObjectAPIArgs` 在响应式 SDK 中应保留为内部上传参数边界，而不是用户侧公开 builder。
- 签名级报告脚本新增 `INTENTIONAL_ARG_BOUNDARIES` 分类，将 `PutObjectAPIArgs` 从未解释缺口移到“响应式 SDK 有意保留为内部边界”。
- 新增 `docs/99-stage101-putobjectapiargs-boundary.md`，说明为什么用户应使用 `PutObjectArgs`、`UploadObjectArgs`、`AppendObjectArgs`、`UploadSnowballObjectsArgs` 等高层入口。

## 阶段 100 minio-java 签名级差异审计

- 新增 `scripts/report-minio-java-signature-parity.py`，在名称级对标之外继续审计方法重载、credentials provider 构造器/工厂和 Args builder 入口。
- 新增 `docs/98-stage100-signature-parity-audit.md`，记录签名级报告的当前结论和后续动作。
- 当前报告显示对象 API 与 Admin API 没有缺失或重载较少项；credentials 阻塞 HTTP 构造器属于响应式 SDK 有意不同；Args 仅 `PutObjectAPIArgs` 需要后续判定是否公开 builder。

## 阶段 99 credentials provider 行为细化

- `AwsEnvironmentProvider` 对空字符串主变量改为和 minio-java 一致：存在但为空时直接报错，不再静默回退到次级变量。
- `Jwt` 增加 `access_token` / `expires_in` 的 Jackson 映射，支持 OIDC/ClientGrants JSON 直接反序列化。
- 新增 `docs/97-stage99-credentials-provider-behavior.md`，记录 provider 行为差异、验证和边界。
- 新增测试覆盖空字符串环境变量和 minio-java 风格 JWT JSON 字段名。

## 阶段 98 provider 行为深化与 Crypto Gate 回归

- `AssumeRoleProvider`、`WebIdentityProvider`、`ClientGrantsProvider`、`LdapIdentityProvider`、`CertificateIdentityProvider` 新增 `fromStsClient(...)` 桥接工厂。
- identity provider 通过现有 `ReactiveMinioStsClient` 强类型方法换取临时凭证，不在 provider 内部复制阻塞 HTTP 客户端。
- 新增 `docs/96-stage98-provider-behavior-crypto-regression.md`，记录 provider 行为深化和 Crypto Gate 回归结论。
- 重新生成能力矩阵后，Admin `encrypted-blocked = 11`、`destructive-blocked = 29`、`raw-fallback = 0`，未误降级加密和破坏性边界。
- 新增测试覆盖 STS mock 响应下的 identity provider 桥接工厂。

## 阶段 97 Provider 与客户端 builder 桥接

- 所有公开客户端 builder 新增 `credentialsProvider(io.minio.reactive.credentials.Provider)` 重载。
- builder 内部统一使用 `ReactiveCredentialsProvider.from(provider)` 桥接，客户端运行时仍保持响应式凭证接口。
- 新增 `docs/95-stage97-provider-builder-integration.md`，记录使用方式和边界。
- `ReactiveCredentialsProvidersTest` 增加所有 builder 接受 `StaticProvider` 的覆盖。

## 阶段 96 credentials provider 类名覆盖收口

- 新增 minio-java 同名 credentials provider 迁移层，覆盖 `Credentials`、`Provider`、静态、链式、环境变量、AWS/MinIO 配置文件、JWT、STS identity provider 和 IAM 边界类。
- `ReactiveCredentialsProvider.from(Provider)` 支持把 minio-java 风格 provider 桥接到响应式客户端 builder。
- `Credentials.toString()` 默认脱敏，配置文件测试只使用临时假凭证，不输出 secret。
- `IamAwsProvider` 暂不自动访问元数据服务，先提供中文安全失败边界，避免隐式网络访问。
- 新增 `docs/94-stage96-credentials-provider-parity.md` 与 `ReactiveCredentialsProvidersTest`。
- 重新生成 minio-java 对标报告后，credentials provider 按类名缺失为 0；对象、Admin、Args 继续保持满格。

## 阶段 95 剩余对象与分片 Args builder 收口

- 新增剩余 38 个 `*Args` 类，覆盖对象治理、分片上传、PromptObject、SelectObjectContent、通知监听和 minio-java 基础继承体系名称。
- `ReactiveMinioClient` 为可执行 Args 增加重载，继续委托既有强类型方法；基础继承体系类只作为迁移兼容父类，不伪装成独立业务能力。
- `scripts/report-minio-java-parity.py` 结论增加 Args 完成口径，避免 Args 已满格后继续提示 Args 是主要缺口。
- 新增 `docs/93-stage95-remaining-object-args-builder.md`，记录 Args 收口范围、边界和后续 credentials provider 重点。
- 重新生成 minio-java 对标报告后，`*Args` builder 达到 86 / 86；对象和 Admin 核心 API 继续保持精确同名满格。

## 阶段 94 bucket 子资源 Args builder 补齐

- 新增 26 个 bucket 子资源 `*Args` 类，覆盖 tags、CORS、policy、lifecycle、versioning、notification、encryption、object-lock、replication 的 get/set/delete 迁移入口。
- `ReactiveMinioClient` 新增对应 Args 重载，继续复用既有强类型方法和 XML/JSON 子资源流程。
- `BaseArgs` 增加字符串 Map 防御性复制和中文校验，避免空 tags/key/value 进入 HTTP 层。
- 新增 `docs/92-stage94-bucket-subresource-args-builder.md`，记录 bucket 子资源 Args 范围、边界和后续配置对象深化方向。
- 重新生成 minio-java 对标报告后，`*Args` builder 从 22 / 86 提升到 48 / 86。

## 阶段 93 对象存储高频 Args builder 起步

- 新增 22 个对象存储相关 `*Args` 类，覆盖 bucket、object、文件上传下载、append、compose、presign、FanOut 和 Snowball 的高频迁移路径。
- `ReactiveMinioClient` 新增 Args 重载，继续委托既有强类型方法，避免把 Args 入口做成 raw 包装。
- Builder 增加中文基础校验，避免空 bucket/object/filename/列表等错误进入 HTTP 层。
- 新增 `docs/91-stage93-object-args-builder.md`，记录本阶段 Args 范围、边界和后续补齐方向。
- 重新生成 minio-java 对标报告后，`*Args` builder 从 0 / 86 提升到 22 / 86；对象和 Admin 核心 API 继续保持精确同名满格。

## 阶段 92 Admin 核心 API 同名覆盖收口

- `ReactiveMinioAdminClient` 新增 `addUpdateGroup`、`removeGroup`、`attachPolicy`、`detachPolicy`、`setPolicy`、`clearBucketQuota`、`listServiceAccount`、`getServiceAccountInfo` 等 minio-java 同名迁移入口。
- 新增入口继续使用 Admin 专用客户端已有协议实现和类型边界，不退回 raw client；服务账号读取仍以 `EncryptedAdminResponse` 明确保留 Crypto Gate 边界。
- `scripts/report-minio-java-parity.py` 的结论改为根据当前缺口动态输出，避免在对象/Admin 都收口后继续提示“缺失对象 API”。
- 新增 `docs/90-stage92-admin-api-full-name-parity.md`，记录 Admin 同名入口、风险边界和后续 `*Args` / credentials provider 重点。
- 重新生成 minio-java 对标报告后，Admin 核心 API 精确同名达到 24 / 24，别名或部分覆盖 0 个，缺失 0 个。

## 阶段 91 对象存储核心 API 同名覆盖收口

- 新增 `PutObjectFanOutEntry`、`PutObjectFanOutResult`、`PutObjectFanOutResponse`，并实现 `ReactiveMinioClient.putObjectFanOut`。
- 新增 `SnowballObject`，并实现 `ReactiveMinioClient.uploadSnowballObjects` 的非压缩 tar 自动解包上传路径。
- 新增 `docs/89-stage91-object-api-full-name-parity.md`，记录 FanOut、Snowball、Snappy 压缩边界和剩余 SDK 对标重点。
- 重新生成 minio-java 对标报告后，对象存储核心 API 精确同名达到 59 / 59，缺失 0 个；后续转向 Admin 别名语义、`*Args` builder 与 credentials provider 体系。

## 阶段 90 promptObject 推理请求入口

- `ReactiveMinioClient` 新增 `promptObject` 多个强类型重载，按 POST + `lambdaArn` query + JSON body 调用 MinIO PromptObject 扩展。
- 响应返回 `Flux<byte[]>`，保留推理/对象 Lambda 场景可能出现的流式响应边界。
- 新增 `docs/88-stage90-prompt-object-parity.md`，记录 promptObject 请求流程、边界和 live/lab 待补证项。
- 重新生成 minio-java 对标报告后，对象存储核心 API 精确同名从 56 / 59 提升到 57 / 59，剩余缺口为 `putObjectFanOut`、`uploadSnowballObjects`。

## 阶段 89 appendObject 追加写入入口

- 新增 `ObjectWriteResult`，提取写入响应中的 ETag、versionId 并保留完整响应头。
- `ReactiveMinioClient` 新增 `appendObject` 字节、字符串和 Path 重载，先 HEAD 获取对象当前长度，再 PUT 携带 `x-amz-write-offset-bytes` 追加内容。
- 新增 `docs/87-stage89-append-object-parity.md`，记录追加写入流程、边界和后续 chunk/checksum 差距。
- 重新生成 minio-java 对标报告后，对象存储核心 API 精确同名从 55 / 59 提升到 56 / 59，剩余缺口为 `promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 阶段 88 getPresignedPostFormData 表单上传入口

- 新增 `PostPolicy`，支持 equals、starts-with、content-length-range 条件和保留字段保护。
- `ReactiveMinioClient` 新增 `getPresignedPostFormData(PostPolicy)`，由当前凭证生成浏览器表单上传需要的 policy、credential、date、signature 等字段。
- 新增 `docs/86-stage88-presigned-post-parity.md`，记录预签名 POST 与 minio-java 的对齐语义。
- 重新生成 minio-java 对标报告后，对象存储核心 API 精确同名从 54 / 59 提升到 55 / 59，剩余缺口为 `appendObject`、`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 阶段 87 composeObject 对象组合入口

- 新增 `ComposeSource`，用于描述对象组合的源 bucket、object、versionId 与可选字节范围。
- `ReactiveMinioClient` 新增 `uploadPartCopy` 与多组 `composeObject` 强类型入口，内部使用 multipart upload、multipart copy、complete multipart，并在失败时 abort 清理。
- 新增 `docs/85-stage87-compose-object-parity.md`，记录对象组合流程和与 minio-java 的对标边界。
- 重新生成 minio-java 对标报告后，对象存储核心 API 精确同名从 53 / 59 提升到 54 / 59，剩余缺口为 `appendObject`、`getPresignedPostFormData`、`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 阶段 86 minio-java 文件上传下载同名入口

- `ReactiveMinioClient` 新增 `downloadObject` / `uploadObject` 的 Path 与字符串文件名重载，默认语义对齐 minio-java：下载先 HEAD 再 GET、校验长度、写临时文件后移动，且默认不覆盖已有目标文件。
- 上传本地文件时会在 `boundedElastic` 线程读取普通文件，并在未显式传入 contentType 时尝试探测文件类型。
- 新增 `docs/84-stage86-upload-download-parity.md`，记录同名入口、minio-java 对齐点和剩余对象 API 缺口。
- 重新生成 minio-java 对标报告后，对象存储核心 API 精确同名从 51 / 59 提升到 53 / 59，剩余缺口为 `appendObject`、`composeObject`、`getPresignedPostFormData`、`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。

## 阶段 85 minio-java 主对标基线重建

- 新增 `scripts/report-minio-java-parity.py`，从同目录 `minio-java` 生成对象存储、Admin、`*Args`、credentials provider 对标报告。
- `ReactiveMinioClient` 补充 minio-java 同名包装方法，当前对象存储核心 API 从 41 个精确同名提升到 51 个，剩余 8 个明确缺口。
- 新增 `docs/83-stage85-minio-java-rebaseline.md`，明确 `minio-java` 是 SDK 主对标，服务端 `minio` 只作为协议真相来源。
- 本阶段不降低 Crypto/lab/release 外部门禁；后续转向 `*Args`、缺失对象 API、credentials provider 与 Admin Crypto 自动解密。

## 阶段 84 Crypto Gate 解释与 Docker 独立 lab 证据

- 新增 `docs/82-stage84-crypto-gate-docker-lab.md`，把 Crypto Gate 涉及接口、MinIO madmin 加密处理、解密条件和“不是一定可解密”的边界说明清楚。
- `scripts/minio-lab/run-destructive-tests.sh` 不再把 lab 端点复制到通用 `MINIO_ENDPOINT`，避免内部门禁误判 lab 与共享环境相同。
- 修正 bucket quota lab 示例字段为 madmin-go 实际使用的 `quotatype`。
- `DestructiveAdminIntegrationTest` 为 config KV 与 bucket quota 增加 raw 兜底写入/读取/恢复验证；JDK8 与 JDK17+ 均在 Docker 独立 MinIO lab 中通过。
- 本阶段只证明 config、bucket quota 与 remote target 只读探测的 typed/raw 路径，tier/remote target 写入、batch job、site replication 仍等待私有夹具，Crypto Gate 仍保持 Fail。

## 阶段 83 最终完成边界判定

- 新增 `docs/81-stage83-final-boundary.md`，明确当前“发布候选完成、正式发布阻塞于外部门禁”的结论。
- 当前 route parity 233 / 233、product-typed 233 / 233、raw-fallback 0；剩余 11 个加密边界和 29 个破坏性边界需要外部证据。
- 继续保持 `0.1.0-SNAPSHOT`，不打 tag、不发布 Maven。

## 阶段 82 mc 只读旁证刷新

- 使用临时 `mc` 配置目录执行只读命令，刷新共享 MinIO ready、online、region、bucket/object 数和磁盘摘要证据。
- 新增 `docs/80-stage82-mc-readonly-evidence-refresh.md`，记录只读证据和凭据边界。
- 本阶段不执行写入、不修改配置、不减少 `destructive-blocked = 29`。

## 阶段 81 示例中文诊断复核

- `ReactiveMinioLiveExample` 的终端输出改为中文，包括缺少配置、bucket/object 流程、S3 错误和响应头说明。
- 新增 `docs/79-stage81-example-chinese-diagnostics.md`，记录示例中文体验边界：只改用户输出，不改协议/API/门禁。
- 本阶段继续不改变 Crypto Gate、独立 lab 和 Maven 发布状态。

## 阶段 80 Maven 发布元数据预检

- 新增 `scripts/report-pom-release-metadata.py`，用标准库审计双分支 POM 的基础坐标、发布元数据和 source/javadoc/sign/SBOM 插件准备度。
- 新增 `docs/78-stage80-maven-release-metadata-preflight.md`，明确当前基础坐标齐全，但许可证、SCM、developers、distribution/signing/SBOM 等正式发布材料不能擅自补。
- 本阶段不改版本号、不打 tag、不发布 Maven，继续保持 `0.1.0-SNAPSHOT`。

## 阶段 79 发布暂缓后缺口再审计

- 新增 `docs/77-stage79-post-release-hold-gap-audit.md`，确认阶段 78 后没有新的 route/catalog/product/raw 缺口。
- 明确剩余差距是 Crypto Gate、独立破坏性 lab 和 Maven/tag 发布工程证据，不是继续新增重复 API。
- 下一轮安全推进方向收敛为 POM 发布元数据预检、用户示例/中文诊断和只读旁证扩展。

## 阶段 78 发布工程预检

- 新增 `docs/76-stage78-release-engineering-preflight.md`，明确当前只能作为发布候选继续验证，不能正式 tag/Maven 发布。
- 当前版本继续保持 `0.1.0-SNAPSHOT`；route parity 233 / 233、product-typed 233 / 233、raw-fallback 0 不替代 Crypto Gate 和独立 lab 证据。
- 发布工程的下一步仍是补齐 Crypto Gate Pass、独立破坏性 lab 报告、源码包/javadoc/pom 元数据/签名/校验和/许可证或 SBOM 等材料。

## 阶段 77 独立破坏性 lab 门禁复核

- 新增 `scripts/minio-lab/audit-readiness.sh`，用于在不连接 MinIO、不输出凭证的情况下审计独立 lab 门禁准备度。
- 新增 `docs/75-stage77-destructive-lab-gate-review.md`，记录当前没有真实 `lab.properties`、无环境变量拒绝和共享端点拒绝的验证口径。
- 本阶段不减少 `destructive-blocked = 29`，也不把共享 MinIO 当作真实破坏性写入验证通过。

## 阶段 76 Crypto Gate Pass 可执行清单

- 新增 `docs/74-stage76-crypto-gate-execution-checklist.md`，把 Crypto Gate Pass 前必须完成的依赖审查、批准材料、测试矩阵和失败回退语义整理为清单。
- 本阶段不新增依赖、不修改 `pom.xml`、不放行 Crypto Gate；`encrypted-blocked = 11` 继续保持。
- 明确即使未来 Gate Pass，也必须保留 `EncryptedAdminResponse` 回退和安全诊断语义。

## 阶段 75 新口径缺口审计

- 新增 `docs/73-stage75-post-stage74-gap-audit.md`，基于 `encrypted-blocked = 11` 重新审计当前 SDK 缺口。
- 结论：route parity、product-typed 与 raw-fallback 均已满格；剩余是 11 个加密边界、29 个破坏性边界和发布工程门禁。
- 明确后续不应通过新增重复 API、伪装明文模型或共享端点破坏性写入来宣称完成。

## 阶段 74 IDP 加密边界统计修正

- 能力矩阵把 `ADMIN_LIST_IDP_CONFIG` 与 `ADMIN_GET_IDP_CONFIG` 纳入 Crypto Gate 统计。
- 当前 Admin `encrypted-blocked` 从 9 修正为 11；这是更真实的风险统计，不是 Crypto Gate 放行或 SDK 覆盖退化。
- 新增 `docs/72-stage74-encrypted-boundary-accounting.md` 记录修正依据、当前 11 个加密边界和验证口径。

## 阶段 73 加密响应安全诊断增强

- `EncryptedAdminResponse` 新增 `encryptedSize()`、`decryptSupported()`、`requiresCryptoGate()` 与 `diagnosticMessage()`。
- 诊断方法只暴露算法、字节数和 Gate 状态，不暴露 secret、token、配置值或明文响应。
- 新增 `docs/71-stage73-encrypted-response-diagnostics.md` 记录使用方式、安全边界和验证口径。

## 阶段 72 IDP 配置加密边界补充

- `ReactiveMinioAdminClient` 新增 `listIdpConfigsEncrypted(type)` 与 `getIdpConfigEncrypted(type, name)`。
- 明确 MinIO IDP 配置读取真实响应属于 madmin 加密边界，默认 Crypto Gate 未通过前不伪装成明文配置模型。
- 新增 `docs/70-stage72-idp-config-encrypted-boundary.md` 记录使用方式、安全边界和验证口径。

## 阶段 71 Replication MRF 只读模型补充

- 新增 `AdminReplicationMrfSummary`，用于解析 `replication/mrf` 的按行 JSON backlog 与 keep-alive 空白。
- `ReactiveMinioAdminClient` 新增 `getReplicationMrfSummary(bucket)`，原 `getReplicationMrfInfo(bucket)` 与 `replicationMrf(bucket)` 继续保留。
- 新增 `docs/69-stage71-replication-mrf-readonly-models.md` 记录 MRF backlog 只读边界、字段口径和验证要求。

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
