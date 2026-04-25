# 阶段 77：独立破坏性 lab 门禁复核与准备度审计

## 1. 本阶段目标

阶段 77 只处理破坏性 Admin 实验环境，不改变 SDK 公开 API、不降低 `destructive-blocked = 29` 统计口径，也不把共享 MinIO 当作真实破坏性验证环境。

本阶段要回答三个问题：

1. 当前仓库是否已经存在可直接执行的独立 lab 配置？
2. 没有独立 lab 时，门禁是否仍会拒绝执行？
3. 如果后续拿到独立 lab，如何先做无凭证泄漏的准备度审计，再执行 typed/raw 矩阵？

## 2. 当前事实

当前脚本链路如下：

```text
scripts/minio-lab/run-destructive-tests.sh
  -> scripts/minio-lab/load-config.sh
  -> scripts/minio-lab/verify-env.sh
  -> DestructiveAdminIntegrationTest
  -> scripts/minio-lab/write-report.sh
```

配置来源有两类：

- 本机私有配置文件：`MINIO_LAB_CONFIG_FILE` 指向的文件，或默认 `scripts/minio-lab/lab.properties`。
- 运行时环境变量：`MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS`、`MINIO_LAB_ENDPOINT`、`MINIO_LAB_ACCESS_KEY`、`MINIO_LAB_SECRET_KEY`、`MINIO_LAB_CAN_RESTORE` 等。

仓库内只有 `scripts/minio-lab/lab.example.properties` 示例模板，没有提交真实 `lab.properties`。因此当前不能宣称“独立破坏性 lab 已经执行通过”。

## 3. 本阶段新增准备度审计脚本

新增：

```text
scripts/minio-lab/audit-readiness.sh
```

它的职责是：

- 读取本机私有配置文件或环境变量。
- 展示必需门禁项是否已设置。
- 复用 `verify-env.sh` 得出“通过/拒绝”结果。
- 只输出 access key、secret key、请求体、删除 ARN 等敏感项的“已设置/未设置”，不输出真实值。
- 不连接 MinIO，不执行写入测试，不生成真实破坏性报告。

通过示例：

```bash
scripts/minio-lab/audit-readiness.sh
```

如果脚本通过，只代表可以启动 `run-destructive-tests.sh`；真实 typed/raw 证据仍必须来自后续执行报告。

## 4. 无独立 lab 时的正确结果

没有设置任何 lab 变量时，审计脚本和 `verify-env.sh` 都必须拒绝，并提示必须显式设置：

```text
MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true
```

如果把 `MINIO_LAB_ENDPOINT` 指向共享 MinIO 或常见本机默认端点，也必须拒绝，并提示不能指向共享环境。

这两个拒绝结果是安全门禁的一部分，不是测试失败。

## 5. 有独立 lab 后的执行顺序

拿到独立、可回滚 MinIO lab 后，按下面顺序执行：

1. 把真实参数放入仓库外私有配置文件，或放入当前 shell 环境变量。
2. 运行 `scripts/minio-lab/audit-readiness.sh`，确认门禁准备度。
3. 运行 `scripts/minio-lab/run-destructive-tests.sh`。
4. 检查 `target/minio-lab-reports/` 下的报告。
5. 确认报告中 typed 专用客户端路径与 raw catalog 兜底路径都有明确 PASS/FAIL、异常类型和恢复提示。
6. 确认报告、终端日志和仓库变更不包含 access key、secret key、token、签名或请求体。

只有这些证据满足后，才能讨论降低 29 个 `destructive-blocked` 边界中的对应项。

## 6. 本阶段不做的事

- 不把共享 `http://127.0.0.1:9000` 当作独立 lab。
- 不把 `lab.example.properties` 复制成真实 `lab.properties` 提交。
- 不在文档、报告或提交信息中写入真实凭证。
- 不减少 `destructive-blocked = 29`。
- 不修改 SDK 的 route catalog 或 typed 覆盖统计。

## 7. 验证口径

本阶段完成时应满足：

- `audit-readiness.sh` 语法通过。
- 无环境变量时，审计脚本拒绝执行。
- 指向共享端点时，审计脚本拒绝执行。
- 指向非共享端点且必需门禁变量齐全时，审计脚本只证明门禁通过，不声称真实 lab 已跑完。
- route parity 仍为 233 / 233。
- capability matrix 仍为 product-typed 233 / 233、raw-fallback 0、encrypted-blocked 11、destructive-blocked 29。
