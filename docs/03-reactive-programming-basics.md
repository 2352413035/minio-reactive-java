# 03 响应式开发基础

## 1. 你要先区分三个概念

### 1.1 阻塞

调用线程发出请求后，必须等结果回来，线程才能继续往下走。

典型表现：

- `InputStream.read()`
- `Future.get()`
- `CompletableFuture.join()`

### 1.2 异步

调用发出去之后，当前线程可以先做别的事情，结果以后再通知你。

典型表现：

- callback
- `CompletableFuture`

### 1.3 响应式

响应式不只是“异步”。

它更强调：

- 异步数据流
- 声明式组合
- 背压
- 上下游传播模型

Java 里最常见的响应式抽象就是：

- `Mono<T>`
- `Flux<T>`

## 2. 为什么 `CompletableFuture` 不等于 WebFlux

`CompletableFuture` 能表达：

- 一个未来结果

但它不擅长表达：

- 多个数据项的流
- 背压
- 对网络字节流的响应式处理
- 与 `Publisher` 生态的自然组合

而 `WebFlux/Reactor` 擅长这些。

所以：

- `CompletableFuture` 更像异步任务抽象
- `Mono/Flux` 更像异步流抽象

## 3. Reactor 的两个核心类型

### 3.1 Mono

表示：

- 0 个或 1 个结果

适合：

- `bucketExists`
- `statObject`
- `removeObject`

### 3.2 Flux

表示：

- 0 到多个结果

适合：

- `listObjects`
- 下载对象字节流
- 上传时的分块数据流

## 4. WebFlux 对 MinIO SDK 有什么意义

如果你的 SDK 是响应式的，那么它就可以更自然地和这些代码配合：

- Spring WebFlux Controller
- `WebClient`
- `Flux<DataBuffer>`
- Reactor pipeline

例如上传文件时，你更希望拿到的是：

- `Flux<DataBuffer>`

而不是：

- `InputStream`

因为前者更适合端到端非阻塞传输。

## 5. 为什么当前 minio-java 不够“WebFlux 友好”

虽然它已经异步化了一部分，但它对外和内部仍然大量依赖：

1. `CompletableFuture`
2. `InputStream`
3. `RandomAccessFile`
4. 某些链路中的 `.join()`

这会让它在 WebFlux 项目里使用时出现几个问题：

1. API 不是 `Mono/Flux`
2. 数据流模型不统一
3. 背压不是主设计目标
4. 很容易在接入层又包出新的阻塞点

## 6. 你做 reactive SDK 时要坚持的原则

### 6.1 公共 API 优先返回 Mono/Flux

不要再设计为：

- `CompletableFuture<T>`

### 6.2 网络层优先使用 WebClient

因为它和 Spring WebFlux 生态天然一致。

### 6.3 数据读取优先用响应式流

下载尽量返回：

- `Flux<byte[]>`
- 或 `Flux<DataBuffer>`

上传尽量接受：

- `Flux<byte[]>`
- 或 `Publisher<DataBuffer>`

### 6.4 阻塞操作要明确隔离

如果某个阶段暂时必须兼容阻塞组件：

- 用 `boundedElastic`
- 并明确标识这是过渡方案

不要把阻塞逻辑默默混进主链路。

## 7. 你做 MinIO reactive SDK 时最常见的误区

### 7.1 误区一：把 CompletableFuture 简单包成 Mono 就算响应式

不够。

这只是外观上换了返回值，不是设计真的响应式。

### 7.2 误区二：继续用 InputStream 做核心输入输出模型

这会让“响应式”只剩外壳。

### 7.3 误区三：把所有东西都塞进一个大 Client 类

后面 multipart、presign、listObjects、错误处理都会变乱。

### 7.4 误区四：一开始就追求全功能

会失控。

应该先跑通最小闭环。

## 8. 你现在应该建立的正确心智模型

你不是在做：

- “把现有 MinIO Java SDK 改个 API 名字”

而是在做：

- “保留 S3 协议知识和 SDK 分层思想”
- “重写一套更适合 Reactor/WebFlux 的传输和流模型”

这两者差别很大。
