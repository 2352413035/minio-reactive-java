# 阶段 41：Admin IAM / IDP 只读 typed 深化

阶段 41 的目标是继续处理不会修改服务端状态的 IAM / IDP 只读接口，同时避免把 access key、secret、session token 等敏感内容带进普通模型。

## 1. 新增产品入口

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `listLdapPolicyEntities()` | `AdminPolicyEntities` | 读取 LDAP 策略绑定实体摘要，统计用户、用户组和策略映射数量。 |
| `listLdapAccessKeySummaries(userDN, listType)` | `AdminAccessKeySummaryList` | 读取指定 LDAP 用户 DN 的 access key 只读摘要。 |
| `listLdapAccessKeySummaries(listType)` | `AdminAccessKeySummaryList` | 批量读取 LDAP access key 只读摘要。 |
| `listOpenidAccessKeySummaries(listType)` | `AdminAccessKeySummaryList` | 批量读取 OpenID access key 只读摘要。 |

## 2. 敏感字段处理

新增的 `AdminAccessKeySummary` 和 `AdminAccessKeySummaryList` 故意不继承 `AdminJsonResult`，也不保存 raw JSON。

原因是 access key 列表响应在不同 MinIO / IDP 配置下可能包含敏感字段。模型只保留：

- `accessKey`
- `parentUser`
- `accountStatus`
- `name`
- `description`
- `expiration`
- `hasPolicy`

以下字段即使服务端返回，也不会进入模型：

- `secretKey`
- `sessionToken`
- 私钥
- provider token
- 其它未知凭证字段

## 3. 对能力矩阵的影响

阶段 41 后，Admin product-typed 从 60 / 128 提升到 63 / 128。`encrypted-blocked = 9`、`destructive-blocked = 29` 不变。

## 4. 仍然保留的边界

- 内置 access key / service account 的默认 madmin 加密响应仍走 `EncryptedAdminResponse`。
- LDAP/OpenID access key 摘要只是只读排障模型，不代表可以读取 secret。
- `importIam`、`addIdpConfig`、`updateIdpConfig`、`deleteIdpConfig` 等写入接口仍不是共享环境默认测试目标。
