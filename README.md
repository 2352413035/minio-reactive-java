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

## 完整 MinIO 接口覆盖

除常用 S3 对象存储方法外，项目还提供：

- `MinioApiCatalog`：覆盖本地 `minio` 路由文件中的 S3/Admin/KMS/STS/Health/Metrics 233 个 HTTP 接口。
- `ReactiveMinioRawClient`：对 catalog 中任意接口执行响应式签名请求，适合 admin/KMS/STS 等暂未强类型建模的接口。

详见 `docs/09-minio-api-catalog.md`。

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
