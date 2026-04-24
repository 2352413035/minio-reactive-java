# 阶段 64：发布工程与外部门禁清单

## 1. 本阶段结论

阶段 64 不打 tag、不发布 Maven、不改变版本号。本阶段只把正式发布前还需要的外部门禁和发布工程材料写清楚，避免把“已经完成 SDK API 对标”误读为“可以立即 1.0 发布”。

当前项目状态：

- `0.1.0-SNAPSHOT` 继续保留。
- route parity：233 / 233。
- product-typed：S3/Admin/KMS/STS/Metrics/Health 全部分组满格。
- `raw-fallback = 0`。
- `encrypted-blocked = 9` 仍未放行。
- `destructive-blocked = 29` 仍未放行。

## 2. 不允许直接发布 1.0 的原因

| 阻塞项 | 当前状态 | 为什么阻塞正式发布 |
| --- | --- | --- |
| Crypto Gate | Fail | 默认 madmin 加密响应还没有三方批准、依赖审查和完整测试矩阵。 |
| 独立破坏性 lab | 未配置 | tier、remote target、batch、site replication、force-unlock 等写入能力还没有独立可回滚环境的真实执行报告。 |
| 发布工程材料 | 未收口 | Maven 发布坐标、签名、SBOM、变更说明、兼容性承诺和回滚策略尚未形成最终材料。 |
| 结果模型深化 | 可继续做 | 128 / 128 表示产品边界完整，不代表每个 Admin 文本/JSON 响应都已经拆成最细业务模型。 |

因此，当前最准确的发布口径是“功能入口和风险边界闭环的 0.1.0-SNAPSHOT 发布候选”，而不是“风险能力全部真实放行的正式 1.0”。

## 3. Crypto Gate Pass 清单

只有全部满足后，才能把 `encrypted-blocked = 9` 里的默认 madmin 加密响应升级为明文模型：

1. owner 批准：确认维护成本、兼容范围和失败回退策略。
2. security-reviewer 批准：确认密码学依赖、许可证、安全公告、FIPS/Provider 影响和敏感日志策略。
3. architect 批准：确认 JDK8/JDK17+ 双分支长期维护方式。
4. 明确候选依赖版本，不引入未审查 native/provider 行为。
5. 双分支测试矩阵：JDK8、JDK17、JDK21、JDK25。
6. Go madmin fixture 与 Java 解密结果互操作验证。
7. 失败回退：不支持的算法必须返回中文可解释边界，不允许静默错解。
8. 文档更新：`docs/12-madmin-encryption-compat.md`、`docs/16-crypto-boundary-map.md`、`docs/43-stage45-crypto-gate-pass-prep.md`、`docs/release-gates.md` 同步更新。

在这些条件满足前，正确行为仍是返回 `EncryptedAdminResponse`。

## 4. 独立破坏性 lab 放行清单

只有全部满足后，才能减少 `destructive-blocked = 29`：

1. 设置 `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`。
2. `scripts/minio-lab/verify-env.sh` 返回 0。
3. `MINIO_LAB_ENDPOINT` 不是共享端点，也不能与 `MINIO_ENDPOINT` 指向同一环境。
4. 具备 `MINIO_LAB_CAN_RESTORE=true`，并记录恢复方案。
5. tier、remote target、batch、site replication 写入夹具如启用，必须设置 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`。
6. 真实执行 typed 与 raw 双路径矩阵。
7. 报告只记录环境指纹、步骤名、PASS/FAIL、异常类型和恢复提示，不记录凭证、token、签名、请求体或完整敏感堆栈。
8. 每次执行后保存本机报告，并在发布说明中明确它来自独立 lab，不来自共享 MinIO。

共享 `http://127.0.0.1:9000` 只能用于 smoke、只读和 live 安全路径，不能用于破坏性矩阵放行。

## 5. Maven / tag 发布工程清单

当外部门禁满足并决定发布时，至少需要准备：

1. 版本决策：从 `0.1.0-SNAPSHOT` 切到明确版本，例如 `0.1.0` 或更高版本。
2. 双分支策略：说明 JDK8 与 JDK17+ 是否同时发布、artifactId 是否区分、兼容承诺是什么。
3. 构建验证：JDK8/JDK17 单元测试、真实 MinIO live、JDK21/JDK25 compile、route/capability、Crypto/lab 门禁。
4. 产物验证：`mvn package`、源码包、javadoc 包、pom 元数据、许可证说明。
5. 供应链材料：签名、校验和、SBOM 或依赖清单、依赖许可证检查。
6. 发布说明：列出已完成能力、风险边界、升级指南、破坏性 lab 状态和已知限制。
7. 回滚策略：发布失败或发现兼容问题时如何撤回、废弃或发布修复版本。
8. 提交与 tag：提交信息继续使用 Lore Commit Protocol；tag 前必须确认两个工作区干净。

本阶段不执行这些发布动作，只记录清单。

## 6. 当前仍可继续做的非发布阻塞工作

即使不满足外部门禁，也可以继续做：

- 为稳定 Admin JSON/text 响应补更细中文结果模型。
- 完善示例和用户指南。
- 增加更多只读 smoke 或 mc 旁证。
- 改进错误消息和排障提示。
- 增强报告脚本可读性。

这些工作不能降低 `encrypted-blocked` 或 `destructive-blocked`，也不应新增重复同义 API。

## 7. 本阶段验证要求

阶段 64 仍按发布候选门禁重新验证：

- route parity 233 / 233。
- capability matrix Admin 128 / 128，`raw-fallback = 0`。
- JDK8/JDK17 单元测试。
- JDK8/JDK17 真实 MinIO live 测试。
- JDK21/JDK25 `test-compile`。
- Crypto Gate Fail 复核。
- 破坏性 lab 无配置与共享端点拒绝。
- `bash -n`。
- `git diff --check`。
- 真实凭证扫描。
- 双分支文档同步检查。
