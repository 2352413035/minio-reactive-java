# 阶段 91：对象存储核心 API 同名覆盖收口

## 目标

阶段 90 后，`minio-java` 对象存储核心 API 59 个中已有 57 个精确同名，剩余 `putObjectFanOut` 与 `uploadSnowballObjects`。阶段 91 补齐这两个 MinIO 扩展入口，使对象存储核心 API 达到 59 / 59 精确同名。

## 新增 FanOut 能力

- `PutObjectFanOutEntry`：FanOut 目标对象描述，支持 key、用户元数据、tags 与 contentType。
- `PutObjectFanOutResult`：单个目标对象的写入结果，包含 key、etag、versionId、error。
- `PutObjectFanOutResponse`：FanOut 响应，保留原始响应文本并解析每个目标结果。
- `ReactiveMinioClient.putObjectFanOut(...)`：复用 `PostPolicy` 生成 presigned POST form-data，再构造 multipart/form-data 请求，携带 `x-minio-fanout-list` 与 file 内容。

FanOut 表单上传不再额外加 Authorization 头，因为签名已经体现在 `policy` 与 `x-amz-signature` 表单字段中。

## 新增 Snowball 能力

- `SnowballObject`：Snowball 批量上传中的单个对象，支持内存字节、字符串和本地文件来源。
- `ReactiveMinioClient.uploadSnowballObjects(...)`：将多个对象打包成 tar，上传为 `snowball.<uuid>.tar`，并设置 `X-Amz-Meta-Snowball-Auto-Extract=true` 让 MinIO 自动解包。

当前 Snowball 实现先支持非压缩 tar。minio-java 的 Snappy 压缩路径需要额外依赖评审，本阶段不新增依赖，因此 `compression=true` 会显式返回中文不支持错误。

## 当前 minio-java 对标报告

阶段 91 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 精确同名：`59` 个。
- 缺失：`0` 个。
- Admin API：`24` 个中无完全缺失，但仍有 8 个别名或部分覆盖项。
- `*Args` builder 与 credentials provider 体系仍是后续重点。

## 验证

- 单元测试覆盖 FanOut multipart POST 路径、contentType、无 Authorization、结果解析和空 entry 中文错误。
- 单元测试覆盖 Snowball 上传路径、自动解包元数据头、tar contentType、ObjectWriteResult 和空对象列表/压缩不支持错误。
- 双分支重新生成 minio-java 对标报告，确认对象存储核心 API 59 / 59 精确同名。
