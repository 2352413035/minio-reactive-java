# 阶段 33：S3 通知监听流式边界

阶段 33 把 S3 通知监听从“目录型兼容入口”提升为产品化业务入口。它的重点不是解析所有事件字段，而是先修正响应式边界：通知监听是长连接事件流，不能被包装成一次性 `Mono<String>`。

## 1. 新增入口

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `listenBucketNotification(bucket, events)` | `Flux<byte[]>` | 监听指定 bucket 的事件通知，调用方负责订阅、超时和取消。 |
| `listenRootNotification(events)` | `Flux<byte[]>` | 监听根路径事件通知，这是 MinIO 扩展能力，适合运维或测试场景。 |

旧的 `s3ListenBucketNotification(...)` / `s3ListenRootNotification(...)` 仍保留为 advanced 兼容入口，但它们是一次性字符串读取方式，不适合真实长连接监听。

## 2. 设计边界

- 当前阶段返回字节流，不强行假设服务端事件分片和 JSON 行格式。
- 后续如果要升级为 `Flux<NotificationEvent>`，必须先固定事件帧格式、心跳、断线重连和 backpressure 行为。
- 真实业务代码应在订阅端设置超时、重试和取消逻辑，避免测试或服务关闭时悬挂长连接。

## 3. 对完成度的影响

阶段 33 后，S3 product-typed 口径达到 77 / 77。这里的 77 / 77 表示 S3 公开路由已经都有产品化入口或明确产品边界，不表示所有事件流字段都已经结构化解析。
