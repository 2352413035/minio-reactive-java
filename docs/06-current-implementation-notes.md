# 当前实现说明

这份文档只讲当前 `minio-reactive-java` 已经落地的部分，目的是让你读代码时先建立一个准确的心智模型。

## 1. 当前项目已经实现了什么

当前原型已经和真实 MinIO 跑通了下面这条最小闭环：

1. `bucketExists`
2. `makeBucket`
3. `putObject`
4. `statObject`
5. `getObjectAsString`
6. `removeObject`
7. `removeBucket`

对应核心入口在：

- [ReactiveMinioClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\ReactiveMinioClient.java)
- [ReactiveMinioLiveExample.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\examples\ReactiveMinioLiveExample.java)

## 2. 当前项目的分层

### 2.1 对外 API 层

`ReactiveMinioClient` 是当前唯一的对外客户端。
它负责提供 `Mono` / `Flux` 风格的方法，但它自己不直接拼签名算法，也不直接操作 `WebClient` 的细节。

这一层做的事很简单：

1. 根据业务动作构造 `S3Request`
2. 取凭证
3. 调签名器
4. 调 HTTP 层

这是一个很重要的设计点。
如果这四步揉在一个方法里，代码会很快变成不可读。

### 2.2 请求模型层

`S3Request` 是内部请求模型。
它把一个 S3 请求拆成这些部分：

1. `method`
2. `bucket`
3. `object`
4. `headers`
5. `queryParameters`
6. `body`
7. `contentType`

为什么需要这一层：

1. 签名器要拿到“标准化后的请求”
2. HTTP 层也要拿到“同一个请求”
3. 如果签名时看的 URI 和真正发出去的 URI 不一致，就会签名失败

所以 `S3Request` 的核心价值不是“封装”，而是“让签名和发送共享同一份请求语义”。

### 2.3 签名层

`S3RequestSigner` 负责最小可用的 AWS Signature V4。

它当前做了这些事：

1. 计算 payload SHA-256
2. 生成 `X-Amz-Date`
3. 生成 canonical request
4. 生成 string to sign
5. 推导 signing key
6. 生成 `Authorization` header

这部分是整个 SDK 最协议化的地方。
它不是 MinIO 私有逻辑，而是 S3 兼容协议的核心要求。

### 2.4 HTTP 层

`ReactiveHttpClient` 负责真正发请求。

当前基于：

1. `Spring WebFlux WebClient`
2. `reactor-netty-http`

这里要特别注意：

- 当前项目是响应式 API
- 但“响应式 API”不等于“实现绝对完美”
- 真正的复杂点在于 body 的生命周期、错误传播、流式处理和内存模型

目前 HTTP 层已经把 2xx 和非 2xx 分开处理，并把错误统一包装成 `ReactiveS3Exception`。

## 3. 当前项目和官方 minio-java 的关系

当前这个原型不是照搬 `minio-java`，而是借鉴它的核心职责分离：

1. 客户端门面
2. 请求模型
3. 签名
4. HTTP 发送
5. 凭证提供

但它和官方项目有两个关键差别：

1. 官方项目更完整，协议覆盖面远大于当前原型
2. 官方项目对外既有同步 API，也有基于 `CompletableFuture` 的异步 API，而当前原型直接以 Reactor 为主

## 4. 当前项目还没有实现什么

现在不要误以为项目已经接近完成。
它只是完成了一个“真实可验证的最小版本”。

当前还没做的重点包括：

1. `listObjects`
2. XML 响应解析
3. XML 错误解析
4. presigned URL
5. multipart upload
6. region 自动发现
7. 更完整的 credentials provider
8. 更完整的异常模型
9. 大文件真正的端到端流式上传/下载

## 5. 为什么先做到这里是合理的

这是工程上很重要的一步。

如果一开始就同时做：

1. WebFlux
2. SigV4
3. XML
4. multipart
5. presigned URL
6. region 发现

那你很难判断问题究竟出在哪一层。

当前先把最小闭环跑通的好处是：

1. 已经确认基础架构方向对了
2. 已经确认真实 MinIO 可验证
3. 后续新增能力时，回归范围更清晰

## 6. 建议你怎么读当前代码

推荐按这个顺序读：

1. [ReactiveMinioLiveExample.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\examples\ReactiveMinioLiveExample.java)
2. [ReactiveMinioClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\ReactiveMinioClient.java)
3. [S3Request.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\http\S3Request.java)
4. [S3RequestSigner.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\signer\S3RequestSigner.java)
5. [ReactiveHttpClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\http\ReactiveHttpClient.java)

这个顺序对应的是：

1. 先看业务流程
2. 再看 API 入口
3. 再看请求怎么被表达
4. 再看签名怎么生成
5. 最后看请求怎么发出去
