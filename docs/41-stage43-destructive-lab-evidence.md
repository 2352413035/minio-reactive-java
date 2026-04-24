# 阶段 43：破坏性 lab 真实证据报告增强

阶段 43 的目标不是在共享 MinIO 上执行破坏性操作，而是让独立、可回滚 lab 的执行证据更容易审计。此前报告能说明夹具是否启用、请求体是否设置和失败后如何恢复；本阶段继续补齐 typed/raw 步骤级证据和 `mc` 只读核验提示。

## 1. 步骤状态文件

`scripts/minio-lab/run-destructive-tests.sh` 会为每次执行生成：

```text
target/minio-lab-reports/destructive-lab-<UTC时间>.steps
```

也可以通过 `MINIO_LAB_STEP_STATUS_FILE` 覆盖。`DestructiveAdminIntegrationTest` 在执行关键步骤时写入：

```text
范围|步骤|PASS/FAIL|说明
```

示例：

```text
tier-write|typed addTier|PASS|
tier-write|raw ADMIN_ADD_TIER|PASS|
remote-target-write|raw ADMIN_SET_REMOTE_TARGET|FAIL|IllegalStateException
```

步骤文件只记录范围、步骤名、PASS/FAIL 和异常类型，不记录请求体、access key、secret key、token、签名或完整异常堆栈。

## 2. 报告新增字段

`write-report.sh` 新增：

- `步骤状态文件`：说明本次报告读取哪个步骤状态文件。
- `typed/raw 执行明细`：按表格列出专用客户端和 raw 兜底路径各自的通过/失败情况。
- `mc 恢复/核验提示`：如果系统安装了 `mc`，报告会提示使用本机私有 alias 做只读核验。

`MINIO_LAB_MC_ALIAS` 只用于报告中的命令提示。alias 必须由用户在本机 `mc` 配置中维护，不能把 `mc alias set` 命令或凭证写入仓库。

## 3. 仍然保持的边界

- 无 lab 环境时，`DestructiveAdminIntegrationTest` 仍然默认跳过。
- 共享端点、常见本机默认端点和缺少回滚能力的环境仍被 `verify-env.sh` 拒绝。
- 检测到写入请求体但没有 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true` 时仍直接失败。
- 报告只适合保存在本机 lab 证据目录，不应提交到 SDK 仓库。

## 4. 验证方式

本阶段的默认验证不执行真实破坏性写入，只验证：

- `bash -n scripts/minio-lab/*.sh` 语法通过。
- 使用合成步骤状态文件调用 `write-report.sh`，报告能渲染 typed/raw 明细和 `mc` 提示。
- `DestructiveAdminIntegrationTest` 在未开启 lab 时仍跳过，不修改共享环境。
- 双分支常规单元、live、route parity、capability matrix 和 Crypto Gate 继续通过。
