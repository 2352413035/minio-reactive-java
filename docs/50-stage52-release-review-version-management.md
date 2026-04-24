# 阶段 52：发布复审与版本管理刷新

## 1. 本阶段结论

阶段 52 在完成阶段 47-51 后重新刷新发布复审和版本管理口径。当前 SDK 仍处于 `0.1.0-SNAPSHOT`：可以作为“路由完整、调用入口完整、风险边界明确”的发布候选继续验证，但还不是 1.0 稳定版。

| 口径 | 当前状态 |
| --- | --- |
| 双分支 route parity | JDK8 与 JDK17+ 均为 233 / 233，catalog 缺失 0、额外 0。 |
| 可调用覆盖 | `raw-fallback = 0`，所有公开 catalog 路由至少有专用 typed 或 advanced 兼容入口。 |
| 产品 typed 成熟度 | S3 77 / 77、Admin 81 / 128、KMS 7 / 7、STS 7 / 7、Metrics 6 / 6、Health 8 / 8。 |
| Crypto Gate | 继续 Gate Fail，`encrypted-blocked = 9`。 |
| 破坏性 Admin | 当前没有独立 lab 配置，`destructive-blocked = 29` 不减少。 |
| 版本线 | JDK8 `master` 与 JDK17+ `chore/jdk17-springboot3` 继续保持相同 SDK 语义。 |

## 2. 阶段 47-51 增量

1. **阶段 47**：IAM 与 bucket metadata 导出改为 `AdminBinaryResult`，Admin product-typed 提升到 66 / 128。
2. **阶段 48**：client devnull、site replication devnull/netperf、speedtest 系列改为 `AdminTextResult`，Admin product-typed 提升到 75 / 128。
3. **阶段 49**：Admin KMS 与专用 KMS 客户端边界收口，Admin product-typed 提升到 78 / 128。
4. **阶段 50**：IAM / IAM v2 / bucket metadata 导入 archive 入口落地，Admin product-typed 提升到 81 / 128。
5. **阶段 51**：复核独立 lab 窗口；没有本机 `lab.properties` 时不执行真实破坏性矩阵，只验证共享端点拒绝。

## 3. 版本管理口径

- JDK8 分支仍以 `master` 为主线，`pom.xml` 使用 `maven.compiler.source/target=1.8`。
- JDK17+ 分支仍以 `chore/jdk17-springboot3` 为主线，`pom.xml` 使用 `maven.compiler.release=17` 和 Spring Boot 3 依赖管理。
- 两条线当前仍使用同一个 Maven 版本号 `0.1.0-SNAPSHOT`，不在阶段 52 打正式 tag。
- 如果后续要发布预览版，应先决定版本号，例如 `0.1.0-alpha.N` 或 `0.1.0-rc.N`，再分别在两条分支生成签名/校验产物。
- 不允许 JDK17+ 分支悄悄扩大 public API 语义；除构建基线外，用户可见能力必须和 JDK8 线同步。

## 4. 机器报告

阶段 52 重新生成：

- `.omx/reports/route-parity-jdk8.md`
- `.omx/reports/route-parity-jdk17.md`
- `.omx/reports/route-parity-jdk8.json`
- `.omx/reports/route-parity-jdk17.json`
- `.omx/reports/capability-matrix.md`
- `.omx/reports/capability-matrix.json`
- `.omx/reports/capability-matrix-stage52-jdk8.md`
- `.omx/reports/capability-matrix-stage52-jdk17.md`

发布说明必须引用这些脚本产物，不允许手写统计值替代。

## 5. 阶段 52 验证命令

```bash
mvn -q -DfailIfNoTests=true test
mvn -q -Dtest=LiveMinioIntegrationTest test
scripts/madmin-fixtures/check-crypto-gate.sh
scripts/minio-lab/verify-env.sh
python3 scripts/report-route-parity.py --minio-root ../minio --worktree . --format markdown --output ../.omx/reports/route-parity-jdk8.md
python3 scripts/report-capability-matrix.py --worktree . --format markdown --output ../.omx/reports/capability-matrix-stage52-jdk8.md
git diff --check
```

JDK17+ 分支还要执行 JDK21/JDK25 `test-compile`。真实破坏性 lab 仍需本机私有 `lab.properties` 或等价环境变量，本阶段没有执行。

## 6. 仍未完成的边界

- Admin product-typed 仍为 81 / 128，剩余接口大多是高风险写入、环境相关诊断、字段漂移较强的管理端能力。
- `encrypted-blocked = 9` 需要 Crypto Gate Pass 后才能减少。
- `destructive-blocked = 29` 需要独立 lab 真实报告后才能减少。
- 不能把 route parity 233 / 233 解读为“所有 Admin 能力都已经最终产品化”。
