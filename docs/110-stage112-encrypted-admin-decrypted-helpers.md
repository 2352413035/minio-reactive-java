# 阶段 112：加密 Admin 响应显式解密便捷入口

## 背景

阶段 111 已让 `EncryptedAdminResponse` 支持 MinIO 默认 madmin 加密算法。为了让用户不必在每个业务点重复写 `decryptAsUtf8(secretKey)` 与解析逻辑，本阶段补充一组显式 `secretKey` 入口。

## 新增入口

| 能力 | 入口 | 返回 |
| --- | --- | --- |
| 用户列表 | `listUsersDecrypted(secretKey)` | 明文 JSON 字符串 |
| 配置 KV | `getConfigKvDecrypted(key, secretKey)` | 明文文本 |
| 配置历史 | `listConfigHistoryKvDecrypted(count, secretKey)` | 明文文本 |
| 完整配置 | `getConfigDecrypted(secretKey)` | 明文文本 |
| IDP 配置列表 | `listIdpConfigsDecrypted(type, secretKey)` | 明文 JSON 字符串 |
| 单个 IDP 配置 | `getIdpConfigDecrypted(type, name, secretKey)` | 明文 JSON 字符串 |
| 服务账号信息 | `getServiceAccountInfoDecrypted(accessKey, secretKey)` | 明文 JSON 字符串 |
| 当前用户服务账号 | `listServiceAccountsDecrypted(secretKey)` | 明文 JSON 字符串 |
| 指定用户服务账号 | `listServiceAccountDecrypted(username, secretKey)` | 明文 JSON 字符串 |
| access key 信息 | `getAccessKeyInfoTyped(accessKey, secretKey)` | `AdminAccessKeyInfo` |
| access key 列表 | `listAccessKeysTyped(listType, secretKey)` | `AdminAccessKeyList` |
| 创建服务账号 | `createServiceAccount(request, secretKey)` / `addServiceAccount(request, secretKey)` | `ServiceAccountCreateResult` |

## 安全边界

- 所有入口都要求调用方显式传入 `secretKey`。
- SDK 不保存、不缓存、不打印 `secretKey`。
- 原有 `*Encrypted` 方法继续保留，用于排障、延迟解密或用户自行解析。
- 解密失败仍抛出中文异常，不能伪装成空模型。

## 验证

新增 mock 测试覆盖配置、access key 信息、access key 列表和创建服务账号四类路径，证明客户端能从 madmin 加密响应解密到明文字符串或明文业务模型。
