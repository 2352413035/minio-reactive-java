# 阶段 73：加密响应安全诊断增强

## 1. 本阶段目标

阶段 73 继续做非破坏性成熟度提升，聚焦 `EncryptedAdminResponse` 的可解释性。用户遇到 MinIO madmin 默认加密响应时，最常见的问题不是“接口能不能调”，而是“不知道这个响应为什么不能当作 JSON 解析”。

本阶段不新增解密依赖，不改变 Crypto Gate Fail 结论，只给边界对象增加安全诊断方法，帮助调用方在日志、告警或调试页面中说明当前遇到的响应状态。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag。

## 2. 新增方法

| 方法 | 含义 | 是否包含敏感内容 |
| --- | --- | --- |
| `encryptedSize()` | 返回加密载荷字节数，只用于诊断。 | 否 |
| `decryptSupported()` | 当前 Java 端是否支持响应声明的算法。 | 否 |
| `requiresCryptoGate()` | 是否仍需要 Crypto Gate 放行后才能解密。 | 否 |
| `diagnosticMessage()` | 返回中文诊断说明。 | 否 |

## 3. 使用示例

```java
EncryptedAdminResponse response = admin.getConfigEncrypted().block();
if (response.requiresCryptoGate()) {
  log.warn(response.diagnosticMessage());
}
```

示例输出类似：

```text
madmin 加密算法 Argon2id + AES-GCM 当前需要 Crypto Gate 放行后才能解密
```

这条消息只描述算法和 Gate 状态，不包含 secret、token、配置值或响应明文。

## 4. 安全边界

1. `diagnosticMessage()` 不能替代解密，也不会返回明文配置。
2. `encryptedSize()` 只是密文字节数，不代表明文大小。
3. `decryptSupported()` 为 true 只表示算法能力存在，不代表所有业务响应都应该自动解析为明文模型。
4. Crypto Gate 未通过前，Argon2id 系列默认响应仍保持 `requiresCryptoGate() == true`。

## 5. 验证口径

阶段 73 至少需要验证：

- Argon2id + AES-GCM 响应会提示需要 Crypto Gate。
- PBKDF2 + AES-GCM 响应会显示当前 Java 端支持解密。
- 短载荷、空载荷或未知算法会得到安全中文诊断，而不是误报为已解密。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭据扫描继续通过。
