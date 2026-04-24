# 阶段 54：Admin 策略与复制轻量边界

## 1. 本阶段目标

阶段 54 继续处理非 Crypto Gate 的 Admin 缺口，把 replication MRF、tier verify、内置策略绑定/解绑、LDAP 策略绑定/解绑从 advanced 字符串入口提升为更容易理解的产品边界。

## 2. 新增产品方法

| 方法 | 对应路由 | 返回 | 边界说明 |
| --- | --- | --- | --- |
| `getReplicationMrfInfo(...)` | `ADMIN_REPLICATION_MRF` | `AdminJsonResult` | 只读查询；字段随 MinIO 版本变化，保留通用 JSON。 |
| `verifyTierInfo(...)` | `ADMIN_VERIFY_TIER` | `AdminTextResult` | 可能访问外部 tier，调用方应控制超时和维护窗口。 |
| `attachBuiltinPolicy(...)` | `ADMIN_ATTACH_DETACH_BUILTIN_POLICY` + `attach` | `AdminTextResult` | 权限变更操作，要求非空请求体。 |
| `detachBuiltinPolicy(...)` | `ADMIN_ATTACH_DETACH_BUILTIN_POLICY` + `detach` | `AdminTextResult` | 权限变更操作，要求非空请求体。 |
| `attachLdapPolicy(...)` | `ADMIN_LDAP_ATTACH_DETACH_POLICY` + `attach` | `AdminTextResult` | 身份源策略变更，要求非空请求体。 |
| `detachLdapPolicy(...)` | `ADMIN_LDAP_ATTACH_DETACH_POLICY` + `detach` | `AdminTextResult` | 身份源策略变更，要求非空请求体。 |

## 3. 安全边界

- 策略 attach/detach 方法只发送调用方提供的请求体，不解析、不保存、不记录其中的用户、组、策略或身份源信息。
- SDK 固定 `attach` / `detach` operation，减少调用方拼错 operation 字符串的风险。
- tier verify 可能触达外部对象存储或远端 tier，只包装响应文本，不在共享 live 中执行真实外部连通性验证。
- raw client 仍可通过 catalog 调用同一 endpoint，用于新 MinIO 版本的临时兜底。

## 4. 当前能力矩阵变化

- Admin product-typed：88 / 128 -> 94 / 128。
- `raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 保持不变。
- `destructive-blocked = 29` 保持不变。

## 5. 后续建议

阶段 55 已继续处理配置历史和配置删除高风险接口。后续如果触及服务控制、token 吊销等更高风险接口，即使增加产品方法，也必须保留高风险命名、中文文档和独立 lab 门禁。
