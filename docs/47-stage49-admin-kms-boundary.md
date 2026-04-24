# 阶段 49：Admin KMS 与专用 KMS 客户端边界

## 1. 本阶段结论

KMS 的普通业务入口仍然优先使用 `ReactiveMinioKmsClient`。它覆盖 `/minio/kms/v1` 下 7 个 KMS 路由，包括状态、版本、API 列表、指标、key 创建、key 列表和 key 状态。

MinIO 同时在 Admin 路由下保留了一组 madmin 兼容 KMS 路径：

- `ADMIN_KMS_STATUS`
- `ADMIN_KMS_KEY_CREATE`
- `ADMIN_KMS_KEY_STATUS`

阶段 49 不把这两组路径混在一起，而是明确分工：

1. 普通 KMS 使用 `ReactiveMinioKmsClient`。
2. 需要对照 madmin/Admin 路由时，使用 `ReactiveMinioAdminClient` 的 Admin KMS 桥接方法。
3. 历史 `kmsStatus()` / `kmsKeyCreate(...)` / `kmsKeyStatus()` 字符串入口保留但标记为 `@Deprecated`，迁移到强类型桥接方法。

## 2. 新增 Admin KMS 桥接方法

| 方法 | 返回 | 用途 |
| --- | --- | --- |
| `getAdminKmsStatus()` | `KmsJsonResult` | 通过 Admin 路由读取 KMS 状态。 |
| `createAdminKmsKey(String keyId)` | `Mono<Void>` | 通过 Admin 路由创建 KMS key。 |
| `getAdminKmsKeyStatus()` | `KmsKeyStatus` | 通过 Admin 路由检查默认 KMS key。 |
| `getAdminKmsKeyStatus(String keyId)` | `KmsKeyStatus` | 通过 Admin 路由检查指定 KMS key。 |

这些方法存在的原因不是替代 `ReactiveMinioKmsClient`，而是给已经围绕 Admin 客户端组织权限、审计或 madmin 兼容路径的调用方一个清晰 typed 入口。

## 3. 推荐选择

| 场景 | 推荐入口 |
| --- | --- |
| 常规 KMS 状态、版本、API、指标、key 列表 | `ReactiveMinioKmsClient` |
| 常规 key 创建、key 状态检查 | `ReactiveMinioKmsClient#createKey(...)` / `getKeyStatus(...)` |
| 必须走 `/minio/admin/v3/kms/...` 路径对齐 madmin 行为 | `ReactiveMinioAdminClient#getAdminKmsStatus()` 等桥接方法 |
| SDK 尚未支持的新参数或排障 | `ReactiveMinioRawClient` |

## 4. 与 advanced 入口的迁移关系

以下旧入口继续保留二进制兼容，但不再作为推荐业务入口：

- `kmsStatus()` / `kmsStatus(byte[], String)`
- `kmsKeyCreate(String)` / `kmsKeyCreate(String, byte[], String)`
- `kmsKeyStatus()`

它们返回 `Mono<String>`，调用方需要自行解析 JSON。新桥接方法直接返回 `KmsJsonResult` 或 `KmsKeyStatus`，并保留 raw JSON 字段。

## 5. 能力矩阵影响

Admin product-typed 从 75 / 128 提升到 78 / 128。KMS family 仍为 7 / 7，不重复计数 Admin 路由。

## 6. 验证命令

```bash
mvn -q -Dtest=ReactiveMinioSpecializedClientsTest test
python3 scripts/report-capability-matrix.py --worktree . --format markdown --output ../.omx/reports/capability-matrix-stage49-jdk8.md
git diff --check
```

JDK17+ 分支同步后继续执行 JDK17 全量测试、真实 MinIO smoke、JDK21/JDK25 test-compile、Crypto Gate 和 secret scan。
