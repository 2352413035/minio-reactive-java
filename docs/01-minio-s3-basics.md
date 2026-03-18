# 01 MinIO 与 S3 基础

## 1. MinIO 是什么

MinIO 是一个对象存储服务。

它最重要的特征不是“它有自己的独立协议”，而是：

- 它实现了 Amazon S3 兼容 API。
- Java SDK 实际上是在调用 S3 兼容 HTTP 接口。

所以你学习 MinIO Java SDK，本质上同时也在学习 S3 兼容对象存储 SDK。

## 2. 对象存储和文件系统的区别

对象存储的核心概念是：

- `bucket`：桶，类似逻辑容器。
- `object`：对象，类似文件，但从协议角度它是一个带元数据的二进制对象。

和本地文件系统不同：

- 对象存储没有真正的目录。
- `/` 只是对象名中的一个普通字符。
- 所谓“目录结构”通常只是前缀模拟出来的。

例如：

- `images/a.png`
- `images/2026/03/a.png`

它们只是对象名，不是本地文件夹。

## 3. 一个最基本的 MinIO 连接需要什么

最基本需要四类信息：

1. `endpoint`
   - 例如 `http://localhost:9000`
2. `accessKey`
3. `secretKey`
4. `bucket` 与 `object`

有些场景还需要：

5. `region`
6. `session token`
7. TLS/证书配置

## 4. MinIO Java SDK 实际在做什么

以 `putObject` 为例，SDK 并不是“调用某个内部 Java 方法”就完成上传。

它实际上做了这些事情：

1. 校验 bucket/object 名称是否合法。
2. 构造 HTTP 请求。
3. 计算请求签名。
4. 设置 `Host`、`X-Amz-Date`、`Authorization` 等头。
5. 把对象内容作为请求体发送给 MinIO。
6. 解析响应头和响应体。
7. 转成 Java 对象返回给调用方。

## 5. 连接 MinIO 的规范本质是什么

你后面要实现响应式 SDK，核心规范就是这些：

1. HTTP/HTTPS 请求规范
2. S3 bucket/object 命名规则
3. AWS Signature V4 签名规范
4. S3 的查询参数和 header 规范
5. XML/JSON 响应格式
6. multipart upload 规范

这也是为什么一个 MinIO SDK 并不只是“封装一个 HTTP Client”那么简单。

## 6. 你后面需要重点掌握的 8 个知识点

### 6.1 Endpoint

决定：

- 连接哪个服务
- 使用 HTTP 还是 HTTPS
- 端口是多少

### 6.2 Bucket naming

Bucket 名称要满足 S3 规则。

例如：

- 只能小写字母、数字、点、横线
- 不能像 IP 地址
- 不能出现非法组合

### 6.3 Region

S3 请求很多时候和 region 有关。

有些服务端会要求：

- 你必须用正确 region 去签名
- 如果 region 错误，服务端会返回重定向或错误信息

### 6.4 Credentials

常见有两类：

1. 静态凭证
   - `accessKey + secretKey`
2. 临时凭证
   - `accessKey + secretKey + sessionToken`

### 6.5 Signature V4

这是请求认证的核心。

SDK 必须把请求内容、header、query、日期、region 等组合起来，计算签名。

### 6.6 Presigned URL

它不是普通 `Authorization` 头签名，而是把签名信息放到 URL query 参数里。

### 6.7 Multipart upload

大文件上传通常不能直接单次 PUT。

而是：

1. 初始化 multipart upload
2. 逐片上传
3. 合并完成

### 6.8 Error model

服务端错误并不总是普通 HTTP 文本。

很多时候是：

- HTTP 状态码 + S3 XML 错误体

SDK 需要把它解析成结构化异常。

## 7. 为什么这些知识对响应式版本也一样重要

即使你换成 WebFlux，也只是传输实现换了：

- 从 `OkHttp + CompletableFuture`
- 变成 `WebClient + Mono/Flux`

但是这些协议要求没有变：

- endpoint 还是要拼
- header 还是要算
- 签名还是要做
- multipart 还是要守协议
- 错误响应还是要解析

所以你的学习顺序必须是：

1. 先懂协议
2. 再懂当前 SDK
3. 再做响应式重构
