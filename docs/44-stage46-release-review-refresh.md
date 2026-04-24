# 阶段 46：发布复审刷新

## 1. 复审结论

阶段 46 重新生成 route parity 与 capability matrix，并把阶段 40 到阶段 45 的真实变化重新收口到发布口径中。当前结论如下：

| 口径 | 当前状态 |
| --- | --- |
| 路由对标 | JDK8 与 JDK17+ 双分支均为 233 / 233，catalog 缺失 0、额外 0。 |
| 可调用覆盖 | `raw-fallback = 0`，所有 catalog 路由至少有专用或 advanced 入口。 |
| 产品 typed 成熟度 | 阶段 46 为 S3 77 / 77、Admin 64 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8；阶段 47 后 Admin 提升到 66 / 128，阶段 48 后提升到 75 / 128，阶段 49 后提升到 78 / 128，阶段 50 后提升到 81 / 128；阶段 52 复审后确认当前仍为 81 / 128，阶段 53 后提升到 88 / 128，阶段 54 后提升到 94 / 128。 |
| Crypto Gate | 继续 Gate Fail；阶段 45 只完成放行准备清单，不引入依赖。 |
| 破坏性 Admin | 仍为独立 lab 边界；阶段 43 已增强 typed/raw 步骤证据和 `mc` 核验提示。 |
| 错误体验 | 阶段 44 已将协议异常与 raw 本地校验统一为中文诊断。 |

## 2. 阶段 40-45 的增量

1. **阶段 40**：Admin metrics、inspect-data、profiling/profile 增加文本或二进制诊断包装，Admin product-typed 从 55 提升到 60。
2. **阶段 41**：LDAP/OpenID access key 和 LDAP policy entities 增加安全只读摘要，Admin product-typed 从 60 提升到 63。
3. **阶段 42**：site replication peer IDP settings 增加安全摘要，并补齐 site replication 专用调用的 `api-version=1`，Admin product-typed 从 63 提升到 64。
4. **阶段 43**：破坏性 lab 报告增加 typed/raw 执行明细和 `mc` 只读恢复核验提示。
5. **阶段 44**：错误说明默认中文化，保留结构化异常字段，raw 构造校验也给出中文原因。
6. **阶段 45**：Crypto Gate Pass 准备清单落地，但 Gate Fail 状态不变，未新增 crypto/native/provider 依赖。

## 3. 机器报告

阶段 46 使用以下报告作为发布复审依据：

- `.omx/reports/route-parity-jdk8.md`
- `.omx/reports/route-parity-jdk17.md`
- `.omx/reports/route-parity-jdk8.json`
- `.omx/reports/route-parity-jdk17.json`
- `.omx/reports/capability-matrix.md`
- `.omx/reports/capability-matrix.json`
- `.omx/reports/capability-matrix-jdk8.md`
- `.omx/reports/capability-matrix-jdk17.md`

这些报告是由脚本生成，不手写统计值。

## 4. 当前不能夸大的边界

- 233 / 233 代表路由目录对标完成，不代表 Admin 全部接口都已经是最终产品级 typed 模型。
- `raw-fallback = 0` 代表每个 catalog 路由至少有可调用入口，不代表每个入口都适合普通业务默认使用。
- `encrypted-blocked = 9` 仍然存在；Crypto Gate Pass 前不能把 madmin 默认加密响应伪装成明文 JSON。
- `destructive-blocked = 29` 仍然需要独立、可回滚 lab；共享 MinIO 只做安全 live smoke。
- 用户提供的真实 MinIO/API/UI 凭证只允许作为运行时输入，不允许写入仓库、报告、提交信息或文档。

## 5. 阶段 46 验证命令

本阶段发布复审至少执行：

```bash
python3 scripts/report-route-parity.py --minio-root ../minio --worktree . --format markdown --output ../.omx/reports/route-parity-jdk8.md
python3 scripts/report-capability-matrix.py --worktree . --format markdown --output ../.omx/reports/capability-matrix-jdk8.md
scripts/madmin-fixtures/check-crypto-gate.sh
mvn -q -DfailIfNoTests=true test
mvn -q -Dtest=LiveMinioIntegrationTest test
git diff --check
```

JDK17+ 分支还要执行 JDK17 全量测试、真实 MinIO smoke，以及 JDK21/JDK25 `test-compile`。

## 6. 阶段 47 后续刷新

阶段 47 已补充 `exportIamData()` 与 `exportBucketMetadataData()` 二进制产品边界，Admin product-typed 提升到 66 / 128。完整说明见 `docs/45-stage47-admin-sensitive-export.md`。

## 7. 阶段 48 后续刷新

阶段 48 已补充 client devnull、site replication devnull/netperf 和 speedtest 系列 `AdminTextResult` 产品入口，Admin product-typed 提升到 75 / 128。完整说明见 `docs/46-stage48-admin-diagnostic-probes.md`。

## 8. 阶段 49 后续刷新

阶段 49 已收口 Admin KMS 与专用 KMS 客户端关系，Admin product-typed 提升到 78 / 128。完整说明见 `docs/47-stage49-admin-kms-boundary.md`。

## 9. 阶段 50 后续刷新

阶段 50 已补充 IAM、IAM v2 和 bucket metadata 导入 archive 产品入口，Admin product-typed 提升到 81 / 128，但 destructive-blocked 不减少。完整说明见 `docs/48-stage50-sensitive-import-lab-boundary.md`。

## 10. 阶段 51 后续刷新

阶段 51 已确认当前没有独立 lab 配置，并重新验证共享端点拒绝逻辑。`destructive-blocked = 29` 不减少，完整说明见 `docs/49-stage51-independent-lab-window.md`。

## 11. 阶段 52 后续刷新

阶段 52 已重新生成 route parity 和 capability matrix，确认双分支 route parity 233 / 233、Admin product-typed 81 / 128、`raw-fallback = 0`，并刷新版本管理口径。阶段 53 继续把 heal、decommission、rebalance 维护操作纳入产品边界，Admin product-typed 提升到 88 / 128；阶段 54 补充 replication MRF、tier verify 和 policy attach/detach 语义化入口，Admin product-typed 提升到 94 / 128。完整说明见 `docs/50-stage52-release-review-version-management.md`、`docs/51-stage53-admin-maintenance-boundary.md` 与 `docs/52-stage54-admin-policy-replication-boundary.md`。

## 12. 下一阶段建议

阶段 46 之后，优先级仍然是：

1. 继续把 Admin 中低风险只读接口升级为产品级 typed 摘要。
2. 如果用户提供独立 lab，执行破坏性写入矩阵并保存本机报告证据。
3. Crypto Gate 只有在三方批准、依赖审查和测试矩阵齐全后才进入实现。
4. 发布说明继续采用 route parity、callability、typed maturity、live/destructive/crypto 边界的分层口径。
