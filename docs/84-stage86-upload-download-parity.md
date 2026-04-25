# 阶段 86：补齐 minio-java 文件上传下载同名入口

## 目标

阶段 85 重新以同目录 `minio-java` 为主对标项目后，对象存储核心 API 仍缺少 `downloadObject` 与 `uploadObject` 等 8 个同名入口。阶段 86 先补齐文件上传/下载这两个高频迁移入口，降低从官方 Java SDK 迁移到响应式 SDK 时的心智成本。

## 本阶段新增能力

- `ReactiveMinioClient.downloadObject(bucket, object, Path filename)`：下载对象到本地文件，默认不覆盖已有文件。
- `ReactiveMinioClient.downloadObject(bucket, object, Path filename, boolean overwrite)`：可显式允许覆盖。
- `ReactiveMinioClient.downloadObject(bucket, object, String filename)` 与带 `overwrite` 的字符串重载：便于从 minio-java 的 `filename` 迁移。
- `ReactiveMinioClient.uploadObject(bucket, object, Path filename)`：上传本地普通文件，未指定 `contentType` 时尝试根据文件探测，失败则回落到 `application/octet-stream`。
- `ReactiveMinioClient.uploadObject(bucket, object, Path filename, String contentType)` 与字符串重载：支持显式内容类型。

## 与 minio-java 的对齐点

`downloadObject` 不只是简单把 GET 响应写入文件，而是保留了 minio-java 的关键安全步骤：

1. 先用 HEAD 获取对象元数据。
2. 再用 GET 读取对象内容。
3. 根据 `Content-Length` 校验下载字节数。
4. 先写入同目录 `.part.minio` 临时文件。
5. 校验通过后再移动到目标文件。
6. 默认不覆盖已有文件，调用方必须显式传入 `overwrite=true` 才会覆盖。

`uploadObject` 当前复用已有 `putObject` 传输能力。由于现有 `putObject` 仍以 byte[] 请求体为主，本阶段先在 `boundedElastic` 线程读取本地文件，避免把阻塞文件 IO 放到响应式主链路线程中。后续补齐分片/大文件能力时，会继续保留同名入口并升级内部实现。

## 当前 minio-java 对标报告

阶段 86 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 精确同名：`53` 个。
- 缺失：`6` 个。
- 剩余缺失：`appendObject`、`composeObject`、`getPresignedPostFormData`、`promptObject`、`putObjectFanOut`、`uploadSnowballObjects`。
- Admin API：仍为 `24` 个中无完全缺失，另有 8 个别名或部分覆盖项。
- `*Args` builder 与 credentials provider 体系仍是后续阶段重点。

## 验证

- 单元测试新增文件上传/下载同名入口测试，覆盖 PUT、HEAD、GET、contentType、目标文件写入与默认禁止覆盖行为。
- 双分支均重新生成 `minio-java` 对标 markdown/json 报告。
- 后续提交前需继续执行 JDK8/JDK17 全量测试，以及 JDK21/JDK25 对 JDK17+ 分支的 `test-compile` 验证。
