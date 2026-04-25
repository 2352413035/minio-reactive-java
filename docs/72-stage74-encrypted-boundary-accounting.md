# 阶段 74：IDP 加密边界统计口径修正

## 1. 本阶段目标

阶段 72 已经确认 MinIO 服务端在 `idp-config` 列表和单项读取中会调用 `madmin.EncryptData(...)`，madmin-go 客户端读取后也会调用 `DecryptData(...)`。这说明 `ADMIN_LIST_IDP_CONFIG` 与 `ADMIN_GET_IDP_CONFIG` 应该被纳入 Crypto Gate 统计，而不是继续按旧口径留在 `encrypted-blocked = 9` 之外。

阶段 74 修正能力矩阵和当前用户文档：当前 Admin 加密响应边界从 9 个调整为 11 个。这个变化不是退化，而是把真实服务端协议边界纳入统计，避免把 IDP 配置加密响应误读成普通明文 JSON。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag，也不放行 Crypto Gate。

## 2. 新统计口径

当前 `encrypted-blocked = 11`，包括：

1. `ADMIN_LIST_USERS`
2. `ADMIN_ADD_SERVICE_ACCOUNT`
3. `ADMIN_INFO_SERVICE_ACCOUNT`
4. `ADMIN_LIST_SERVICE_ACCOUNTS`
5. `ADMIN_GET_CONFIG`
6. `ADMIN_GET_CONFIG_KV`
7. `ADMIN_LIST_CONFIG_HISTORY_KV`
8. `ADMIN_LIST_IDP_CONFIG`
9. `ADMIN_GET_IDP_CONFIG`
10. `ADMIN_INFO_ACCESS_KEY`
11. `ADMIN_LIST_ACCESS_KEYS_BULK`

其中 IDP 两个读取接口已经有 `listIdpConfigsEncrypted(type)` 和 `getIdpConfigEncrypted(type, name)` 作为安全边界入口。旧的 `listIdpConfigs(type)`、`getIdpConfigInfo(type, name)` 继续作为旧版本或测试环境明文兼容入口保留。

## 3. 为什么不是减少或放行

- `EncryptedAdminResponse` 是产品边界，不是明文解析完成。
- `encrypted-blocked` 统计的是“真实服务端默认响应仍受 Crypto Gate 限制”的路由数量。
- 把 9 修正为 11 是更诚实的风险统计，不表示 SDK 覆盖倒退。
- 只有 Argon2id/ChaCha20 依赖、ADR、许可证/安全审查、双分支测试和失败回退都满足后，才能减少该计数。

## 4. 验证口径

阶段 74 至少需要验证：

- `report-capability-matrix.py` 输出 Admin `encrypted-blocked = 11`。
- route parity 仍为 `233 / 233`，缺失 0，额外 0。
- product-typed 与 raw-fallback 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭据扫描继续通过。
