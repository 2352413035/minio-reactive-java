# 阶段 111：Crypto Gate Pass 与默认 madmin 解密放行

## 背景

重新确认主对标项目为同目录 `minio-java` 后，发现官方 Java SDK 的 `adminapi` 已使用 `org.bouncycastle:bcprov-jdk18on:1.82` 实现 madmin 默认加密响应的解密能力。此前本项目只支持 PBKDF2 + AES-GCM，并把 Argon2id 系列响应保留为 `EncryptedAdminResponse` 边界；这会让合法用户即使持有账号 `secretKey`，也无法像 minio-java 一样解密配置、服务账号、access key 等管理端响应。

阶段 111 按 minio-java 的实现方向放行 Crypto Gate：加入 Bouncy Castle 依赖，支持 Argon2id + AES-GCM、Argon2id + ChaCha20-Poly1305 与 PBKDF2 + AES-GCM 三类 madmin 算法。

## 当前能力

| 算法 ID | madmin 算法 | 当前 Java 端能力 |
| --- | --- | --- |
| `0x00` | Argon2id + AES-GCM | 可识别、可解密 |
| `0x01` | Argon2id + ChaCha20-Poly1305 | 可识别、可解密 |
| `0x02` | PBKDF2 + AES-GCM | 可识别、可解密 |

`EncryptedAdminResponse` 仍然是公开边界对象。它现在表示“这是 madmin 加密响应，需要调用方显式提供对应账号的 `secretKey`”，而不是“SDK 完全不能解密”。

## 用户使用方式

```java
EncryptedAdminResponse response = admin.getConfigEncrypted().block();
String plaintext = response.decryptAsUtf8(adminSecretKey);
```

注意：

1. SDK 不会保存、猜测或自动读取用户的 `secretKey`。
2. 解密失败时会抛出中文异常，调用方应保留原始 `EncryptedAdminResponse` 用于排障，不能把失败结果伪装成空 JSON。
3. 加密响应里可能包含 secret、token、策略或配置内容，日志只允许记录算法名、字节数和是否可解密，不允许输出明文。

## 依赖与对标依据

- 对标实现：`minio-java/adminapi/src/main/java/io/minio/admin/Crypto.java`。
- 运行时依赖：`org.bouncycastle:bcprov-jdk18on:1.82`。
- 选择理由：与 minio-java 当前依赖一致，覆盖 JDK8 及更高版本，不引入 native/JNA 绑定。
- 继续拒绝：SDK 自研 Argon2id 或 ChaCha20-Poly1305；这类密码学基础实现不应由本项目自行维护。

## 验证证据

本阶段要求以下证据同时通过：

- committed Go fixture：PBKDF2 + AES-GCM、Argon2id + AES-GCM、强制生成的 Argon2id + ChaCha20-Poly1305。
- 当前 Go 工具链新生成 fixture：默认算法与强制 ChaCha20 fixture。
- `MadminEncryptionSupportTest`：三类算法识别、解密、诊断和 round-trip。
- `scripts/madmin-fixtures/check-crypto-gate.sh`：状态文件、依赖版本、源码 import 与 fixture 矩阵。
- 双分支多 JDK：JDK8/JDK17 运行测试，JDK21/JDK25 编译验证。

## 与 capability matrix 的关系

阶段 111 后，`encrypted-blocked` 应降为 `0`。这不代表所有加密 Admin API 都自动返回业务明文模型，而是代表 SDK 已具备合法用户显式解密默认 MinIO 响应的能力。

破坏性操作仍由 `destructive-blocked` 管理，阶段 111 不降低该计数。
