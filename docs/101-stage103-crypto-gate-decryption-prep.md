# 阶段 103：Crypto Gate 自动解密准备与最小可用路径

## 涉及 Crypto Gate 的能力

当前能力矩阵中的 `encrypted-blocked = 11` 主要对应 madmin 默认加密响应，例如：

- Admin config 读取：`getConfigEncrypted()`、`getConfigKvEncrypted(...)`、`listConfigHistoryKvEncrypted(...)`
- 用户与 access key 读取：`listUsersEncrypted()`、`getAccessKeyInfoEncrypted(...)`、`listAccessKeysEncrypted(...)`
- service account 读取/创建结果：`getServiceAccountInfoEncrypted(...)`、`listServiceAccountsEncrypted()`、`addServiceAccount(...)`
- IDP 配置读取：`listIdpConfigsEncrypted(...)`、`getIdpConfigEncrypted(...)`

这些接口不是“不能调用”，而是默认响应可能包含敏感配置、secret、token 或账号材料，MinIO/madmin 会用调用方账号的 secret key 派生密钥加密返回内容。

## MinIO / madmin 的处理方式

本地 `madmin-go` 参考实现 `encrypt.go` 的公开格式为：

```text
salt(32) | algorithmId(1) | nonce(8) | encrypted stream
```

解密时需要：

1. 完整的加密载荷。
2. 对应账号的 `secretKey`。
3. Java 运行时支持该载荷头部声明的算法。

madmin 当前可能使用：

| 算法 ID | 算法 | 当前 Java SDK 状态 |
| --- | --- | --- |
| `0x00` | Argon2id + AES-GCM | 可识别，不自动解密 |
| `0x01` | Argon2id + ChaCha20-Poly1305 | 可识别，不自动解密 |
| `0x02` | PBKDF2 + AES-GCM | 可识别，可解密 |

因此，SDK **不是一定可以解密**。即使用户合法持有响应，也必须提供匹配的 `secretKey`；如果服务端返回默认 Argon2id 系列载荷，而项目尚未引入并批准 Argon2/ChaCha 依赖，SDK 会继续返回 `EncryptedAdminResponse` 边界并给出中文诊断。

## 本阶段新增能力

`EncryptedAdminResponse` 新增显式解密方法：

```java
EncryptedAdminResponse response = admin.getConfigEncrypted().block();
if (response.decryptSupported()) {
  String plaintext = response.decryptAsUtf8(secretKey);
}
```

新增方法：

- `requiresSecretKey()`：说明该响应是否需要调用方显式提供 secret key。
- `decrypt(String secretKey)`：返回解密后的明文字节，仅支持已放行算法。
- `decryptAsUtf8(String secretKey)`：按 UTF-8 返回明文字符串。

## 失败语义

- 未提供 secret key：抛出中文参数错误。
- 载荷不是已识别 madmin 加密格式：抛出中文格式错误。
- 默认 Argon2id / ChaCha20-Poly1305：抛出中文算法不支持错误，`requiresCryptoGate()` 仍为 `true`。
- 解密认证失败：抛出中文解密失败错误，调用方必须保留原始 `EncryptedAdminResponse`，不能伪装为空对象或空 JSON。

## 当前 Gate 结论

阶段 103 只放行了“不新增依赖即可支持的 PBKDF2 + AES-GCM 解密路径”。MinIO 默认 Argon2id 系列仍未放行，因此：

- `encrypted-blocked` 仍保持 `11`。
- Crypto Gate 仍未整体 Pass。
- 后续若要完全解锁默认响应，需要单独完成 Argon2id / ChaCha20-Poly1305 依赖评估、安全审查、许可证审查、双分支多 JDK 测试和 Go madmin 互操作矩阵。
