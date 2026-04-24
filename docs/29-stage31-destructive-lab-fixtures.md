# 阶段 31：破坏性实验夹具深化

阶段 31 的目标不是增加公开接口数量，而是让破坏性 Admin 能力的验证更可信、更容易回滚、更容易审计。

## 1. 设计目标

1. 共享 MinIO 仍然只能跑安全 live 测试，不能执行 破坏性 Admin 测试。
2. 独立 lab 必须先通过 `scripts/minio-lab/verify-env.sh`。
3. tier、remote target、batch job 夹具同时验证专用 typed 客户端和 `ReactiveMinioRawClient` catalog 兜底调用。
4. 每次 破坏性实验环境运行都生成本机报告，记录夹具开关、端点指纹和失败恢复提示。
5. 报告和配置文件不得包含 access key、secret key、session token 或请求签名。

## 2. 新增和强化的 夹具

| 夹具 | 变量 | 行为 |
| --- | --- | --- |
| tier 列表匹配 | `MINIO_LAB_TIER_NAME`、`MINIO_LAB_EXPECT_TIER_IN_LIST` | 先调用 `listTiers()`，再用 raw `ADMIN_LIST_TIER` 交叉比对数量；可选要求列表中必须出现指定 tier。 |
| remote target 摘要 | `MINIO_LAB_BUCKET`、`MINIO_LAB_REMOTE_TARGET_TYPE`、`MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN` | 调用 `listRemoteTargetsInfo(...)`，再用 raw `ADMIN_LIST_REMOTE_TARGETS` 交叉比对数量；可选要求出现指定 ARN。 |
| batch job 摘要 | `MINIO_LAB_ENABLE_BATCH_JOB_PROBES`、`MINIO_LAB_BATCH_EXPECTED_JOB_ID` | 调用 batch job typed 摘要，再用 raw `ADMIN_LIST_BATCH_JOBS`、`ADMIN_BATCH_JOB_STATUS`、`ADMIN_DESCRIBE_BATCH_JOB` 交叉比对；可选要求出现指定任务 ID。 |

这些夹具仍然默认跳过。只有独立 lab 配置文件或环境变量显式设置后才会执行。

## 3. 报告生成

`scripts/minio-lab/run-destructive-tests.sh` 会在退出时调用 `write-report.sh`，默认写入：

```text
target/minio-lab-reports/destructive-lab-<UTC时间>.md
```

可以通过 `MINIO_LAB_REPORT_DIR` 覆盖目录。报告模板见：

```text
scripts/minio-lab/report-template.md
```

报告只写入：

- 脱敏端点指纹。
- 夹具是否启用。
- bucket、tier 等资源名称。
- 是否设置了 remote target ARN 或 batch job ID。
- 失败恢复提示。

报告不会写入：

- access key。
- secret key。
- session token。
- 请求签名。
- 完整 Authorization 头。

## 4. 失败恢复顺序

1. 如果 config KV 写入失败，优先使用 `MINIO_LAB_RESTORE_CONFIG_KV` 恢复。
2. 如果 bucket quota 写入失败，优先使用 `MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON` 恢复。
3. 如果 tier、remote target、batch job 探测失败，先确认 typed/raw 两条路径是否都失败，再查看 MinIO 管理日志。
4. 如果夹具修改了远端状态，只能在独立 lab 中用控制台或 `mc admin` 回滚，不能把共享 live 环境当成恢复目标。

## 5. 验证口径

阶段 31 完成后，发布说明引用破坏性实验环境证据时应同时说明：

- `verify-env.sh` 会拒绝共享端点。
- 假的独立 lab 端点 能通过门禁格式校验，但不会执行真实 破坏性测试。
- `run-destructive-tests.sh` 在门禁失败时也会生成失败报告，便于定位和审计。
- 单元测试默认跳过 破坏性用例，不会修改用户提供的共享 MinIO。
