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

- SigV4 普通请求签名与 presigned URL 签名
- `listBuckets`
- `bucketExists`
- `makeBucket`
- `removeBucket`
- `getBucketLocation`
- `listObjects` / `listObjectsPage`
- `putObject`（byte[]、String、Publisher<byte[]> 便捷入口）
- `getObject`
- `getObjectRange`
- `getObjectAsBytes`
- `getObjectAsString`
- `statObject`
- `copyObject`
- `removeObject`
- `removeObjects`
- object/bucket tagging
- bucket policy/lifecycle/versioning/notification/encryption/object-lock/replication XML/JSON 子资源入口
- multipart upload 基础流程：create/uploadPart/listParts/complete/abort

这些能力已经通过 JDK8 单元测试以及真实 MinIO 集成测试进行了验证。

## 运行真实 MinIO 示例

示例入口：

- `io.minio.reactive.examples.ReactiveMinioLiveExample`

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

详见 `docs/04-minio-reactive-java-design.md` 和 `docs/09-minio-api-catalog.md`。

## 文档目录

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
