# Crypto 边界地图（阶段 111 后）

阶段 111 已对齐同目录 `minio-java` 的 adminapi crypto 方案，引入 `org.bouncycastle:bcprov-jdk18on:1.82`，Crypto Gate 当前状态为 **Pass**。

这表示 SDK 已能解密 MinIO madmin 默认加密响应，但不表示 SDK 会自动保存或猜测用户凭证。所有解密仍必须由调用方显式传入对应账号的 `secretKey`。

## 当前能力

| 能力 | 当前状态 | 说明 |
| --- | --- | --- |
| 识别 madmin 加密响应 | 支持 | 通过 32 字节 salt 后的算法 ID 判断。 |
| 解密 Argon2id + AES-GCM | 支持 | 对齐 minio-java 与 madmin-go 默认 AES 路径。 |
| 解密 Argon2id + ChaCha20-Poly1305 | 支持 | 使用 Bouncy Castle，覆盖无 Native AES 场景。 |
| 解密 PBKDF2 + AES-GCM | 支持 | 兼容 FIPS/历史夹具与 SDK 写入路径。 |
| 明文自动解析 | 谨慎推进 | `EncryptedAdminResponse` 先提供显式 `decrypt(...)` / `decryptAsUtf8(...)`；部分明文模型提供 secretKey overload。 |

## 原 encrypted-blocked 路由

以下路由仍返回 `EncryptedAdminResponse` 边界对象，但因为用户可显式解密，能力矩阵中的 `encrypted-blocked` 已降为 `0`。

| route | 当前入口 | 说明 |
| --- | --- | --- |
| `ADMIN_GET_CONFIG` | `getConfigEncrypted()` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_GET_CONFIG_KV` | `getConfigKvEncrypted(...)` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_LIST_CONFIG_HISTORY_KV` | `listConfigHistoryKvEncrypted(...)` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_LIST_USERS` | `listUsersEncrypted()` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_ADD_SERVICE_ACCOUNT` | `addServiceAccount(...)` / `createServiceAccount(...)` | 可返回加密凭证；调用方显式解密。 |
| `ADMIN_INFO_SERVICE_ACCOUNT` | `getServiceAccountInfoEncrypted(...)` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_LIST_SERVICE_ACCOUNTS` | `listServiceAccountsEncrypted()` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_INFO_ACCESS_KEY` | `getAccessKeyInfoEncrypted(...)` / `getAccessKeyInfoTyped(..., secretKey)` | secretKey overload 可解析明文模型。 |
| `ADMIN_LIST_ACCESS_KEYS_BULK` | `listAccessKeysEncrypted(...)` / `listAccessKeysTyped(..., secretKey)` | secretKey overload 可解析明文列表模型。 |
| `ADMIN_LIST_IDP_CONFIG` | `listIdpConfigsEncrypted(...)` | 服务端用 madmin 加密响应；调用方显式解密。 |
| `ADMIN_GET_IDP_CONFIG` | `getIdpConfigEncrypted(...)` | 服务端用 madmin 加密响应；调用方显式解密。 |

## 安全规则

1. 不把 secretKey 写入文档、日志、测试报告或 git 提交。
2. 解密失败必须抛出明确中文错误，不能伪装成空 JSON 或空模型。
3. 日志只能记录算法名、加密字节数、是否可解密等安全诊断字段。
4. 若后续 madmin-go 新增算法，先让 `requiresCryptoGate()` 重新暴露为 true，再补依赖审查和 fixture。
