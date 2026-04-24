# 12 madmin 加密兼容决策门

## 1. 当前结论

本阶段只完成 madmin 默认加密响应的决策门，不引入新依赖，也不修改 `pom.xml`。当前 Java SDK 支持：

1. 生成 PBKDF2 + AES-GCM madmin 写入载荷。
2. 解密 PBKDF2 + AES-GCM madmin 载荷。
3. 识别 madmin-go 默认 Argon2id + AES-GCM 和 Argon2id + ChaCha20-Poly1305 载荷。
4. 对尚未支持的默认算法给出中文可解释错误，而不是把它伪装成普通坏数据。

当前 Java SDK 暂不支持：

1. Argon2id 密钥派生。
2. ChaCha20-Poly1305 AEAD。
3. 服务端默认加密响应的完整 typed 解析。

## 2. 为什么不能直接实现默认响应解密

MinIO madmin-go 默认路径会根据构建和运行环境选择 Argon2id + AES-GCM 或 Argon2id + ChaCha20-Poly1305。JDK8 标准库没有 Argon2id，也没有通用的 ChaCha20-Poly1305 JCE 支持。如果要完整解密默认响应，通常需要引入第三方 crypto 依赖。

crypto 依赖属于安全和长期维护决策，不能在普通功能提交中顺手加入。必须先完成 ADR，确认许可证、JDK8/JDK17 支持、是否 native、维护活跃度、安全风险、测试矩阵和回退方案。

## 3. 测试夹具

`src/test/resources/madmin-fixtures/` 中只保存测试 secret 和测试明文生成的载荷，不保存任何真实 MinIO access key 或 secret key。当前夹具包括：

| 文件 | 生成方式 | 预期 |
| --- | --- | --- |
| `pbkdf2-aesgcm-go.base64` | madmin-go v3.0.109，`go run -tags fips .` | Java 必须能解密为 `madmin fixture payload`。 |
| `argon2id-aesgcm-go-default.base64` | madmin-go v3.0.109，`go run .`（当前硬件默认算法为 `0x00` 时） | Java 必须能识别为 madmin 加密载荷，并明确报“不支持 Argon2id + AES-GCM”。 |

## 3.1 Go 驱动 fixture 脚本

系统 Go 已就绪后，推荐在当前工作区根目录执行以下脚本：

```bash
scripts/madmin-fixtures/generate-fixtures.sh
scripts/madmin-fixtures/verify-fixtures.sh
```

注意：madmin fixture 使用随机 salt 和 nonce，因此重新生成的密文不要求逐字节一致。当前校验关注的是：

1. 固定 `madmin-go` 版本
2. 算法 ID 是否符合预期
3. Java 是否能读取 / 诊断 Go 新生成的 fixture

## 3.2 为什么 fixture 不能逐字节比较

madmin fixture 含随机 salt 与 nonce，因此重新生成的密文不保证逐字节一致。当前工程化校验关注的是：

1. 固定 `madmin-go` 版本
2. 固定明文与测试 secret
3. 算法 ID 是否符合预期
4. Java 是否能读取 / 诊断新生成的 fixture

如果当前硬件默认算法是 `0x01`，生成脚本会产出 `argon2id-chacha20-go-default.base64`；当前仓库若尚未提交 committed ChaCha20 fixture，则 `verify-fixtures.sh` 会先验证 Go 新生成 fixture，再输出 skip reason 提醒仓库资源尚未补齐。

## 4.1 当前阶段结论

本轮阶段 9 的实际结论是：

- 已完成 Go 驱动 fixture / 互操作 / 校验脚本工程化。
- 尚未批准任何新的 Java crypto 依赖。
- 默认加密响应继续保持 `EncryptedAdminResponse` 或等价边界，不承诺明文 typed 解析。

## 4. 后续允许升级的条件

只有满足以下条件后，才能进入“实现默认响应解密”的阶段：

1. ADR 写明候选依赖、许可证、JDK8/JDK17 兼容性、native 风险、维护状态和替代方案。
2. `security-reviewer` 明确 APPROVE 安全、许可证和密钥/响应泄露风险。
3. `architect` 明确 APPROVE 双分支语义、依赖边界和长期维护影响。
4. JDK8 与 JDK17+ 都有 fixture 测试证明默认 madmin-go 响应可解密。
5. 失败回退仍保留 `EncryptedAdminResponse`，不能让调用方误以为凭证已经被解析。


## 5. 阶段 18 复核结果

阶段 18 已增加 `EncryptedAdminResponse.algorithm()` / `algorithmName()`，调用方可以在日志或诊断中看到服务端返回的 madmin 算法名称。该增强只提升边界可解释性，不改变 Gate Fail 结论。

复核命令：

```bash
scripts/madmin-fixtures/check-crypto-gate.sh
```

当前结论：Go fixture 互操作可以稳定验证，`pom.xml` 不新增 crypto 依赖，默认加密响应继续停留在 `EncryptedAdminResponse` 边界。

## 6. 阶段 25 决策复核结果

阶段 25 使用 2026-04-24 的公开资料重新复核候选依赖后，结论仍然是 **Crypto Gate Fail**：

1. Bouncy Castle 仍是后续最可行的纯 Java 候选之一，但它会把通用 crypto provider 带入 SDK 依赖面，必须先完成 owner、security reviewer、architect 三方批准。
2. `argon2-jvm` 是 native/JNA binding，且许可证与平台覆盖边界更复杂；它只能解决 Argon2，不能单独解决 ChaCha20-Poly1305。
3. 自研 Argon2id / ChaCha20-Poly1305 被拒绝，因为密码学实现不应由当前 SDK 自行维护。
4. `scripts/madmin-fixtures/check-crypto-gate.sh` 已加固为同时检查 `pom.xml` 和源码 import，防止未批准依赖绕过门禁。

因此，本 SDK 继续支持 PBKDF2 + AES-GCM 请求载荷和 fixture 解密；madmin-go 默认响应仍返回 `EncryptedAdminResponse`，并通过 `algorithm()` / `algorithmName()` 给出中文诊断边界。

详见 `docs/23-stage25-crypto-gate-review.md` 与 `docs/adr/001-madmin-default-encryption-dependency.md`。
