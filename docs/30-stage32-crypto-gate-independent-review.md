# 阶段 32：Crypto Gate 独立复审

阶段 32 的结论是：**继续保持 Crypto Gate Fail**。当前没有 owner、security reviewer、architect 三方批准，因此 SDK 仍然不能引入默认 madmin 加密响应解密依赖，也不能把默认加密响应伪装成明文 typed 结果。

## 1. 当前批准状态

| 角色 | 状态 | 说明 |
| --- | --- | --- |
| owner | 未批准 | 未收到明确的依赖和传递风险批准。 |
| security reviewer | 未批准 | 未完成安全公告、密码学 API、FIPS/Provider、失败回退审查。 |
| architect | 未批准 | 未完成 JDK8/JDK17+ 双分支行为一致性和长期维护边界审查。 |

批准状态同步写入 `scripts/madmin-fixtures/crypto-gate-status.properties`，并由 `scripts/madmin-fixtures/check-crypto-gate.sh` 校验。

## 2. 本阶段决策

1. 不修改 `pom.xml`。
2. 不新增 Bouncy Castle、argon2-jvm、Tink、libsodium、JNA 或同类 crypto/native 依赖。
3. 不新增未审查 crypto/native import。
4. 默认 madmin 加密响应继续返回 `EncryptedAdminResponse`。
5. PBKDF2 + AES-GCM 写入方向和 fixture 解密继续保留。
6. Argon2id + AES-GCM、Argon2id + ChaCha20-Poly1305 继续只做算法识别和诊断。

## 3. Gate Pass 前置条件

未来如果要把 Gate Fail 改为 Gate Pass，必须先完成：

1. 三方批准：owner、security reviewer、architect 均明确 APPROVE。
2. 依赖选择：明确依赖坐标、版本、许可证、维护状态和传递依赖。
3. 安全审查：覆盖安全公告、禁用算法、随机数来源、异常回退和密钥材料处理。
4. Provider 边界：说明是否注册 JVM 全局 provider，是否影响调用方已有 provider 顺序。
5. JDK8/JDK17+ 矩阵：JDK8、JDK17、JDK21、JDK25 均编译和测试通过。
6. fixture 证明：默认 Argon2id + AES-GCM 与 Argon2id + ChaCha20-Poly1305 fixture 都能解密。
7. 失败语义：解密失败时保留原始加密边界和中文诊断，不吞异常、不泄露 secret。

## 4. 本阶段加固

- 新增状态文件 `scripts/madmin-fixtures/crypto-gate-status.properties`。
- `check-crypto-gate.sh` 会先验证状态文件仍是 `fail`，且三方批准状态均为 `false`。
- 如果有人只改文档声称 Gate Pass，但没有同步状态文件和脚本门禁，检查会失败。

## 5. 验证口径

阶段 32 的验证重点不是“解密能力通过”，而是“边界没有被绕过”：

```bash
scripts/madmin-fixtures/check-crypto-gate.sh
mvn -q -DfailIfNoTests=true test
git diff --check
```

JDK17+ 分支还必须继续执行 JDK21/JDK25 compile。
