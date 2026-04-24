# 阶段 45：Crypto Gate Pass 准备清单

## 1. 本阶段结论

阶段 45 只做 **Crypto Gate Pass 准备**，不直接放行：

1. `CRYPTO_GATE_STATUS` 继续保持 `fail`。
2. 不修改 `pom.xml`，不新增 crypto、native、JNA 或 provider 依赖。
3. 不把 madmin 默认加密响应解析成明文 typed 模型。
4. `EncryptedAdminResponse` 继续作为默认响应的安全边界。
5. 本阶段新增的是候选方案、审查清单、测试矩阵和回退语义，方便后续正式 Gate Pass 时逐项验收。

换句话说，阶段 45 的目标不是“现在解密默认响应”，而是让后续真正要解密时不会靠临时判断或口头批准推进。

## 2. 当前 madmin 加密事实

对照本机 `madmin-go v3.0.109` 源码，`EncryptData` 仍围绕三类算法 ID：

| 算法 ID | madmin 算法 | 当前 Java 边界 |
| --- | --- | --- |
| `0x00` | Argon2id + AES-GCM | 可识别，不默认解密。 |
| `0x01` | Argon2id + ChaCha20-Poly1305 | 可识别，不默认解密。 |
| `0x02` | PBKDF2 + AES-GCM | 写入方向和 fixture 解密已支持。 |

因此，当前缺口不是“HTTP 路由缺失”，而是“默认加密响应的密码学能力尚未批准”。路由目录、专用客户端、advanced 兼容入口和 raw 兜底仍然可以覆盖调用入口；真正被拦住的是默认加密响应的明文 typed 解析。

## 3. 候选方案矩阵

| 候选 | 可解决的问题 | 主要风险 | 阶段 45 结论 |
| --- | --- | --- | --- |
| Bouncy Castle JDK8 可用系列 | 可能同时提供 Argon2id 与 ChaCha20-Poly1305 所需能力。 | 通用 crypto/provider 依赖面较大；需要确认具体版本 API、许可证、安全公告、FIPS/provider 行为和包体影响。 | 保留为后续首选候选，但必须在实现前重新核对当前版本与安全公告。 |
| `argon2-jvm` + 其它 AEAD 实现 | 可补 Argon2id。 | native/JNA、平台覆盖、许可证和额外 AEAD 依赖都会扩大风险；单独不能解决 ChaCha20-Poly1305。 | 不作为默认首选。 |
| 仅使用 JDK 标准库 | 依赖最少。 | JDK8 无 Argon2id，也无法统一覆盖 ChaCha20-Poly1305；JDK17+ 与 JDK8 会产生能力分裂。 | 不能作为完整 Gate Pass 方案。 |
| SDK 自行实现 Argon2id / ChaCha20-Poly1305 | 不新增外部依赖。 | 密码学实现、审计和长期维护风险过高。 | 继续拒绝。 |

任何候选方案都必须以实施时的官方文档、Maven 坐标、许可证和安全公告为准。本文件不把阶段 25 的版本事实继续当作“永久最新事实”。

## 4. Gate Pass 前必须补齐的批准材料

真正从 Gate Fail 改为 Gate Pass 前，必须把以下材料写入 ADR 或同级决策记录：

1. **owner 批准**：明确同意引入候选依赖及其传递依赖风险。
2. **security reviewer 批准**：覆盖安全公告、禁用算法、密钥材料处理、异常回退、敏感响应不泄漏。
3. **architect 批准**：确认 JDK8 与 JDK17+ 双分支 public API、异常语义、依赖边界和长期维护成本一致。
4. **依赖锁定策略**：明确 groupId、artifactId、version、许可证、传递依赖、升级策略和漏洞响应策略。
5. **Provider 边界**：说明是否注册 JVM 全局 provider；如果注册，必须证明不会改变调用方既有 provider 顺序或给出 opt-in 策略。
6. **fixture 证明**：Argon2id + AES-GCM 与 Argon2id + ChaCha20-Poly1305 两类 madmin-go 默认 fixture 都要能在 Java 端解密。
7. **失败回退**：解密失败时必须保留 `EncryptedAdminResponse` 或等价边界信息，不能吞异常，不能泄露 secret、token、签名或明文敏感字段。

## 5. JDK 与分支测试矩阵

Gate Pass 实现提交前，至少要跑以下矩阵：

| 工作区 | JDK | 必跑验证 |
| --- | --- | --- |
| `minio-reactive-java` | JDK8 | `mvn -q -DfailIfNoTests=true test`、`scripts/madmin-fixtures/check-crypto-gate.sh` 的 Pass 版等价门禁、默认 fixture 解密单测。 |
| `minio-reactive-java-jdk17` | JDK17 | `mvn -q -DfailIfNoTests=true test`、默认 fixture 解密单测、真实 MinIO smoke。 |
| `minio-reactive-java-jdk17` | JDK21 | `mvn -q -DskipTests test-compile`，确认没有依赖或 API 退化。 |
| `minio-reactive-java-jdk17` | JDK25 | `mvn -q -DskipTests test-compile`，确认预览期/未来 JDK 下仍可编译。 |

如果候选依赖包含 native 组件，还要额外补 Linux x64、本机开发环境和 CI 环境的加载失败用例。阶段 45 未批准 native 依赖，因此当前只记录该风险，不新增相关实现。

## 6. 门禁文件处理原则

`crypto-gate-status.properties` 当前必须继续保持：

```properties
CRYPTO_GATE_STATUS=fail
CRYPTO_GATE_OWNER_APPROVED=false
CRYPTO_GATE_SECURITY_APPROVED=false
CRYPTO_GATE_ARCHITECT_APPROVED=false
```

只有当三方批准材料齐全、依赖方案明确、双分支测试矩阵通过后，才允许在专门 Gate Pass 提交中同步修改状态文件、脚本门禁、ADR、能力矩阵和 release gate。任何只改文档、不改门禁，或只改依赖、不补批准证据的做法都视为失败。

## 7. 对当前 SDK 完整性的影响

阶段 45 之后，当前发布口径保持不变：

- route parity 仍然证明 MinIO 公开路由已经登记。
- `raw-fallback = 0` 仍然证明所有 catalog 路由至少有专用或 advanced 入口。
- `encrypted-blocked = 9` 仍然存在，且必须在 Crypto Gate Pass 前继续存在。
- 用户遇到默认加密响应时应读取 `EncryptedAdminResponse.algorithm()` / `algorithmName()`，而不是把响应当普通 JSON。

这种边界会让 SDK 看起来少了少数明文 typed 模型，但它避免了更严重的问题：在没有审查的情况下把密码学依赖和 provider 行为传递给所有 SDK 使用者。

## 8. 阶段 45 验证命令

本阶段只验证“准备材料完整且 Gate Fail 没被绕过”：

```bash
scripts/madmin-fixtures/check-crypto-gate.sh
mvn -q -DfailIfNoTests=true test
git diff --check
```

JDK17+ 分支还要继续用 JDK21/JDK25 执行 `test-compile`，确认文档阶段没有引入依赖或源码变更导致编译矩阵退化。
