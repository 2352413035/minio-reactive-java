# 阶段 76：Crypto Gate Pass 可执行清单

## 1. 本阶段目标

阶段 76 只整理 Crypto Gate Pass 的可执行准备材料，不新增依赖、不修改 `pom.xml`、不改变当前 Gate Fail 状态。当前 SDK 已经能识别 madmin 默认加密响应并提供 `EncryptedAdminResponse` 安全边界，但 11 个默认加密响应仍不能升级为明文 typed 模型。

## 2. 当前 Gate Fail 原因

当前不能放行的原因不是 API 缺失，而是以下材料仍未全部满足：

1. Argon2id / ChaCha20-Poly1305 Java 依赖尚未完成选型。
2. 依赖许可证、维护状态、安全公告和 JDK8 兼容性尚未复核。
3. owner / security / architect 三方批准尚未记录。
4. 默认 madmin-go fixture 的 Java 解密成功路径尚未覆盖双分支。
5. 解密失败时回退到 `EncryptedAdminResponse` 的语义尚未在完整矩阵中验证。
6. FIPS / provider 行为说明尚未写入发布边界。

## 3. 候选依赖审查表

| 项 | 必须回答的问题 | 放行要求 |
| --- | --- | --- |
| Argon2id 实现 | 是否支持 JDK8、是否维护活跃、是否有 CVE 记录 | 明确版本、许可证和安全公告查询日期。 |
| ChaCha20-Poly1305 实现 | JDK8 下是否需要第三方 provider | 明确 provider 初始化方式和失败回退。 |
| 许可证 | 是否与项目发布方式兼容 | 记录许可证文本和 Maven 坐标。 |
| 体积与传递依赖 | 是否引入大型或不必要依赖 | 列出依赖树并解释必要性。 |
| FIPS/合规 | 在受限 provider 环境下如何表现 | 明确禁用、降级或报错策略。 |

## 4. 必须补齐的测试矩阵

Crypto Gate Pass 前，至少需要以下测试：

1. JDK8：Argon2id + AES-GCM 默认 fixture 解密成功。
2. JDK8：Argon2id + ChaCha20-Poly1305 默认 fixture 解密成功。
3. JDK17：上述两类 fixture 解密成功。
4. JDK21 / JDK25：`test-compile` 与 fixture 解密测试通过。
5. 错误 secret：明确抛出中文异常，不泄露明文或 secret。
6. 短载荷、未知算法、篡改 tag：继续安全失败。
7. 解密失败路径：调用方仍可拿到或重构 `EncryptedAdminResponse` 边界信息。
8. 真实 MinIO live：只读取安全对象，不打印配置、access key、service account 或 IDP 明文。

## 5. 必须保留的回退语义

即使 Crypto Gate Pass，以下语义也不能删除：

- `EncryptedAdminResponse.encryptedData()` 继续可用。
- `algorithm()` / `algorithmName()` / `diagnosticMessage()` 继续可用于排障。
- 解密失败不能吞异常，不能返回半解析对象。
- 任何敏感字段都不能默认写入日志、报告或文档。
- 用户可以选择只处理加密边界而不启用明文解析。

## 6. 放行前检查项

- [ ] 依赖 ADR 已更新。
- [ ] 依赖许可证和安全公告已记录。
- [ ] owner 批准已记录。
- [ ] security 批准已记录。
- [ ] architect 批准已记录。
- [ ] JDK8/JDK17/JDK21/JDK25 测试矩阵通过。
- [ ] 默认 madmin-go 两类 fixture 解密成功。
- [ ] 失败回退测试通过。
- [ ] 文档说明哪些接口会从 `EncryptedAdminResponse` 升级为明文模型。
- [ ] 发布说明明确如何关闭或绕过明文解析。

## 7. 本阶段结论

阶段 76 不放行 Crypto Gate。它只是把后续真正放行前必须完成的材料拆成可执行清单。当前能力矩阵仍保持：

- `encrypted-blocked = 11`
- `destructive-blocked = 29`
- `raw-fallback = 0`
