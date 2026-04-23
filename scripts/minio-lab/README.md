# MinIO destructive lab 说明

这里的脚本只服务于 **独立、可回滚** 的 Admin 实验环境。

## 硬门禁

只有满足以下条件时，才允许 destructive Admin 测试开始：

- `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
- `MINIO_LAB_ENDPOINT`、`MINIO_LAB_ACCESS_KEY`、`MINIO_LAB_SECRET_KEY` 已设置
- `MINIO_LAB_CAN_RESTORE=true`
- `MINIO_LAB_ENDPOINT` 不能等于共享环境 `http://127.0.0.1:9000`
- `scripts/minio-lab/verify-env.sh` 返回 0

## 为什么需要单独实验环境

destructive Admin 测试会修改配置、复制、quota 或远端目标。共享环境不适合做这类验证。

## 当前状态

- 目前只落地了硬门禁与环境验证。
- 真正的 config write + restore 用例将在实验环境准备完成后接入。
