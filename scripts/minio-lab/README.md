# MinIO destructive lab 说明

这里的脚本只服务于 **独立、可回滚** 的 Admin 实验环境。共享 MinIO（例如 `http://127.0.0.1:9000`）只允许安全 live 测试，不能执行 destructive Admin 测试。

## 硬门禁

只有满足以下条件时，才允许 destructive Admin 测试开始：

- `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
- `MINIO_LAB_ENDPOINT`、`MINIO_LAB_ACCESS_KEY`、`MINIO_LAB_SECRET_KEY` 已设置
- `MINIO_LAB_CAN_RESTORE=true`
- `MINIO_LAB_ENDPOINT` 不能等于共享环境或常见本机默认环境：`http://127.0.0.1:9000`、`http://localhost:9000`、`http://0.0.0.0:9000`
- 如果同时设置了 `MINIO_ENDPOINT`，`MINIO_LAB_ENDPOINT` 不能与它相同
- `scripts/minio-lab/verify-env.sh` 返回 0

## 运行方式

```bash
export MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true
export MINIO_LAB_ENDPOINT=http://独立-lab:9000
export MINIO_LAB_ACCESS_KEY=...
export MINIO_LAB_SECRET_KEY=...
export MINIO_LAB_CAN_RESTORE=true

scripts/minio-lab/run-destructive-tests.sh
```

## config write + restore 用例

如果要执行真实配置写入与恢复，还必须提供一组可回滚配置：

```bash
export MINIO_LAB_TEST_CONFIG_KV='api requests_max=10'
export MINIO_LAB_RESTORE_CONFIG_KV='api requests_max=0'
```

测试流程是：

1. 运行 `verify-env.sh`，证明这是独立 lab。
2. 使用 `setConfigKvText(MINIO_LAB_TEST_CONFIG_KV)` 写入测试配置。
3. 通过 `getConfigHelp(...)` 做只读探测，证明服务端仍可响应。
4. finally 中使用 `setConfigKvText(MINIO_LAB_RESTORE_CONFIG_KV)` 恢复。
5. 恢复后再次执行只读探测。

如果没有提供这两个变量，真实配置写入测试会跳过；默认 `mvn test` 永远不会修改 MinIO 服务端配置。
