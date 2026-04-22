# 04 minio-reactive-java 设计方案

## 1. 设计目标

这个项目的第一目标不是立刻追平官方 SDK 的所有能力。

第一阶段目标是：

1. 设计清晰
2. 代码可读
3. 适合学习
4. 能逐步扩展
5. 与 WebFlux 生态自然兼容

## 2. 第一版建议能力范围

MVP 只做这些：

1. `bucketExists`
2. `statObject`
3. `removeObject`
4. `putObject`
5. `getObject`
6. `listObjects`
7. `getPresignedObjectUrl`

先不做：

1. admin API
2. Snowball
3. SelectObjectContent
4. fan-out
5. append object
6. 全量 SSE 场景
7. 全量 STS/provider

## 3. 包结构建议

建议使用下面的结构：

```text
io.minio.reactive
  client
  config
  credentials
  errors
  http
  messages
  operations
  signer
```

当前为了控制复杂度，代码先放在较少包里，后续再细分。

## 4. 核心抽象

### 4.1 ReactiveMinioClient

职责：

- 对外统一入口
- 暴露用户真正关心的 API

### 4.2 ReactiveMinioClientConfig

职责：

- 管理 endpoint、region、bucket style、超时等配置

### 4.3 ReactiveCredentialsProvider

职责：

- 用响应式方式解析凭证

设计成：

- `Mono<ReactiveCredentials> getCredentials()`

这样比同步 `fetch()` 更适合未来扩展动态刷新。

### 4.4 S3Request

职责：

- 在业务 API 和 WebClient 之间，建立一个稳定的内部请求模型

### 4.5 S3RequestSigner

职责：

- 单独负责签名
- 避免把签名逻辑散落在 HTTP 层

### 4.6 ReactiveHttpClient

职责：

- 真正发送请求
- 统一处理响应状态和响应体

## 4.7 当前 SDK 分层理念

当前 SDK 不再把所有能力都塞进一个大客户端，而是分成“目录、兜底调用器、专用客户端”三层。

第一层是 `MinioApiCatalog`。它是 MinIO 公开接口目录，集中记录接口名、分组、HTTP 方法、路径模板、query 要求和认证方式。这样做的目的，是先保证本地 MinIO 公开路由在 SDK 中有统一登记，不会因为强类型模型还没写完就完全无法调用。

第二层是 `ReactiveMinioRawClient`。它是兜底原始调用器，按照目录条目构造请求并发送。它适合两类场景：

1. SDK 暂时还没有封装某个 MinIO 新接口时，用户可以先通过 raw client 调用。
2. 某些管理端或监控接口返回结构还没有强类型模型时，可以先拿原始 XML、JSON、文本或字节结果。

第三层是专用客户端：

- `ReactiveMinioClient`：对象存储专用客户端，面向上传、下载、列对象、对象标签、预签名、分片上传等常用 S3/MinIO 操作。
- `ReactiveMinioAdminClient`：管理端专用客户端。
- `ReactiveMinioKmsClient`：KMS 专用客户端。
- `ReactiveMinioStsClient`：临时凭证 STS 专用客户端。
- `ReactiveMinioMetricsClient`：监控指标专用客户端。
- `ReactiveMinioHealthClient`：健康检查专用客户端。

这种拆分的原则是：业务常用能力做成更好用的专用客户端；所有公开接口都保留 raw 兜底入口；后续新增强类型方法时，复用目录和 raw client，不重复维护路径、query 和认证逻辑。

## 5. 为什么不直接把所有逻辑塞进 WebClient 调用链里

因为那样很快会失控。

你后面需要面对：

1. header 计算
2. query 组装
3. presign
4. 错误响应
5. retry
6. region 发现
7. multipart

如果没有清晰中间抽象，代码会很快变成一大片 lambda。

## 6. 当前代码骨架的定位

当前代码骨架做的是三件事：

1. 把配置、凭证、请求模型抽出来
2. 提前留好签名扩展点
3. 建立一个可以持续迭代的结构

它故意没有在这一轮把所有 API 一次性写完。

原因是：

- 先把结构做对，比先把功能堆满更重要

## 7. 当前实现策略

### 7.1 传输层

第一阶段用 `WebClient`。

理由：

- 与 Spring WebFlux 兼容
- 学习成本低于直接操作 Reactor Netty 原始 API
- 代码可读性更高

### 7.2 签名层

第一阶段先实现基础签名入口和规范化逻辑。

后续逐步补齐：

1. 普通请求签名
2. presign
3. streaming / multipart 场景签名

### 7.3 数据模型

第一阶段用：

- `Mono<Boolean>`
- `Mono<Void>`
- `Mono<Map<String, List<String>>>`
- `Flux<byte[]>`

后续可以再引入更细的对象模型。

## 8. 代码注释原则

后续所有代码都按这个原则写注释：

1. 注释解释“为什么”
2. 不解释显而易见的 Java 语法
3. 在协议相关、签名相关、响应式流边界处重点写

## 9. 这一版文档和代码如何配合

建议你的学习方式是：

1. 先读 `02-minio-java-architecture.md`
2. 再读 `03-reactive-programming-basics.md`
3. 再读这篇设计文档
4. 然后回头读代码骨架

这样你会知道：

- 当前项目为什么这么写
- 为什么 reactive 版本不能简单照抄
- 我们新的代码为什么要这么分层
