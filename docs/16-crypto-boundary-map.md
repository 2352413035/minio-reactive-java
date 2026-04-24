# 16 Crypto Boundary Map

阶段 18 的结论仍然是 **Crypto Gate Fail（暂不引入新依赖）**。这不是功能遗漏，而是安全边界：在没有 owner、security reviewer、architect 三方批准前，SDK 不能引入 Argon2id / ChaCha20-Poly1305 依赖，也不能把 madmin 默认加密响应伪装成明文 typed 结果。

## 当前可支持

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 生成 PBKDF2 + AES-GCM 请求载荷 | 支持 | 用于 `setConfigKvText(...)`、`setConfigText(...)`、`addUser(...)` 等写入请求。 |
| 解密 PBKDF2 + AES-GCM fixture | 支持 | `MadminEncryptionSupportTest` 和 Go fixture 已证明。 |
| 识别 Argon2id + AES-GCM | 支持识别，不支持解密 | 返回 `EncryptedAdminResponse.algorithmName()` 供诊断。 |
| 识别 Argon2id + ChaCha20-Poly1305 | 支持识别，不支持解密 | 返回 `EncryptedAdminResponse.algorithmName()` 供诊断。 |

## 当前 encrypted-blocked 路由

| 路由 | SDK 边界入口 | 原因 |
| --- | --- | --- |
| `ADMIN_GET_CONFIG` | `getConfigEncrypted()` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_GET_CONFIG_KV` | `getConfigKvEncrypted(...)` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_LIST_CONFIG_HISTORY_KV` | `listConfigHistoryKvEncrypted(...)` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_LIST_USERS` | `listUsersEncrypted()` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_ADD_SERVICE_ACCOUNT` | `createServiceAccount(...)` / `addServiceAccount(...)` | 创建结果可能包含加密凭证。 |
| `ADMIN_INFO_SERVICE_ACCOUNT` | `getServiceAccountInfoEncrypted(...)` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_LIST_SERVICE_ACCOUNTS` | `listServiceAccountsEncrypted()` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_INFO_ACCESS_KEY` | `getAccessKeyInfoEncrypted(...)` | 服务端用 madmin 默认加密响应。 |
| `ADMIN_LIST_ACCESS_KEYS_BULK` | `listAccessKeysEncrypted(...)` | 服务端用 madmin 默认加密响应。 |

## 阶段 18 复核命令

```bash
scripts/madmin-fixtures/check-crypto-gate.sh
```

该命令会：

1. 验证仓库 committed fixture。
2. 用系统 Go 重新生成 madmin fixture 并跑 Java 诊断测试。
3. 检查 `pom.xml` 未出现未批准 crypto 依赖候选。

通过这个命令并不代表 Gate Pass；它代表当前 Gate Fail 边界仍然被正确执行。
