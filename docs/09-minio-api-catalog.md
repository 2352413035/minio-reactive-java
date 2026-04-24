# 09 MinIO API 目录覆盖说明

当前项目提供三类 API：

1. `ReactiveMinioClient`：面向常用 S3 对象存储场景的强类型、Reactor 风格便捷客户端。
2. `ReactiveMinioAdminClient`、`ReactiveMinioKmsClient`、`ReactiveMinioStsClient`、`ReactiveMinioMetricsClient`、`ReactiveMinioHealthClient`：和对象存储客户端平级的专用领域客户端。
3. `ReactiveMinioRawClient` + `MinioApiCatalog`：面向 MinIO 服务端公开 HTTP 路由的完整目录和原始响应式执行器。

它们的关系是：常用对象存储操作优先使用 `ReactiveMinioClient`；管理端、KMS、STS、监控、健康检查等接口优先使用对应专用客户端；尚未建模或新出现的接口再通过 raw 完整暴露。raw 是兜底能力，不是普通业务主路径。

## 已覆盖的路由来源

接口目录对照本地 `minio` 项目中的下列公开路由文件整理：

- `minio/cmd/api-router.go`
- `minio/cmd/admin-router.go`
- `minio/cmd/kms-router.go`
- `minio/cmd/sts-handlers.go`
- `minio/cmd/healthcheck-router.go`
- `minio/cmd/metrics-router.go`

## 接口分组

当前目录共覆盖 233 个公开 HTTP 接口。

| 分组 | 数量 | 来源说明 |
| --- | ---: | --- |
| `s3` | 77 | S3 对象、桶、根路径相关接口 |
| `admin` | 128 | MinIO 管理端 `/minio/admin/v3` 接口 |
| `kms` | 7 | MinIO KMS `/minio/kms/v1` 接口 |
| `health` | 8 | `/minio/health/*` 存活和就绪检查接口 |
| `metrics` | 6 | Prometheus 以及 metrics v2/v3 监控接口 |
| `sts` | 7 | AWS STS 兼容接口 |

## 当前完成度口径

阶段 19 之后，项目用以下口径说明“对标 MinIO”的进度，不再使用单一完成百分比：

| 口径 | JDK8 / JDK17+ 当前结果 | 解释 |
| --- | --- | --- |
| route parity | 233 / 233，missing 0，extra 0 | SDK catalog 与本地 `minio` router 在 family/method/path/query/auth 语义上对齐。 |
| callability | raw-fallback 0 | 每个公开 catalog 路由至少有 typed 或 advanced 兼容入口。 |
| product typed | S3 77 / 77、Admin 113 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8 | 这是用户友好强类型成熟度，不代表剩余路由不能调用。 |
| blocked risk | encrypted-blocked 9、destructive-blocked 29 | 这些接口受 madmin 加密响应或破坏性操作边界限制，不能在共享环境中伪装成普通 typed 完成。 |

机器报告统一由 `scripts/report-route-parity.py` 和 `scripts/report-capability-matrix.py` 生成，当前结果见：

- `.omx/reports/route-parity-jdk8.md`
- `.omx/reports/route-parity-jdk17.md`
- `.omx/reports/capability-matrix.md`

## 使用示例

```java
ReactiveMinioClient client =
    ReactiveMinioClient.builder()
        .endpoint("http://127.0.0.1:9000")
        .region("us-east-1")
        .credentials(accessKey, secretKey)
        .build();

ReactiveMinioRawClient raw =
    ReactiveMinioRawClient.builder()
        .endpoint("http://127.0.0.1:9000")
        .region("us-east-1")
        .credentials(accessKey, secretKey)
        .build();

String xml = raw.executeToString(
    MinioApiCatalog.byName("S3_LIST_BUCKETS"),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    Collections.<String, String>emptyMap(),
    null,
    null).block();
```

建议使用方式：

- 普通对象存储流程优先使用 `ReactiveMinioClient`。
- 管理端能力优先使用 `ReactiveMinioAdminClient`。
- KMS 能力优先使用 `ReactiveMinioKmsClient`。
- 临时凭证能力优先使用 `ReactiveMinioStsClient`。
- 监控指标优先使用 `ReactiveMinioMetricsClient`。
- 健康检查优先使用 `ReactiveMinioHealthClient`。
- 如果 SDK 暂时没有跟上某个新增 API，或者专用客户端还没有更细的返回模型，可以使用 `ReactiveMinioRawClient` 兜底调用；raw 不作为普通业务首选路径。
- 后续如果为某类接口补充更强的请求/响应模型，不应删除 `MinioApiCatalog` 中已有的原始接口覆盖。

当前仓库还提供四个可编译示例：

- `ReactiveMinioLiveExample`：对象存储主路径。
- `ReactiveMinioTypedAdminExample`：Admin 只读 typed 模型与加密边界。
- `ReactiveMinioRawFallbackExample`：raw 兜底调用 S3 与 Admin 原始接口。
- `ReactiveMinioOpsExample`：Health 与 Metrics 运维入口。

## 认证语义

目录中的每个接口都记录认证语义：

- `sigv4`：使用 AWS Signature V4 签名。
- `bearer`：使用调用方提供的 `Authorization: Bearer ...`，当前用于 metrics 相关接口。
- `none`：不需要 SDK 自动签名，当前用于健康检查等接口。

具体约定：

- S3、Admin、KMS 以及需要签名的 STS 接口使用 SigV4。
- STS 的签名表单接口使用 `sts` service scope；S3、Admin、KMS 使用 `s3` service scope。
- Metrics 默认按 MinIO 的 JWT/Bearer 认证方式建模；如果服务端配置为公开 metrics，调用时可以不传 `Authorization`。
- 原始客户端会拒绝调用方为 SigV4 接口手动传入 `Host`、`Authorization`、`X-Amz-Date`、`X-Amz-Content-Sha256`、`X-Amz-Security-Token` 等由签名器管理的头。

## 设计边界

`MinioApiCatalog` 覆盖的是 MinIO 服务端公开、面向客户端或管理端调用的 HTTP 接口。

`minio/cmd/routers.go` 中还会在分布式 erasure 场景注册一些内部协议路由，例如：

- storage REST
- peer REST
- bootstrap REST
- namespace lock REST
- grid route

这些属于 MinIO 节点之间的内部通信协议，不是稳定的公开 SDK 接口。本项目当前不把它们纳入 Java SDK 的公开 API 面。如果未来确实要调研这些内部协议，应单独设计内部协议客户端，而不是混入普通 MinIO SDK。

## 专用客户端拆分

当前已经按接口分组提供专用客户端入口：

1. `ReactiveMinioClient`：对象存储相关接口。
2. `ReactiveMinioAdminClient`：管理端接口。
3. `ReactiveMinioKmsClient`：KMS 接口。
4. `ReactiveMinioStsClient`：STS 临时凭证接口。
5. `ReactiveMinioMetricsClient`：监控指标接口。
6. `ReactiveMinioHealthClient`：健康检查接口。

这些专用客户端的方法名既保留 `MinioApiCatalog` 中的接口名以便对照，又逐步补充用户更容易理解的业务方法。当前已经为 Health、Metrics、STS、KMS、Admin 增加了强业务入口，例如健康检查布尔结果、Prometheus 文本包装和样本解析、STS 临时凭证解析、KMS/Admin JSON 摘要模型；KMS 与 Metrics 的指标入口均已进入 Prometheus typed 包装；Admin 继续补充 pool、rebalance、tier、site replication、peer IDP、top locks、OBD、health info 等只读或安全摘要入口；S3 已继续补充对象属性、对象保留策略、Legal Hold、归档恢复、bucket CORS、website、logging、policy status、accelerate、request payment 等模型。其它非对象存储接口仍可能先返回原始文本响应；后续会继续补充更细的请求对象、响应对象、XML/JSON 解析和业务语义。

强类型客户端不应只是把 `ReactiveMinioRawClient` 再包一层，而应像 `ReactiveMinioClient` 一样表达清楚请求对象、响应模型、错误语义和安全边界。它们可以复用 `MinioApiCatalog` 的路径、query、认证方式等元数据来避免重复维护，也可以复用底层签名和 HTTP 发送能力；`ReactiveMinioRawClient` 的定位是兜底调用器，用于 SDK 尚未产品化某个接口或用户需要直接访问新增 MinIO route 的场景。

## 路由对标（route parity）机器门禁

阶段 14 开始，`MinioApiCatalog` 不再只依赖手工计数说明。维护者应使用 `scripts/report-route-parity.py` 从本地 `minio` 服务端路由文件抽取 route baseline，并和 SDK catalog 做语义对照。

推荐命令：

```bash
python3 scripts/report-route-parity.py \
  --minio-root /dxl/minio-project/minio \
  --worktree /dxl/minio-project/minio-reactive-java \
  --format markdown \
  --output /dxl/minio-project/.omx/reports/route-parity-jdk8.md
```

这个门禁对照的不是单纯数量，而是以下关键语义：

- family：接口所属分组，例如 s3、admin、kms、sts、metrics、health。
- method：HTTP 方法。
- path template：路径模板，正则路由会收敛成稳定占位符。
- default query：固定 query，例如 `type=2`、`Action=AssumeRoleWithWebIdentity`。
- required query：必填 query，例如 `bucket`、`accessKey`、`uploadId`。
- auth scheme：`sigv4`、`bearer`、`none`。

脚本会排除 MinIO 服务端明确拒绝或未实现的 dummy/rejected route，避免把不可用路由包装成 SDK 产品能力。条件路由会在报告中保留说明，例如分布式/纠删码模式才注册的 heal、pool、rebalance，以及受 `enableConfigOps` 控制的配置接口。

`src/test/resources/minio-route-baseline.json` 是当前 MinIO router 抽取出的测试基线，`MinioApiCatalogTest` 会用它校验 SDK catalog 的 family/method/path/query/auth 语义。如果升级 `minio` 对照项目后该测试失败，必须先看路由对标报告，不要只修改计数。
