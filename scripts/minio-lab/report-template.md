# MinIO 破坏性实验环境执行报告模板

- 生成时间（UTC）：`<自动填充>`
- 执行结果：`<通过|失败>`
- 退出码：`<自动填充>`
- 说明：`<自动填充>`
- Lab 端点：`<只记录协议、主机和端口；不记录用户信息、路径、查询参数>`
- Java Home：`<自动填充>`
- Maven 测试：`DestructiveAdminIntegrationTest`
- 步骤状态文件：`<自动填充；只记录 typed/raw 步骤名称与结果，不记录请求体或凭证>`

## 夹具开关

- config KV 写入 + 恢复：`<启用|跳过>`
- bucket quota 写入 + 恢复：`<启用|跳过>`
- tier typed/raw 探测：`<启用|跳过>`
- remote target typed/raw 探测：`<启用|跳过>`
- batch job typed/raw 探测：`<启用|跳过>`
- tier add/edit/remove 写入 + 恢复：`<启用|跳过>`
- remote target set/remove 写入 + 恢复：`<启用|跳过>`
- batch job start/status/cancel 实验矩阵：`<启用|跳过>`
- site replication add/edit/remove 实验矩阵：`<启用|跳过>`

## 夹具指纹（不含凭证）

- 配置文件：`<路径或未使用>`
- bucket：`<资源名或未设置>`
- tier 名称：`<资源名或未设置>`
- tier 写入名称：`<资源名或未设置>`
- tier add 请求体：`<已设置|未设置>`
- tier edit 请求体：`<已设置|未设置>`
- remote target 类型：`<replication|其他>`
- remote target 写入 bucket：`<资源名或未设置>`
- remote target set 请求体：`<已设置|未设置>`
- remote target 预期 ARN：`<已设置|未设置>`
- remote target 删除 ARN：`<已设置|未设置，可选兜底>`
- batch job start 请求体：`<已设置|未设置>`
- batch job cancel 旧式请求体：`<已设置|未设置>`（当前 SDK 不要求）
- site replication add 请求体：`<已设置|未设置>`
- site replication edit 请求体：`<已设置|未设置>`
- site replication remove 请求体：`<已设置|未设置>`
- 写入夹具总开关：`<true|false>`
- batch job 预期 ID：`<已设置|未设置>`

## typed/raw 执行明细

| 范围 | 步骤 | 结果 | 说明 |
| --- | --- | --- | --- |
| `<自动填充>` | `<typed 或 raw 步骤名>` | `<PASS|FAIL>` | `<不含凭证的说明>` |

如果测试在门禁前退出，或没有执行可选写入矩阵，此处会写“未记录”。

## mc 恢复/核验提示

- 如果系统安装了 `mc`，优先使用本机私有 `MINIO_LAB_MC_ALIAS` 做只读核验。
- 推荐命令包括 `mc alias list`、`mc admin info <alias>`、`mc admin tier ls <alias>`、`mc admin config get <alias> <subsys>`。
- 不要把 `mc alias set` 命令、access key、secret key、token 或签名写入仓库。

## 失败恢复提示

1. 如果 config KV 用例失败，使用 `MINIO_LAB_RESTORE_CONFIG_KV` 对应值恢复服务配置。
2. 如果 bucket quota 用例失败，使用 `MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON` 对应值恢复 bucket quota。
3. 如果 tier 写入夹具失败，优先执行 `MINIO_LAB_REMOVE_TIER_AFTER_TEST=true` 对应的 tier 删除恢复。
4. 如果 remote target 写入夹具失败，优先使用 set 响应返回的 ARN 删除刚写入的 target；响应不可解析时再使用 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` 兜底。
5. 如果 batch job 实验矩阵失败，优先使用 start 响应中的 jobId 执行 `cancelBatchJob(jobId)` 或 `ADMIN_CANCEL_BATCH_JOB?id=<jobId>`；旧式 cancel 请求体仅作人工排错参考。
6. 如果 site replication 实验矩阵失败，优先使用 `MINIO_LAB_SITE_REPLICATION_REMOVE_BODY` 或对应文件移除刚新增的站点复制配置。
7. 如果 remote target、tier 或 batch job 探测失败，先查看 MinIO 管理日志，再用独立 lab 的控制台或 `mc admin` 回滚。
8. 不要把本报告复制到仓库；报告可能包含 lab 端点和资源名称，但不会包含凭证。
