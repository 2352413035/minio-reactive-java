# 调试记录与踩坑总结

这份文档记录的是当前原型在接入真实 MinIO 过程中，已经实际遇到过的问题。
这些记录很重要，因为它们会告诉你：响应式 SDK 的问题，很多不是“语法问题”，而是“协议细节”和“框架行为”问题。

## 1. 依赖问题：只有 spring-webflux 还不够

### 现象

最初项目里只有 `spring-webflux` 和 `reactor-core`。

### 问题本质

`WebClient` 是一个抽象客户端入口，但它仍然需要实际的 HTTP client 实现。
如果没有合适的 Reactor Netty 依赖，运行时可能没有真正的 HTTP 发送能力。

### 修正

在 `pom.xml` 中显式加入：

1. `spring-webflux`
2. `reactor-netty-http`

同时去掉单独声明的 `reactor-core`，避免版本漂移风险。

## 2. PUT Bucket 失败，MinIO 服务端报 XML EOF

### 现象

`bucketExists` 可以执行，但 `makeBucket` 返回失败。

MinIO 服务端日志出现了类似信息：

1. `PutBucketHandler`
2. `parseLocationConstraint`
3. `XML syntax error on line 0: EOF`

### 问题本质

之前的 `PUT Bucket` 发的是空 body。
但服务端在解析 bucket 创建时期待 `LocationConstraint` 的 XML 内容。

### 修正

在 [ReactiveMinioClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\ReactiveMinioClient.java) 的 `makeBucket()` 中：

1. 显式构造 `CreateBucketConfiguration`
2. 写入 `LocationConstraint`
3. 设置 `Content-Type: application/xml`
4. 设置 `Content-Length`

### 学到什么

这说明“一个 PUT 请求发出去了”不等于“协议是对的”。
S3/MinIO 的很多操作不是简单 HTTP 动词，而是带有严格 body 结构要求的协议调用。

## 3. 403 问题不一定是签名错，也可能是账号权限或配置

### 现象

一度遇到 `403`，但没有明显 XML 错误体。

### 问题本质

`403` 可能来自多种原因：

1. access key / secret key 不对
2. 当前账号没有建桶权限
3. region 配置不一致
4. 签名细节不完全匹配

### 调试经验

要先分清失败发生在哪一步：

1. `bucketExists`
2. `makeBucket`
3. `putObject`
4. `getObject`

不能只看一条长栈。
Reactor 的堆栈会很长，但真正有价值的信息通常只有：

1. HTTP 状态码
2. 错误响应体
3. 出错步骤

## 4. 不该随意改用户本地有效凭证

### 现象

示例代码里一度把用户原本可用的本地凭证改成了 `minioadmin/minioadmin`，导致本来可用的测试环境重新失败。

### 问题本质

这不是协议问题，而是配置管理问题。
对本地真实环境来说，硬编码默认凭证很容易破坏已有可用配置。

### 修正

后来改成：

1. 支持项目内配置文件 `config/minio-local.properties`
2. 环境变量可覆盖配置文件
3. 代码里不再依赖写死的默认密钥

### 学到什么

示例程序不能只“为了跑起来”，还要考虑真实开发环境下的可维护性。

## 5. GET Object 成功但下载内容为空

### 现象

真实 MinIO 测试时：

1. 上传成功
2. `HEAD object` 成功
3. `GET object` 没报错
4. 但 `downloaded content:` 是空字符串

### 问题本质

这是当前接入过程里最有代表性的一个 WebFlux 问题。

最初 HTTP 层先用 `exchangeToMono(Mono::just)` 拿到 `ClientResponse`，再在外面读取 body。
这样会让 body 的消费时机脱离 `exchangeToMono` 的处理回调。

对 `GET` 这类真正依赖响应体的请求来说，响应体可能在之后已经不可再安全消费，于是表面看起来“请求成功了”，但内容是空的。

### 修正

在 [ReactiveHttpClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\http\ReactiveHttpClient.java) 中改成：

1. 在 `exchangeToMono` 回调内部直接 `bodyToMono(byte[].class)`
2. 在 `exchangeToFlux` 回调内部直接 `bodyToFlux(DataBuffer.class)`
3. 不再把 `ClientResponse` 取出后延迟消费 body

### 学到什么

这正是“会写响应式语法”和“真正理解响应式客户端行为”的区别。

在响应式 HTTP 客户端里，响应体的生命周期是有约束的。
如果你处理错了时机，错误未必会表现成异常，也可能只是数据为空。

## 6. User-Agent 不应轻易参与签名

### 现象

接入时对签名头的处理一度不稳定。

### 问题本质

像 `User-Agent`、`Accept-Encoding` 这类头，可能被客户端框架自动补充或重写。
如果把这类不稳定头部纳入签名，服务端验签就可能失败。

### 修正

在 [S3RequestSigner.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\signer\S3RequestSigner.java) 中：

1. `authorization` 不参与 canonical headers
2. `user-agent` 不参与 canonical headers
3. `accept-encoding` 不参与 canonical headers

### 学到什么

不是“头越多越严谨”。
SigV4 里最重要的是：参与签名的内容必须稳定，而且要和真正发出去的请求一致。

## 7. 当前阶段为什么要把这些问题记下来

因为这些问题正是你后面继续做：

1. `listObjects`
2. XML 解析
3. presigned URL
4. multipart upload

时会重复遇到的同类问题。

这份记录的价值不是“留档”，而是帮你建立一种排查顺序：

1. 先看失败在哪一步
2. 再分协议问题、配置问题、框架行为问题
3. 不要先怀疑 Reactor 语法
4. 优先核对 method、URI、headers、body、签名参与项
