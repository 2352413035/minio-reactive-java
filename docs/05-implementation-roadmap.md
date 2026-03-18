# 05 实施路线图

## 阶段 1：读懂现有 minio-java

产出：

1. 当前 SDK 架构理解
2. 同步与异步关系理解
3. 连接 MinIO 的协议要点理解

本阶段重点结论：

- 当前 `minio-java` 不是纯阻塞
- 它是 `OkHttp async + CompletableFuture + 同步 join 包装`

## 阶段 2：打基础骨架

产出：

1. Maven 工程配置
2. 配置对象
3. 凭证抽象
4. 请求对象
5. HTTP 客户端封装
6. 签名器入口

## 阶段 3：实现最小功能闭环

建议顺序：

1. `bucketExists`
2. `statObject`
3. `removeObject`

这些 API 好处是：

- 请求体简单
- 协议理解成本低
- 最适合先验证签名与错误处理

## 阶段 4：实现对象上传下载

建议顺序：

1. `putObject(byte[])`
2. `getObject() -> Flux<byte[]>`
3. `putObject(Publisher<byte[]>)`

本阶段重点：

- 先跑通简单数据流
- 后处理大文件和分块

## 阶段 5：实现 list 与 presign

建议顺序：

1. `listObjects`
2. `getPresignedObjectUrl`

本阶段重点：

- query 参数规范
- XML 列表响应解析
- presign 规范

## 阶段 6：实现 multipart upload

这是难度明显提升的一阶段。

你需要额外处理：

1. 初始化上传
2. 分块编号
3. 分块 checksum
4. 合并完成
5. 失败回滚

## 阶段 7：补文档与测试

你最终需要沉淀的不是只有代码。

还应该有：

1. 调用链图
2. 类图
3. 常见错误说明
4. 与官方 `minio-java` 的差异说明
5. WebFlux 集成示例
