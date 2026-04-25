# S3/MinIO 方法级协议说明

这份文档按“方法”的角度来讲当前项目已经实现的 S3/MinIO 请求协议。
目标是让你以后实现一个方法时，可以先翻这份文档，确认：

1. 用什么 HTTP 方法
2. 路径长什么样
3. query 参数是什么
4. 请求头要带什么
5. 请求体要带什么
6. 哪些内容要参与签名
7. 成功时预期返回什么

## 1. 先回答：为什么要签名

### 1.1 签名的作用

S3 协议本质上是 HTTP 协议上的一层“带认证规则的对象存储协议”。

如果只是普通 HTTP：

- 客户端可以直接发 `GET /bucket/object`
- 服务端只能看到“有人发请求了”
- 但无法确认这个请求是不是合法用户发的，也无法确认请求内容有没有被篡改

SigV4 签名解决的是两个问题：

1. 证明“这个请求确实是拿着 accessKey/secretKey 的客户端发出来的”
2. 证明“参与签名的 method、URI、query、headers、body 摘要没有在传输中被篡改”

### 1.2 签名不是随便签一段字符串

SigV4 规定了固定流程：

1. 先构造 canonical request
2. 再构造 string to sign
3. 再用 secretKey 推导 signing key
4. 最后生成 signature

也就是说，签名不是“你自己决定签什么”，而是协议规定了必须签什么。

### 1.3 当前项目里参与签名的核心内容

以当前实现为准，签名依赖这些内容：

1. HTTP 方法，例如 `GET`、`PUT`、`HEAD`、`DELETE`
2. canonical URI，例如 `/bucket/object`
3. canonical query string，例如 `list-type=2&prefix=a`
4. canonical headers
5. signed headers 名单
6. payload SHA-256

对应代码在：

- [S3RequestSigner.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\signer\S3RequestSigner.java)

### 1.4 当前项目里常见的签名相关请求头

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `X-Amz-Security-Token`，仅临时凭证场景需要
5. `Authorization`

注意：

- `Authorization` 是签名结果，不是原始输入的一部分
- `User-Agent` 当前会发送，但不会参与签名
- `Accept-Encoding` 当前不会参与签名

## 2. 一个 S3 请求最少要包含什么

如果你要写一个最基础的签名请求，至少要先明确这些要素：

1. endpoint，例如 `http://127.0.0.1:9000`
2. bucket，例如 `demo-bucket`
3. object，例如 `hello.txt`
4. HTTP 方法
5. query 参数
6. 请求体
7. `Content-Type`
8. `Content-Length`
9. region，例如 `us-east-1`
10. accessKey / secretKey

然后才能生成：

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `Authorization`

## 3. 当前项目的统一请求规则

## 3.1 路径风格

当前原型使用 path-style：

- bucket 在路径里
- 不是放在子域名里

例如：

```text
http://127.0.0.1:9000/my-bucket/hello.txt
```

不是：

```text
http://my-bucket.127.0.0.1:9000/hello.txt
```

## 3.2 canonical URI 规则

当前由 [S3Request.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\http\S3Request.java) 和 [S3Escaper.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\util\S3Escaper.java) 负责：

1. bucket 和 object 会组合成路径
2. object 内部的 `/` 会保留层级含义
3. 其他需要转义的字符会做 percent-encoding

## 3.3 payload hash 规则

当前实现里：

- 有 body 时，对 body 做 SHA-256
- 没有 body 时，对空字节数组做 SHA-256

空 body 的标准 SHA-256 是固定值，对理解 `HEAD/GET/DELETE` 请求很重要。

## 3.4 当前项目里对 MinIO region 的认知

这是当前阶段非常重要的认知点。

在你现在的 MinIO 环境里，`GetBucketLocation` 表现出来的行为更接近：

1. 反映 MinIO 服务端当前的全局 region 配置
2. 而不是 AWS S3 语义下“每个 bucket 都有一个强独立、稳定的 region 属性”

你当前的实测现象是：

1. MinIO 没有设置 region 时，`GetBucketLocation` 返回空值
2. 修改 MinIO 服务端 region 后，`GetBucketLocation` 返回你设置的 region

这说明在当前环境中：

- bucket location 更像是服务端全局 region 的协议映射
- 因此客户端签名 region、`makeBucket` 的 `LocationConstraint`、MinIO 服务端配置的 region，必须保持一致

所以当前原型里，region 至少会出现在三个地方：

1. 客户端配置 `ReactiveMinioClient.builder().region(...)`
2. `makeBucket()` XML 里的 `LocationConstraint`
3. SigV4 的 scope 中，例如 `20260407/ap-southeast-1/s3/aws4_request`

这三者只要有一个不一致，就很容易出现“查到是这个 region，但用这个 region 仍然报错”的情况。

## 4. 方法级协议说明

下面以当前已经实现的方法为准。

## 4.1 `bucketExists`

### 业务含义

检查 bucket 是否存在。

### HTTP 方法

`HEAD`

### 路径

```text
/{bucket}
```

示例：

```text
HEAD /demo-bucket
```

### query 参数

无。

### 请求体

无。

### 关键请求头

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `Authorization`

### 成功响应

- `200` 或其他 `2xx`：bucket 存在
- `404`：bucket 不存在

### 当前代码中的特殊处理

`404` 被转换成 `false`，而不是继续抛异常。

对应代码：

- [ReactiveMinioClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\ReactiveMinioClient.java)

## 4.2 `makeBucket`

### 业务含义

创建一个 bucket。

### HTTP 方法

`PUT`

### 路径

```text
/{bucket}
```

### query 参数

无。

### 请求体

当前原型发送 XML：

```xml
<CreateBucketConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
  <LocationConstraint>your-region</LocationConstraint>
</CreateBucketConfiguration>
```

### 关键请求头

1. `Host`
2. `Content-Type: application/xml`
3. `Content-Length`
4. `X-Amz-Date`
5. `X-Amz-Content-Sha256`
6. `Authorization`

### 为什么这里必须有 body

这是一个很关键的点。

真实调试里，空的 `PUT Bucket` 请求曾触发 MinIO 服务端在解析 `LocationConstraint` 时出现 EOF。
这说明服务端并不是只看“你用了 PUT”，还会去解析请求体结构。

### 当前项目里 region 的额外要求

这里不能把 `LocationConstraint` 写死成某个值。
必须和：

1. 客户端配置 region
2. SigV4 签名使用的 region
3. MinIO 服务端当前配置的 region

保持一致。

### 成功响应

- `200` 或 `204` 之类 `2xx`

## 4.3 `removeBucket`

### 业务含义

删除一个 bucket。

### HTTP 方法

`DELETE`

### 路径

```text
/{bucket}
```

### query 参数

无。

### 请求体

无。

### 关键请求头

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `Authorization`

### 成功前提

bucket 必须为空。
否则很多 S3 兼容实现都会拒绝删除。

## 4.4 `putObject`

### 业务含义

上传一个对象。

### HTTP 方法

`PUT`

### 路径

```text
/{bucket}/{object}
```

示例：

```text
PUT /demo-bucket/hello.txt
```

### query 参数

无。

### 请求体

对象内容本身。
例如：

```text
你好，来自 reactive minio sdk
```

### 关键请求头

1. `Host`
2. `Content-Type`
3. `Content-Length`
4. `X-Amz-Date`
5. `X-Amz-Content-Sha256`
6. `Authorization`

### 请求体和签名的关系

这里和 `HEAD/DELETE` 不同。
`putObject` 有真实 body，因此：

1. body 的字节内容会影响 `X-Amz-Content-Sha256`
2. body 摘要会进入 canonical request
3. 如果 body 在签名前后不一致，验签就会失败

### 成功响应

通常是 `200`。
响应头里常能看到：

1. `ETag`
2. `X-Amz-Request-Id`

## 4.5 `getObject`

### 业务含义

下载一个对象。

### HTTP 方法

`GET`

### 路径

```text
/{bucket}/{object}
```

### query 参数

无。

### 请求体

无。

### 关键请求头

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `Authorization`

### 成功响应

响应体就是对象内容本身。
例如：

```text
你好，来自 reactive minio sdk
```

### 当前项目里的一个重要实现经验

在响应式 HTTP 客户端里，不能先把 `ClientResponse` 取出来再延迟读取 body。
否则会出现请求成功但读取到空内容的问题。

当前修正后的实现要求：

1. 在 `exchangeToMono` 内直接 `bodyToMono(byte[].class)`
2. 在 `exchangeToFlux` 内直接 `bodyToFlux(DataBuffer.class)`

## 4.6 `getObjectAsBytes`

### 业务含义

这是 `getObject` 的便捷包装。
它不是新的协议方法，而是把 `GET Object` 的响应体一次性读入内存。

### 适用场景

1. 示例
2. 小文件
3. 单元测试

### 不适用场景

1. 超大对象
2. 希望完整保留流式 backpressure 的下载逻辑

## 4.7 `getObjectAsString`

### 业务含义

这是 `getObjectAsBytes` 再加上一层字符集解码。

### 协议层面

协议上仍然是同一个 `GET Object` 请求。
变化只发生在客户端收到字节后，怎么把它转成字符串。

## 4.8 `statObject`

### 业务含义

获取对象元数据，不下载对象内容。

### HTTP 方法

`HEAD`

### 路径

```text
/{bucket}/{object}
```

### query 参数

无。

### 请求体

无。

### 关键请求头

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `Authorization`

### 典型响应头

1. `Content-Length`
2. `Content-Type`
3. `ETag`
4. `Last-Modified`

### 当前代码中的用途

你现在的示例里就是先 `putObject`，再 `statObject`，用来确认对象元数据已经正确落库。

## 4.9 `removeObject`

### 业务含义

删除对象。

### HTTP 方法

`DELETE`

### 路径

```text
/{bucket}/{object}
```

### query 参数

无。

### 请求体

无。

### 关键请求头

1. `Host`
2. `X-Amz-Date`
3. `X-Amz-Content-Sha256`
4. `Authorization`

### 成功响应

一般是 `204` 或其他 `2xx`。

## 5. 从代码层面看，请求是怎么一步步形成的

当前项目里一个请求的形成顺序是：

1. 业务方法构造 `S3Request`
2. 凭证提供者返回 `ReactiveCredentials`
3. `S3RequestSigner` 计算签名并补标准头
4. `ReactiveHttpClient` 发送请求并解析响应

对应入口：

- [ReactiveMinioClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\ReactiveMinioClient.java)
- [S3RequestSigner.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\signer\S3RequestSigner.java)
- [ReactiveHttpClient.java](D:\LearningCode\minio-reactive-java\src\main\java\io\minio\reactive\http\ReactiveHttpClient.java)

## 6. 如果你自己新写一个方法，要先核对什么

以后你每写一个新方法，建议按下面顺序检查：

1. 这个方法对应的 HTTP 方法是什么
2. 路径是 bucket 级还是 object 级
3. 有没有 query 参数
4. 有没有请求体
5. `Content-Type` 是什么
6. `Content-Length` 要不要带
7. payload hash 该怎么算
8. 哪些头需要参与签名
9. 成功时应该读响应头、响应体，还是都不读
10. 服务端失败时，错误体通常是什么格式
11. region 在客户端配置、请求体、签名 scope 三个位置是否一致

## 7. 当前文档的边界

这份文档当前只覆盖已经实现的方法。
还没有展开这些能力：

1. `listObjects`
2. presigned URL
3. multipart upload
4. XML 列表解析
5. 更复杂的 SSE / retention / tagging / versioning

这些能力后面实现时，建议继续按这个文档格式追加维护。
