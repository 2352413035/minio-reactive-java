# 阶段 35：Admin IAM / Access Key 边界整理

阶段 35 的目标是把“明文只读 IAM 信息”和“加密 access key / service account 信息”继续分清楚，避免用户误以为所有身份接口都可以直接解析。

## 1. 新增明文只读入口

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `listBucketUsersInfo(bucket)` | `AdminUserList` | 列出指定 bucket 相关用户摘要，不返回 secret。 |
| `getTemporaryAccountInfo(accessKey)` | `AdminAccessKeyInfo` | 获取临时账号只读摘要，用于诊断临时凭证状态。 |

这两个入口都是只读查询，不会修改 MinIO 服务端状态。

## 2. 继续保持的加密边界

以下接口仍然属于 encrypted-blocked，不在 Crypto Gate Pass 前解析成明文模型：

- `getAccessKeyInfoEncrypted(...)`
- `listAccessKeysEncrypted(...)`
- `getServiceAccountInfoEncrypted(...)`
- `listServiceAccountsEncrypted()`
- `addServiceAccount(...)` 返回的加密响应

`getAccessKeyInfoTyped(...)` 和 `listAccessKeysTyped(...)` 继续抛出带中文解释的 `UnsupportedOperationException`，提醒调用方使用 encrypted 边界方法。

## 3. 对完成度的影响

阶段 35 后，Admin product-typed 口径从 53 / 128 提升到 55 / 128。encrypted-blocked 仍为 9，destructive-blocked 仍为 29。
