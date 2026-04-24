# 变更日志

本文件记录 SDK 里程碑级变化。当前项目仍处于 `0.1.0-SNAPSHOT`，阶段 26 是“对标 MinIO 路由完整、调用入口完整、风险边界明确”的发布候选收口，不等同于 1.0 稳定版。

## 0.1.0-SNAPSHOT 阶段 26 发布候选

### 已完成

- 对照本地 `minio` 服务端公开路由，SDK catalog 覆盖 233 / 233，JDK8 与 JDK17+ 分支均无缺失、无额外 catalog。
- 所有 catalog 路由均有 typed 或 advanced 兼容入口，能力矩阵 `raw-fallback = 0`。
- `ReactiveMinioClient` 覆盖对象存储主流程、分片上传、版本列表、对象治理、bucket 子资源治理等常用路径。
- `ReactiveMinioAdminClient` 覆盖安全只读摘要、IAM、用户、用户组、策略、服务账号和风险分层入口。
- `ReactiveMinioKmsClient`、`ReactiveMinioStsClient`、`ReactiveMinioMetricsClient`、`ReactiveMinioHealthClient` 均作为平级专用客户端提供。
- `ReactiveMinioRawClient` 保留为新增接口和特殊接口的兜底入口。
- madmin PBKDF2 + AES-GCM 写入方向和 fixture 解密已支持；默认 Argon2id / ChaCha20 加密响应保持 `EncryptedAdminResponse` 边界。
- destructive Admin 测试已迁移到独立 lab 门禁和本机配置文件，默认共享 MinIO 测试不会执行破坏性写入。

### 仍需显式说明的边界

- Admin `encrypted-blocked = 9`：Crypto Gate Pass 前不提供默认 madmin 加密响应的明文 typed 解析。
- Admin `destructive-blocked = 29`：需要独立可回滚 lab，不能在共享 MinIO 环境默认执行。
- Admin、STS、S3 中仍有一批 advanced-compatible 能力尚未升级为最终产品级 typed 模型。

### 阶段 26 验证

- JDK8：单元测试、真实 MinIO 集成测试、route parity、capability matrix、crypto gate、`git diff --check`。
- JDK17+：JDK17 单元测试、真实 MinIO 集成测试、route parity、crypto gate、JDK21/JDK25 compile、`git diff --check`。
- 双分支：secret scan、阶段文件同步检查。
