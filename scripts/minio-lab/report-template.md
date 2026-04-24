# MinIO 破坏性实验环境执行报告模板

- 生成时间（UTC）：`<自动填充>`
- 执行结果：`<通过|失败>`
- 退出码：`<自动填充>`
- 说明：`<自动填充>`
- Lab 端点：`<只记录协议、主机和端口；不记录用户信息、路径、查询参数>`
- Java Home：`<自动填充>`
- Maven 测试：`DestructiveAdminIntegrationTest`

## 夹具开关

- config KV 写入 + 恢复：`<启用|跳过>`
- bucket quota 写入 + 恢复：`<启用|跳过>`
- tier typed/raw 探测：`<启用|跳过>`
- remote target typed/raw 探测：`<启用|跳过>`
- batch job typed/raw 探测：`<启用|跳过>`

## 夹具指纹（不含凭证）

- 配置文件：`<路径或未使用>`
- bucket：`<资源名或未设置>`
- tier 名称：`<资源名或未设置>`
- remote target 类型：`<replication|其他>`
- remote target 预期 ARN：`<已设置|未设置>`
- batch job 预期 ID：`<已设置|未设置>`

## 失败恢复提示

1. 如果 config KV 用例失败，使用 `MINIO_LAB_RESTORE_CONFIG_KV` 对应值恢复服务配置。
2. 如果 bucket quota 用例失败，使用 `MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON` 对应值恢复 bucket quota。
3. 如果 remote target、tier 或 batch job 探测失败，先查看 MinIO 管理日志，再用独立 lab 的控制台或 `mc admin` 回滚。
4. 不要把本报告复制到仓库；报告可能包含 lab 端点 和资源名称，但不会包含凭证。
