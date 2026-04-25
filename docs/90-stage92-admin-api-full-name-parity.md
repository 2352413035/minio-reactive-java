# 阶段 92：Admin 核心 API 同名覆盖收口

## 目标

阶段 91 后，对象存储核心 API 已经与 `minio-java` 达到 59 / 59 精确同名，但 Admin 核心 API 仍有 8 个别名或部分覆盖入口。阶段 92A 的目标是补齐这些迁移入口，让用户从官方 Java SDK 切换到响应式 SDK 时，常见 Admin 方法名不需要重新记忆。

## 新增同名入口

本阶段在 `ReactiveMinioAdminClient` 中补充以下与 `minio-java` 同名的方法：

- `addUpdateGroup`：新增或更新用户组成员。
- `removeGroup`：删除用户组。
- `attachPolicy`：把内置策略绑定到用户或用户组。
- `detachPolicy`：从用户或用户组解绑内置策略。
- `setPolicy`：兼容旧式用户/用户组策略绑定入口。
- `clearBucketQuota`：清空 bucket quota。
- `listServiceAccount`：按用户名列出服务账号加密响应。
- `getServiceAccountInfo`：按 access key 获取服务账号加密响应。

这些方法不是把调用退回 `ReactiveMinioRawClient`，而是继续落在 `ReactiveMinioAdminClient` 现有 Admin 协议实现和类型边界之上。

## 设计边界

- `attachPolicy` / `detachPolicy` 要求 `user` 与 `group` 必须且只能提供一个，避免请求体语义不明确。
- 服务账号读取类接口仍返回 `EncryptedAdminResponse`，因为真实 MinIO madmin 响应需要 Crypto Gate 条件才能解密。SDK 不会把加密字节伪装成普通明文对象。
- `clearBucketQuota` 是写入类 Admin 操作，单元测试验证请求构造；真实共享 MinIO 环境仍不运行破坏性写入。
- 同名入口只解决迁移命名体验，不替代后续 `*Args` builder 与 credentials provider 体系。

## 当前 minio-java 对标报告

阶段 92 后重新生成报告：

- 对象存储核心 API：`59` 个。
- 对象存储精确同名：`59` 个，缺失 `0` 个。
- Admin 核心 API：`24` 个。
- Admin 精确同名：`24` 个，别名或部分覆盖 `0` 个，缺失 `0` 个。
- `*Args` builder：当前 reactive 同名数量仍为 `0`，这是阶段 93 的优先事项。
- credentials provider：当前只有响应式基础 provider，仍需要补迁移友好的环境变量、链式和配置 provider。

## 验证

- `ReactiveMinioSpecializedClientsTest` 增加同名入口反射覆盖。
- 单元测试验证用户组、策略、服务账号、bucket quota 的请求 path/query 构造。
- 双分支重新生成 `minio-java` 对标报告，确认 Admin 别名或部分覆盖项已经清零。
