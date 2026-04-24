# 阶段 60：发布候选最终复审

## 1. 复审结论

阶段 60 的结论是：当前 SDK 已经完成“对标 MinIO 公开 HTTP 路由”的发布候选闭环。这里的“闭环”包含四层含义：

1. **路由目录闭环**：`MinioApiCatalog` 对照本地 `minio` 公开 router，继续保持 233 / 233，无缺失、无额外 catalog。
2. **调用入口闭环**：能力矩阵 `raw-fallback = 0`，每个 catalog 路由都有专用、typed 或 advanced 兼容入口。
3. **产品边界闭环**：S3、Admin、KMS、STS、Metrics、Health 的 product-typed 计数都已达到对应 route-catalog 数量。
4. **风险门禁闭环**：Crypto Gate 与破坏性 lab 继续单独标记，不把未批准的解密能力或共享环境禁止执行的写入操作伪装成普通成功路径。

因此，当前适合继续作为 `0.1.0-SNAPSHOT` 发布候选验证状态；还不应切到正式 1.0 或移除风险门禁。

## 2. 当前机器证据

本阶段重新生成的报告位于 `.omx/reports/`：

| 报告 | 结论 |
| --- | --- |
| `route-parity-jdk8.md` | 服务端路由 233，SDK catalog 233，缺失 0，额外 0。 |
| `route-parity-jdk17.md` | 服务端路由 233，SDK catalog 233，缺失 0，额外 0。 |
| `capability-matrix-jdk8.md` | Admin product-typed 128 / 128，raw-fallback 0。 |
| `capability-matrix-jdk17.md` | Admin product-typed 128 / 128，raw-fallback 0。 |

能力矩阵摘要：

| family | route-catalog | product-typed | advanced-compatible | raw-fallback | encrypted-blocked | destructive-blocked |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| s3 | 77 | 77 | 77 | 0 | 0 | 0 |
| admin | 128 | 128 | 128 | 0 | 9 | 29 |
| kms | 7 | 7 | 7 | 0 | 0 | 0 |
| sts | 7 | 7 | 7 | 0 | 0 | 0 |
| metrics | 6 | 6 | 6 | 0 | 0 | 0 |
| health | 8 | 8 | 0 | 0 | 0 | 0 |

## 3. 仍然不能夸大的部分

虽然 product-typed 已经满格，但以下边界仍然必须保留：

- `encrypted-blocked = 9`：默认 madmin 加密响应还没有 Crypto Gate Pass，当前只能暴露 `EncryptedAdminResponse` 边界对象。
- `destructive-blocked = 29`：配置写入、站点复制、tier、batch、remote target、force-unlock 等能力仍需要独立可回滚 lab。
- shared live MinIO 只能用于对象存储、安全只读和 smoke 验证，不能作为破坏性写入通过的证据。
- raw client 是兜底调用器，不是普通业务主路径；用户应优先使用平级专用客户端。

## 4. 本阶段验证

阶段 60 重新执行并读取了以下验证结果：

- JDK8：`mvn -q -DfailIfNoTests=true test` 通过。
- JDK17+：`mvn -q -DfailIfNoTests=true test` 通过。
- JDK8/JDK17+：`LiveMinioIntegrationTest` 在共享 MinIO 上通过。
- JDK17+：JDK21 与 JDK25 `mvn -q -DskipTests test-compile` 通过；JDK25 只有 Maven/Guava 的 `sun.misc.Unsafe` 警告。
- 双分支：Crypto Gate Fail 复核通过，没有新增默认响应解密依赖。
- 双分支：`scripts/minio-lab/verify-env.sh` 对无配置和共享端点拒绝通过。
- 双分支：shell 脚本 `bash -n` 通过。
- 双分支：`git diff --check` 与真实凭证扫描通过。

## 5. 后续阶段建议

阶段 61 不应再继续重复“补接口入口”。后续只剩三类有价值工作：

1. **独立 lab 实证**：配置私有 MinIO lab，真实执行破坏性写入矩阵，并保存不含凭证的报告。
2. **Crypto Gate Pass**：在 owner/security/architect 三方批准、依赖审查、双分支测试矩阵齐全后，再实现默认 madmin 加密响应解密。
3. **结果模型深化**：在 128 / 128 产品边界上，把稳定文本或 JSON 响应继续拆成更细的中文结果模型，而不是新增重复方法。

如果没有独立 lab 或 Crypto Gate 批准，下一步应进入发布说明和使用指南整理，而不是降低 blocked 计数。
