# 23 阶段 25 Crypto Gate 决策复核

## 1. 结论

阶段 25 继续保持 **Crypto Gate Fail**：本阶段不修改 `pom.xml`，不新增第三方 crypto 依赖，不把 madmin-go 默认加密响应伪装成明文 typed 结果。

这不是放弃接口，而是把接口真实边界暴露给用户：

1. PBKDF2 + AES-GCM 写入方向和 fixture 解密继续支持。
2. Argon2id + AES-GCM、Argon2id + ChaCha20-Poly1305 可以识别并诊断。
3. 默认加密响应继续返回 `EncryptedAdminResponse`，调用方可以读取 `algorithm()` / `algorithmName()` 判断后续处理方式。
4. SDK 不会在没有安全、许可证、JDK8/JDK17+ 兼容审查前引入不可逆的 crypto 依赖。

## 2. 2026-04-24 资料复核

本轮复核使用公开官方资料和 Maven Central 页面确认候选依赖状态：

| 候选 | 当前事实 | 阶段 25 取舍 |
| --- | --- | --- |
| Bouncy Castle `bcprov-jdk18on` / LTS8 | 官方 Java 文档提供 JDK 1.8+ 与 LTS8 文档入口；Maven Central 显示 `bcprov-jdk18on` 已有 1.84；官方许可证页说明 Bouncy Castle APIs 按 MIT 许可证发布；Javadoc 中存在 `Argon2BytesGenerator`，也能找到 `ChaCha20Poly1305` 低层 API。 | 作为未来首选候选继续保留，但不能直接引入。它是通用 crypto provider，必须先确认具体 API、provider 注册方式、FIPS 要求、安全公告、包体影响和双分支测试矩阵。 |
| `de.mkammerer:argon2-jvm` | 官方仓库说明它是通过 JNA 调用 native C 库的 JVM binding，许可证为 LGPL v3；Maven Central 显示 2.12 于 2025-03-04 发布。 | 不作为当前首选。它只解决 Argon2，还要另配 ChaCha20-Poly1305；native/JNA、平台覆盖和 LGPL 边界都不适合在本阶段引入。 |
| 自研 Argon2id / ChaCha20-Poly1305 | 不新增依赖。 | 拒绝。密码学实现风险过高，SDK 不应自行维护未审计实现。 |

## 3. 为什么仍然不能 Gate Pass

要把 `EncryptedAdminResponse` 升级成默认明文 typed 模型，必须同时解决以下问题：

1. 依赖批准：owner、security reviewer、architect 三方明确 APPROVE。
2. 许可证批准：确认候选依赖许可证适合 SDK 被业务项目直接传递使用。
3. JDK8 兼容：JDK8 分支必须可以编译、测试和运行，不引入只适合 JDK17+ 的 API。
4. JDK17+ 兼容：JDK17、JDK21、JDK25 分支必须保持一致行为。
5. FIPS/Provider 边界：如果引入 provider，必须说明是否会改变调用方 JVM 的全局 provider 行为。
6. fixture 证明：madmin-go 默认 Argon2id + AES-GCM 与 Argon2id + ChaCha20-Poly1305 fixture 都要能解密。
7. 失败回退：解密失败时仍返回可诊断错误，不能吞掉原始加密响应，也不能泄露 secret。

阶段 25 没有取得上述批准，因此只能复核并加固 Gate Fail。

## 4. 本阶段加固内容

1. `scripts/madmin-fixtures/check-crypto-gate.sh` 不再只检查 `pom.xml`，还会扫描源码 import，防止绕过 Maven 依赖检查直接引入 Bouncy Castle、argon2-jvm、Tink、JNA、libsodium 等 crypto/native 包。
2. `MadminEncryptionSupportTest` 增加算法策略测试，明确 Gate Pass 前只有 PBKDF2 + AES-GCM 标记为可解密。
3. `docs/12-madmin-encryption-compat.md`、`docs/16-crypto-boundary-map.md`、`docs/17-release-readiness-report.md`、`docs/release-gates.md` 与 ADR 统一阶段 25 结论。

## 5. 对 SDK 完整性的影响

阶段 25 后，SDK 的完整性口径是：

- route parity 与 callability 不受影响：所有 MinIO 公开路由仍在 catalog / typed / advanced / raw 体系内有入口。
- 用户体验更清晰：encrypted-blocked 接口明确返回边界对象，而不是抛出含糊异常或错误解析。
- 未来可扩展：一旦 Crypto Gate Pass，可以在不破坏现有调用方的前提下，把 `EncryptedAdminResponse` 增强为可解密 typed 模型。

因此，Crypto Gate Fail 不是“功能偷偷缺失”，而是当前发布阶段必须保留的安全边界。

## 6. 阶段 25 验证命令

```bash
scripts/madmin-fixtures/check-crypto-gate.sh
mvn -q -Dtest=MadminEncryptionSupportTest test
mvn -q -DfailIfNoTests=true test
git diff --check
```

JDK17+ 分支同步后，还要补跑 JDK17 单元测试、JDK21/JDK25 compile 与 live smoke。

## 7. 资料来源

- Bouncy Castle Java 文档：https://www.bouncycastle.org/documentation/documentation-java/
- Bouncy Castle 许可证页：https://www.bouncycastle.org/about/license/
- Bouncy Castle `Argon2BytesGenerator` Javadoc：https://downloads.bouncycastle.org/java/docs/bcprov-jdk15to18-javadoc/org/bouncycastle/crypto/generators/Argon2BytesGenerator.html
- Maven Central `org.bouncycastle:bcprov-jdk18on`：https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/
- `argon2-jvm` 官方仓库：https://github.com/phxql/argon2-jvm
- Maven Central `de.mkammerer:argon2-jvm:2.12`：https://repo.maven.apache.org/maven2/de/mkammerer/argon2-jvm/2.12/
