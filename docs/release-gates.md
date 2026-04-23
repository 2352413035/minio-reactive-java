# 发布门禁

本项目在对外标记里程碑或准备发布前，至少要通过以下门禁。

## 1. 基础验证

- JDK8：`mvn -q -DfailIfNoTests=true test`
- JDK17：`mvn -q -DfailIfNoTests=true test`
- JDK8 / JDK17 real MinIO：`LiveMinioIntegrationTest`
- JDK21 / JDK25：JDK17+ compile
- `git diff --check`
- secret scan

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

## 4. 能力总表门禁

发布或里程碑说明必须引用 `scripts/report-capability-matrix.py` 产物，而不是手写统计值。

至少输出：

- `catalog`
- `typed`
- `advanced`
- `raw-only`
- `encrypted-blocked`
- `destructive-blocked`

## 5. 文档门禁

- README 必须明确 typed 优先、advanced 过渡、raw 兜底。
- 中文文档必须明确当前 crypto 边界。
- 示例必须覆盖至少一个对象存储主路径和一个 Admin / raw 兜底路径。

## 6. 审查门禁

- `architect`：架构边界、双分支语义一致性
- `security-reviewer`：涉及 crypto 依赖或敏感响应时必须审查
- `verifier`：验证证据与能力总表闭环
