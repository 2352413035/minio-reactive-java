# 阶段 51：独立 lab 真实执行窗口

## 1. 本阶段结论

阶段 51 检查是否可以进入破坏性 Admin 独立 lab 真实执行。当前工作区没有提交或本机私有的 `scripts/minio-lab/lab.properties`，因此本阶段**不执行真实破坏性写入矩阵**，只验证门禁仍然会拒绝默认共享环境。

这不是功能完成声明，而是一次安全复核：在没有独立可回滚 lab 的情况下，SDK 不允许把共享 MinIO 当作破坏性验证环境。

## 2. 当前环境事实

- `mc` 已安装，版本为 `RELEASE.2025-08-13T08-35-41Z`。
- `scripts/minio-lab/lab.properties` 不存在，仓库只保留 `lab.example.properties`。
- 共享 MinIO endpoint `http://127.0.0.1:9000` 被 `verify-env.sh` 明确拒绝。
- 本阶段没有把任何 access key、secret key、UI 登录口令或请求体写入仓库。

## 3. 已执行的安全门禁验证

缺少显式开关时，双分支均失败：

```bash
scripts/minio-lab/verify-env.sh
```

预期输出：

```text
必须显式设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true 才能执行 破坏性 Admin 测试。
```

即使显式打开破坏性测试，只要 endpoint 指向共享/常见本机默认环境，双分支仍失败：

```bash
MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true \
MINIO_LAB_ENDPOINT=http://127.0.0.1:9000 \
MINIO_LAB_ACCESS_KEY=dummy \
MINIO_LAB_SECRET_KEY=dummy \
MINIO_LAB_CAN_RESTORE=true \
scripts/minio-lab/verify-env.sh
```

预期输出：

```text
MINIO_LAB_ENDPOINT 不能指向共享环境或常见本机默认环境：http://127.0.0.1:9000。
```

这里使用 dummy 值只为了走到 endpoint 门禁，不连接服务端，也不使用真实凭证。

## 4. 后续要真正执行 lab 需要什么

真实破坏性矩阵必须等用户提供独立环境，并至少满足：

1. `MINIO_LAB_ENDPOINT` 不等于共享 MinIO，也不是常见本机默认端点。
2. `MINIO_LAB_ACCESS_KEY` / `MINIO_LAB_SECRET_KEY` 只通过本机未提交配置或环境变量提供。
3. `MINIO_LAB_CAN_RESTORE=true`。
4. 若启用 tier、remote target、batch、site replication 写入夹具，还必须设置 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`。
5. 执行后只提交脱敏报告，不提交 lab.properties、真实请求体、token、签名或凭证。

## 5. 发布口径影响

`destructive-blocked = 29` 不减少。阶段 51 只能证明门禁仍然安全，不能证明破坏性能力已经在真实 lab 中执行通过。
