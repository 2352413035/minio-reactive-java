# 阶段 101：PutObjectAPIArgs builder 边界判定

## 背景

阶段 100 的签名级报告把 `PutObjectAPIArgs` 标记为“已存在但未扫描到 `builder/of/create` 入口”。本阶段专门对照 `minio-java` 的上传参数层级，判断它到底是迁移缺口，还是响应式 SDK 应保留的内部边界。

## 对照结论

在 `minio-java` 中，`PutObjectAPIArgs` 是更底层的上传执行参数：它可以包裹 `PutObjectBaseArgs`、`AppendObjectArgs`、`UploadSnowballObjectsArgs`，并携带 `RandomAccessFile`、内部 `ByteBuffer`、`byte[]`、headers、content type 等阻塞客户端执行细节。

在当前响应式 SDK 中，用户侧上传入口已经由更高层参数承担：

- `PutObjectArgs`
- `UploadObjectArgs`
- `AppendObjectArgs`
- `UploadSnowballObjectsArgs`

`PutObjectAPIArgs` 只保留为内部上传参数边界，不直接暴露 builder。这样可以避免用户绕过高层响应式上传语义，直接依赖阻塞实现里的文件句柄、内部缓冲和 header 组装细节。

## 本阶段变更

签名级报告脚本新增 `INTENTIONAL_ARG_BOUNDARIES` 分类，把 `PutObjectAPIArgs` 从“未解释的 builder 缺口”移动到“响应式 SDK 有意保留为内部边界”。报告会明确写出原因，而不是简单隐藏差异。

## 当前报告状态

- Args 类缺失：无。
- 已存在但未扫描到 `builder/of/create` 入口的 Args：无。
- 响应式 SDK 有意保留为内部边界的 Args：`PutObjectAPIArgs`。

## 后续规则

如果后续真的需要公开更底层上传执行参数，应重新设计一个响应式原生的执行参数对象，而不是直接复制 minio-java 的阻塞 `RandomAccessFile` / `ByteBuffer` / header 组合模型。
