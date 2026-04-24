# 阶段 36：破坏性实验写入夹具深化

阶段 36 的目标是把上一阶段已经能探测的 tier、remote target 能力，推进到“可写入、可验证、可恢复、可审计”的独立 lab 夹具。它不改变共享 MinIO 的默认测试策略：共享环境仍然只能跑安全 live 测试。

## 1. 新增写入夹具

| 夹具 | 必需变量 | 行为 |
| --- | --- | --- |
| tier add/edit/remove | `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`、`MINIO_LAB_TIER_WRITE_NAME`、`MINIO_LAB_ADD_TIER_BODY`、`MINIO_LAB_REMOVE_TIER_AFTER_TEST=true` | 先用 `ReactiveMinioAdminClient` 写入，再用 `ReactiveMinioRawClient` 的 `ADMIN_ADD_TIER` / `ADMIN_REMOVE_TIER` 兜底路径交叉验证，最后执行 remove 恢复。可选 `MINIO_LAB_EDIT_TIER_BODY` 会同时覆盖 typed/raw edit。 |
| remote target set/remove | `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`、`MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET`、`MINIO_LAB_SET_REMOTE_TARGET_BODY`、`MINIO_LAB_REMOVE_REMOTE_TARGET_ARN`、`MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST=true` | 先用专用 Admin 客户端设置 remote target，再用 raw catalog 的 `ADMIN_SET_REMOTE_TARGET` / `ADMIN_REMOVE_REMOTE_TARGET` 验证兜底调用，最后使用指定 ARN 删除恢复。 |

这些变量只应该写入本机私有 lab 配置文件或 CI secret 注入环境，不能提交真实请求体、凭证或真实端点。

## 2. 额外门禁

`verify-env.sh` 现在会检测写入夹具请求体或 remote target 删除 ARN。一旦发现这些危险变量，但没有 `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`，脚本会直接拒绝执行。

这样可以避免“误填了真实写入请求体，但忘记确认独立 lab”的情况。只设置资源名、不设置请求体时不会触发真实写入，也不会影响默认跳过行为。

## 3. typed 与 raw 的关系

阶段 36 继续保持 SDK 分层原则：

1. 用户日常优先使用 `ReactiveMinioAdminClient` 这类专用客户端，因为它能表达更明确的参数、错误解释和恢复语义。
2. `ReactiveMinioRawClient` 只作为兜底调用器，用来验证 catalog 路由完整性，或在 SDK 尚未封装新 API 时临时发起请求。
3. 破坏性 lab 夹具会同时覆盖 typed 与 raw，是为了证明两条路径都可用；这不代表业务代码应该优先使用 raw。

## 4. 报告字段

`write-report.sh` 生成的本机报告新增以下字段：

- 写入夹具总开关。
- tier add/edit/remove 写入 + 恢复是否启用。
- remote target set/remove 写入 + 恢复是否启用。
- tier 写入名称、remote target 写入 bucket。
- tier add/edit 请求体、remote target set 请求体是否已设置。
- remote target 删除 ARN 是否已设置。

报告只记录“已设置/未设置”和资源指纹，不输出请求体内容、access key、secret key、session token 或请求签名。

## 5. 失败恢复顺序

1. tier 夹具失败时，优先用 `MINIO_LAB_TIER_WRITE_NAME` 执行 remove tier；如果脚本已经退出，使用独立 lab 控制台或 `mc admin tier rm` 等价命令恢复。
2. remote target 夹具失败时，优先用 `MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET` 与 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` 删除刚写入的 target。
3. 如果 raw 或 typed 任一路径失败，先检查报告中的夹具开关和 MinIO 管理日志，再确认请求体是否匹配当前 MinIO 版本。
4. 不允许把共享 `http://127.0.0.1:9000` 当作恢复目标。
