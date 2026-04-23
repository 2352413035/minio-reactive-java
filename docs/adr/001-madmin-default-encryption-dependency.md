# ADR-001 madmin 默认加密响应依赖评估

## 状态

提议中，暂不引入依赖。

## 背景

MinIO 管理端部分响应会使用 madmin-go `EncryptData` 格式返回。当前 Java SDK 已支持 PBKDF2 + AES-GCM 写入和解密，但 madmin-go 默认路径可能返回 Argon2id + AES-GCM 或 Argon2id + ChaCha20-Poly1305。JDK8 标准库缺少 Argon2id 和 ChaCha20-Poly1305，完整解密默认响应需要额外 crypto 能力。

## 决策

本阶段不修改 `pom.xml`，不新增第三方依赖。先保留 `EncryptedAdminResponse`，并通过算法识别和 fixture 测试把当前边界固定下来。

## 候选方案

### 方案 A：Bouncy Castle `bcprov-jdk18on` 或 LTS8 系列

- 能力：官方文档显示有 `Argon2BytesGenerator`；Bouncy Castle 也提供低层 crypto/provider 能力，可作为 Argon2id 和 ChaCha20-Poly1305 的候选。
- JDK：`bcprov-jdk18on` 面向 Java 1.8 及以上；LTS8 系列面向长期 Java 8 场景。
- 许可证：Bouncy Castle License，通常比 LGPL 更容易被 SDK 消费。
- 风险：引入通用 crypto provider，依赖面较大；需要确认具体版本的 ChaCha20-Poly1305 API、FIPS 要求和安全公告。

### 方案 B：`de.mkammerer:argon2-jvm` + 另一 ChaCha20-Poly1305 实现

- 能力：专注 Argon2 JVM binding。
- 许可证：公开仓库和 Maven 页面显示 LGPL 3.0。
- 风险：通常涉及 native/JNA 绑定；还需要另选 ChaCha20-Poly1305；许可证和部署复杂度不适合作为当前首选。

### 方案 C：自行实现 Argon2id / ChaCha20-Poly1305

- 能力：理论上无需新增依赖。
- 风险：密码学实现风险高，维护成本高，测试难度大。除非有强制要求，不采纳。

## 当前取舍

先不引入依赖，继续支持 PBKDF2 + AES-GCM 写入方向，并把默认响应保留为 `EncryptedAdminResponse`。这样不会扩大 JDK8 风险，也不会在没有安全审查时引入 crypto provider。

## 密钥与 fixture 边界

测试 fixture 只使用 `fixture-secret` 这类不可用假值，不保存真实 MinIO access key、secret key 或 token。当前分支历史中曾出现过用户提供的测试凭证字面量；本轮不改写历史，但后续发布或共享仓库前应轮换这些测试凭证，并继续保持源码、文档、fixture 中只出现占位符或环境变量。

## 后续批准门

只有执行负责人、`security-reviewer`、`architect` 都明确 APPROVE 后，才允许修改两条分支 `pom.xml`。批准前必须提供：

1. 候选依赖版本、许可证和安全公告检查。
2. JDK8/JDK17+ 测试矩阵。
3. madmin-go 默认响应 fixture 解密测试。
4. 失败回退策略。
5. 双分支 public API 和异常语义一致性说明。

## 参考来源

- Maven Central `org.bouncycastle:bcprov-jdk18on` 版本目录：https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/
- Bouncy Castle Java 文档与 LTS8 包列表：https://www.bouncycastle.org/documentation/documentation-java/
- Bouncy Castle `Argon2BytesGenerator` API 文档：https://downloads.bouncycastle.org/java/docs/bcprov-jdk14-javadoc/org/bouncycastle/crypto/generators/Argon2BytesGenerator.html
- Maven Repository `de.mkammerer:argon2-jvm` 页面：https://mvnrepository.com/artifact/de.mkammerer/argon2-jvm
