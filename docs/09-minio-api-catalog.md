# 09 MinIO API 目录覆盖说明

当前项目提供两层 API：

1. `ReactiveMinioClient`：面向常用 S3 对象存储场景的强类型、Reactor 风格便捷客户端。
2. `ReactiveMinioRawClient` + `MinioApiCatalog`：面向 MinIO 服务端公开 HTTP 路由的完整目录和原始响应式执行器。

这两层的关系是：常用对象存储操作优先使用强类型客户端；管理端、KMS、STS、监控、健康检查等接口优先使用对应专用客户端；尚未建模或新出现的接口再通过原始执行器完整暴露。

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

这些专用客户端的方法名来自 `MinioApiCatalog` 中的接口名。当前已经为 Health、Metrics、STS、KMS、Admin 增加了第一批强业务入口，例如健康检查布尔结果、Prometheus 文本包装、STS 临时凭证解析、KMS/Admin JSON 包装。其它非对象存储接口仍可能先返回原始文本响应；后续会继续补充更细的请求对象、响应对象、XML/JSON 解析和业务语义。

强类型客户端应复用 `MinioApiCatalog` 和 `ReactiveMinioRawClient`，避免重复维护路径、query、认证方式和签名逻辑。
