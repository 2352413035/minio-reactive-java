# 02 minio-java 当前项目架构与实现方式

## 1. 先说结论

`minio-java` 当前不是“纯阻塞项目”。

更准确地说，它是：

- 一个以 `OkHttp` 为底层 HTTP 客户端的 S3/MinIO Java SDK
- 同时提供同步客户端和异步客户端
- 异步模型是 `CompletableFuture`
- 同步模型是在异步模型上调用 `.join()`

所以它不是 Reactor/WebFlux 风格的响应式 SDK，但也不能简单归类为“只有阻塞式实现”。

## 2. 模块结构

仓库主要有四个模块：

1. `api`
   - 核心对象存储 SDK
2. `adminapi`
   - MinIO 管理 API SDK
3. `examples`
   - 示例代码
4. `functional`
   - 功能测试

其中真正最核心的是 `api`。

## 3. 关键依赖

`minio-java` 主要依赖这些技术：

1. `OkHttp`
   - 负责 HTTP 调用
2. `Jackson`
   - 负责 JSON 解析
3. `simple-xml-safe`
   - 负责 XML 解析
4. `Guava`
   - 通用工具支持
5. `BouncyCastle`
   - 加密支持
6. `commons-compress` 与 `snappy-java`
   - 压缩、Snowball 等扩展能力

## 4. 分层理解

这个项目大致可以拆成六层。

### 4.1 对外门面层

核心类：

- `MinioClient`
- `MinioAsyncClient`

职责：

- 对外暴露“我能做什么”
- 给使用方一个稳定、简单的 API 入口

### 4.2 核心 S3 执行层

核心类：

- `BaseS3Client`

职责：

- 处理大多数底层 S3 API 执行细节
- 组织请求发送
- 解析错误
- 处理 region
- 处理 trace

### 4.3 参数对象层

核心类：

- `BaseArgs`
- `BucketArgs`
- `ObjectArgs`
- 各种 `*Args`

职责：

- 把每个 API 的参数模型对象化
- 做参数校验
- 把不合法输入挡在发送请求之前

### 4.4 HTTP 请求构造层

核心类：

- `Http.BaseUrl`
- `Http.S3Request`
- `Http.Headers`
- `Http.QueryParameters`

职责：

- 组织 endpoint、bucket、object、query、headers
- 构造成真正的 HTTP 请求

### 4.5 认证与签名层

核心类：

- `Signer`
- `credentials` 包

职责：

- 提供凭证
- 做 AWS Signature V4 签名
- 支持 presign

### 4.6 协议消息与错误层

核心包：

- `messages`
- `errors`

职责：

- 把 XML/JSON 响应映射成 Java 对象
- 把错误响应映射成 SDK 异常

## 5. 当前项目“不是纯阻塞”的证据

### 5.1 底层 HTTP 是异步的

`BaseS3Client.executeAsync()` 使用的是 `OkHttp` 的异步回调：

- `newCall(...)`
- `enqueue(...)`

这说明底层传输并不是同步 `execute()` 模式。

### 5.2 对外有异步客户端

`MinioAsyncClient` 直接对外返回：

- `CompletableFuture<T>`

例如：

- `getObject(...)`
- `putObject(...)`
- `statObject(...)`

### 5.3 同步客户端是对异步客户端的包装

`MinioClient` 并没有自己实现完整一套同步 HTTP 栈。

它很多方法本质上是：

1. 调用 `asyncClient.xxx(...)`
2. 再 `.join()`

所以同步 API 是建立在异步 API 之上的阻塞封装。

## 6. 但它为什么仍然不是你想要的响应式 SDK

因为它的异步模型不是 Reactor 模型。

它现在是：

- `OkHttp async callback`
- `CompletableFuture`

而不是：

- `Publisher`
- `Mono`
- `Flux`
- backpressure-first 设计

另外，它内部仍然存在这些响应式不友好的点：

1. 大量 `InputStream`
2. `RandomAccessFile`
3. 内部多处 `.join()`
4. 某些流程仍然混入阻塞思维

这意味着：

- 它支持异步
- 但不是端到端响应式
- 与 WebFlux 的自然集成度不够高

## 7. 一次调用是怎么走下去的

以 `putObject` 为例，可以这样理解：

1. 业务代码调用 `MinioClient.putObject(...)`
2. `MinioClient` 转发给 `MinioAsyncClient.putObject(...)`
3. `MinioAsyncClient` 做高层上传流程编排
4. `BaseS3Client` 构造真正 S3 HTTP 请求
5. `Http.S3Request` 组装 URL、headers、body
6. `Signer` 完成签名
7. `OkHttp.enqueue(...)` 异步发送
8. 返回 `CompletableFuture`
9. 如果是同步客户端，则上层 `.join()`

## 8. 这个架构的优点

1. 分层比较清晰
2. 参数对象建模成熟
3. 协议细节封装较完整
4. 同时支持同步和异步调用
5. examples 和 functional test 很适合学习

## 9. 这个架构对你做响应式版本的启发

你不应该推翻它所有设计。

你应该保留这些思想：

1. 参数对象建模
2. 请求构造与签名分离
3. 协议对象独立
4. 凭证 provider 抽象
5. 错误模型独立

你要替换的是：

1. 传输层
   - 从 `OkHttp` 换成 `WebClient`
2. 异步抽象
   - 从 `CompletableFuture` 换成 `Mono/Flux`
3. 流模型
   - 从 `InputStream` 优先换成响应式数据流优先

## 10. 这一篇你应该记住的最重要一句话

`minio-java` 现在是“异步内核 + 同步门面”的 SDK，不是“只有阻塞式实现的 SDK”，但它也还不是“响应式 SDK”。
