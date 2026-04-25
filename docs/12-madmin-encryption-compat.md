# madmin 加密兼容性

阶段 111 后，本项目已经对齐同目录 `minio-java` 的 adminapi crypto 方案，支持 MinIO madmin 默认加密响应的显式解密。

## 支持的算法

| 算法 ID | 算法 | 当前状态 |
| --- | --- | --- |
| `0x00` | Argon2id + AES-GCM | 支持识别和解密 |
| `0x01` | Argon2id + ChaCha20-Poly1305 | 支持识别和解密 |
| `0x02` | PBKDF2 + AES-GCM | 支持识别和解密 |

## 为什么引入 Bouncy Castle

`minio-java` 当前使用 `org.bouncycastle:bcprov-jdk18on:1.82` 提供 Argon2id 与 ChaCha20-Poly1305 能力。本项目为了保持 JDK8 与 JDK17+ 双分支一致，采用同一个依赖版本，而不是自行实现密码学算法，也不引入 native/JNA 绑定。

## 使用方式

```java
EncryptedAdminResponse response = admin.getConfigEncrypted().block();
String plaintext = response.decryptAsUtf8(secretKey);
```

调用方必须显式传入对应账号的 `secretKey`。SDK 不会保存、猜测或输出该凭证。

## fixture 互操作

`scripts/madmin-fixtures/generate-fixtures.sh` 使用 Go 与固定版本 `madmin-go v3.0.109` 生成测试夹具：

- `pbkdf2-aesgcm-go.base64`
- `argon2id-aesgcm-go-default.base64` 或当前硬件默认算法对应文件
- `argon2id-chacha20-go-forced.base64`

`scripts/madmin-fixtures/verify-fixtures.sh` 会验证 committed fixture 与当前 Go 工具链新生成 fixture 都能被 Java 解密。

## 失败回退

如果 secretKey 错误、响应损坏或未来出现未知算法，SDK 会抛出中文异常，并保留 `EncryptedAdminResponse` 的算法名、字节数等诊断信息。调用方不能把失败结果伪装成空明文。
