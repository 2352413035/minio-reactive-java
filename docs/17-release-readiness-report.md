# 17 阶段 19 发布就绪报告

## 1. 结论

阶段 19 的目标不是宣称“所有 MinIO 能力都已经产品化”，而是给出可信、可复查、可继续推进的发布口径。当前结论如下：

- 路由目录已经对齐本地 `minio` 公开 router：JDK8 分支与 JDK17+ 分支均为 233 / 233，catalog 缺失 0、额外 0。
- 所有 catalog 路由都有 typed 或 advanced 兼容入口，能力矩阵里的 `raw-fallback` 为 0。
- SDK 的用户友好 typed 成熟度仍在继续推进，不能用 route parity 代替产品成熟度。
- Admin 加密响应和破坏性操作已经明确建模为风险边界，不会在共享环境中伪装成普通成功能力。

## 2. 当前能力矩阵

当前机器报告来自 `.omx/reports/capability-matrix.md`：

| family | route-catalog | product-typed | advanced-compatible | raw-fallback | encrypted-blocked | destructive-blocked |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| s3 | 77 | 52 | 77 | 0 | 0 | 0 |
| admin | 128 | 33 | 128 | 0 | 9 | 29 |
| kms | 7 | 6 | 7 | 0 | 0 | 0 |
| sts | 7 | 4 | 7 | 0 | 0 | 0 |
| metrics | 6 | 5 | 6 | 0 | 0 | 0 |
| health | 8 | 8 | 0 | 0 | 0 | 0 |

这张表的解释：

- `route-catalog` 表示 MinIO 公开 HTTP 路由已经登记到 SDK catalog。
- `product-typed` 表示有更适合用户直接集成的请求/响应模型、错误说明或风险边界。
- `advanced-compatible` 表示 SDK 保留了可调用入口，但还不一定是最终产品化模型。
- `raw-fallback` 表示只有 raw 能调用、没有专用入口的路由；当前为 0。
- `encrypted-blocked` 表示服务端默认返回 madmin 加密载荷，当前只暴露边界对象。
- `destructive-blocked` 表示需要独立实验环境验证，不能在共享 MinIO 默认执行。

## 3. 对外推荐入口

| 场景 | 推荐入口 | 说明 |
| --- | --- | --- |
| 对象存储 | `ReactiveMinioClient` | 上传、下载、列对象、版本列表、分片上传等主路径。 |
| 管理端 | `ReactiveMinioAdminClient` | 优先使用 L1/L2 typed 方法；L3/L4 按风险门禁执行。 |
| KMS | `ReactiveMinioKmsClient` | key 状态和生命周期接口。 |
| STS | `ReactiveMinioStsClient` | 普通 AssumeRole、WebIdentity、ClientGrants、LDAP typed 凭证解析。 |
| Metrics | `ReactiveMinioMetricsClient` | Prometheus 文本包装和样本解析。 |
| Health | `ReactiveMinioHealthClient` | `isLive()` / `isReady()` 等布尔探针和状态码入口。 |
| 新增或特殊接口 | `ReactiveMinioRawClient` | 兜底调用，不是普通业务主路径。 |

## 4. 当前风险边界

### 4.1 madmin 加密响应

以下接口仍属于 `encrypted-blocked`：

- `ADMIN_GET_CONFIG`
- `ADMIN_GET_CONFIG_KV`
- `ADMIN_LIST_CONFIG_HISTORY_KV`
- `ADMIN_LIST_USERS`
- `ADMIN_ADD_SERVICE_ACCOUNT`
- `ADMIN_INFO_SERVICE_ACCOUNT`
- `ADMIN_LIST_SERVICE_ACCOUNTS`
- `ADMIN_INFO_ACCESS_KEY`
- `ADMIN_LIST_ACCESS_KEYS_BULK`

SDK 会返回 `EncryptedAdminResponse`，并通过 `algorithm()` / `algorithmName()` 暴露诊断信息。只有 Crypto Gate Pass 后，才能把这些响应升级为明文 typed 模型。

### 4.2 destructive Admin

配置写入、站点复制、tier、批处理、远端 target、service restart/update 等高风险接口不能在共享 MinIO 默认验证。必须通过：

1. `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true`
2. `scripts/minio-lab/verify-env.sh`
3. `scripts/minio-lab/run-destructive-tests.sh`
4. 独立可回滚 MinIO 环境
5. 必要时提供 `MINIO_LAB_TEST_CONFIG_KV` 与 `MINIO_LAB_RESTORE_CONFIG_KV`

## 5. 本阶段补齐内容

- 更新 README 的能力口径、入口矩阵、示例列表和完成度说明。
- 更新 `docs/09-minio-api-catalog.md`，把两层 API 说明修正为领域 typed、专用 typed、raw 兜底三类入口。
- 更新 `docs/13-admin-risk-levels.md`、`docs/14-typed-client-usage-guide.md`、`docs/release-gates.md`，保持 Admin 风险、Crypto 边界、发布门禁口径一致。
- 修正 `ReactiveMinioTypedAdminExample`，不再示范会修改共享环境的用户组写操作，也不再调用已明确为加密边界的明文 access-key typed 方法。
- 新增 `ReactiveMinioRawFallbackExample` 和 `ReactiveMinioOpsExample`，分别展示 raw 兜底和运维客户端。

## 6. 验证命令

阶段 19 发布就绪至少应重新执行以下命令，并把输出作为最终证据：

```bash
cd /dxl/minio-project/minio-reactive-java
mvn -q -DfailIfNoTests=true test
MINIO_ENDPOINT=... MINIO_ACCESS_KEY=... MINIO_SECRET_KEY=... mvn -q -Dtest=LiveMinioIntegrationTest test
python3 scripts/report-route-parity.py --minio-root /dxl/minio-project/minio --worktree /dxl/minio-project/minio-reactive-java --format markdown --output /dxl/minio-project/.omx/reports/route-parity-jdk8.md
python3 scripts/report-capability-matrix.py --worktree /dxl/minio-project/minio-reactive-java --worktree /dxl/minio-project/minio-reactive-java-jdk17 --format markdown --output /dxl/minio-project/.omx/reports/capability-matrix.md
scripts/madmin-fixtures/check-crypto-gate.sh

git diff --check
```

JDK17+ 分支还需要额外执行 JDK21/JDK25 compile，确保高版本分支没有语言或依赖退化。

## 7. 下一阶段方向

阶段 20 之后应继续提升 `product-typed`，优先顺序为：

1. S3 剩余高频 typed 子资源：object attributes、retention、legal hold、restore、select、bucket 子资源的模型化。
2. Admin L1/L2 继续 typed 化：service info、policy attachment、bucket quota、tier list、scanner/heal 只读摘要。
3. STS/KMS/Metrics 低风险缺口补齐。
4. destructive lab 扩展更多可回滚真实验证。
5. Crypto Gate 的依赖和安全设计评审。
