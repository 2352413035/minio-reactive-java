# 阶段 72：IDP 配置读取加密边界补充

## 1. 本阶段目标

阶段 72 继续做非破坏性成熟度提升，聚焦 IDP 配置读取接口的真实协议边界。MinIO 服务端在 `idp-config` 列表和单项读取中会按 madmin 协议加密返回内容，里面可能包含敏感配置值，因此 SDK 不能把真实响应伪装成已经解密的明文模型。

本阶段新增显式加密边界方法，让调用方可以安全拿到 `EncryptedAdminResponse`，并通过 `isEncrypted()`、`algorithmName()` 等方法判断是否属于 madmin 加密响应。旧的 `listIdpConfigs(type)` 与 `getIdpConfigInfo(type, name)` 继续保留，主要用于旧版本兼容或测试环境中的明文模拟响应。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag，也不尝试绕过 Crypto Gate。

## 2. 新增方法

| 新方法 | 返回类型 | 说明 |
| --- | --- | --- |
| `listIdpConfigsEncrypted(type)` | `EncryptedAdminResponse` | 列出指定 IDP 类型配置的加密响应。 |
| `getIdpConfigEncrypted(type, name)` | `EncryptedAdminResponse` | 获取单个 IDP 配置的加密响应。 |

## 3. 使用示例

```java
EncryptedAdminResponse encrypted = admin.getIdpConfigEncrypted("openid", "primary").block();
if (encrypted.isEncrypted()) {
  String algorithm = encrypted.algorithmName();
  // 默认响应解密 Gate 未放行前，不要把 encryptedData 当作明文 JSON 使用。
}
```

如果调用方处在旧版本或测试环境，并确认服务端返回明文 JSON，可以继续使用兼容方法：

```java
AdminJsonResult info = admin.getIdpConfigInfo("openid", "primary").block();
```

## 4. 安全边界

1. 新方法只读取 IDP 配置，不新增或修改配置。
2. SDK 不解析、不打印、不落盘 IDP 配置明文值。
3. 默认 Crypto Gate 未通过前，`EncryptedAdminResponse` 只作为边界对象使用。
4. 如果后续批准 Argon2id/ChaCha20 解密依赖，需要先更新 ADR、测试矩阵和失败回退策略，再新增明文摘要模型。

## 5. 验证口径

阶段 72 至少需要验证：

- 新方法命中原 `idp-config/{type}` 与 `idp-config/{type}/{name}` 路由。
- `type`、`name` 空值会得到中文本地校验错误。
- 旧兼容方法继续可用。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭据扫描继续通过。
