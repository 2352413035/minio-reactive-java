# 发布门禁

本项目在对外标记里程碑或准备发布前，至少要通过以下门禁。

## 1. 基础验证

- JDK8：`mvn -q -DfailIfNoTests=true test`
- JDK17：`mvn -q -DfailIfNoTests=true test`
- JDK8 / JDK17 real MinIO：`LiveMinioIntegrationTest`
- JDK21 / JDK25：JDK17+ compile
- `git diff --check`
- secret scan

阶段 19 发布收口还要求 main examples 可编译，因为示例是用户理解 SDK 分层的第一入口。

## 2. Go 互操作门禁

如果当前里程碑涉及 madmin 加密兼容，必须额外通过：

- `scripts/madmin-fixtures/verify-fixtures.sh`
- 固定 `go version`
- 固定 `madmin-go` 版本
- fixture metadata 与文档一致

## 3. destructive Admin 门禁

默认发布流程不执行 destructive Admin 测试。

只有满足以下条件时，才允许把 destructive 验证列为“已完成”：

- `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
- `scripts/minio-lab/verify-env.sh` 返回 0
- 使用独立、可回滚的实验环境
- 至少一条 config write + restore 流程成功

## 4. 路由对标（route parity）与能力总表门禁

发布或里程碑说明必须引用脚本产物，而不是手写统计值。

路由对标必须输出：

- `scripts/report-route-parity.py` 的 markdown 或 json 报告。
- 服务端 router 与 SDK catalog 的 family/method/path/query/auth 差异。
- `missingFromCatalog = 0` 且 `extraInCatalog = 0`，除非文档明确说明该路由是内部、dummy 或 rejected route。

能力总表至少输出：

- `route-catalog`
- `product-typed`
- `advanced-compatible`
- `raw-fallback`
- `encrypted-blocked`
- `destructive-blocked`

## 5. 文档门禁

- README 必须明确 typed 优先、advanced 过渡、raw 兜底。
- 中文文档必须明确当前 crypto 边界。
- 示例必须覆盖至少一个对象存储主路径、一个 Admin typed 路径、一个 raw 兜底路径和一个运维入口路径。
- 发布说明不得使用单一百分比宣称“完成 MinIO”，必须分 route parity、callability、typed maturity、live/destructive/crypto 边界说明。

## 6. 审查门禁

- `architect`：架构边界、双分支语义一致性
- `security-reviewer`：涉及 crypto 依赖或敏感响应时必须审查
- `verifier`：验证证据与能力总表闭环


## Destructive Admin Lab

破坏性 Admin 测试必须通过 `scripts/minio-lab/verify-env.sh`，并且只能由 `scripts/minio-lab/run-destructive-tests.sh` 在独立可回滚环境中启动。默认 `mvn test` 只会跳过 destructive 用例，不允许修改共享 MinIO。

真实 config write + restore 需要额外提供 `MINIO_LAB_TEST_CONFIG_KV` 与 `MINIO_LAB_RESTORE_CONFIG_KV`。这两个变量缺失时，测试只验证环境门禁，不执行配置写入。

## 阶段 19 发布就绪检查清单

发布或里程碑说明至少要附上以下证据：

1. `.omx/reports/route-parity-jdk8.md` 与 `.omx/reports/route-parity-jdk17.md` 均显示 catalog 缺失 0、额外 0。
2. `.omx/reports/capability-matrix.md` 显示双分支能力矩阵一致。
3. `docs/17-release-readiness-report.md` 记录当前完成度、风险边界、验证命令和下一阶段计划入口。
4. README、`docs/09-minio-api-catalog.md`、`docs/13-admin-risk-levels.md`、`docs/14-typed-client-usage-guide.md`、`docs/16-crypto-boundary-map.md` 的口径一致。
5. 示例类全部位于 `io.minio.reactive.examples` 包下，并通过 `mvn test` 的 main compile 阶段。
