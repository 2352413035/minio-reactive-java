# 阶段 82：mc 只读运维旁证刷新

## 1. 本阶段目标

阶段 82 使用系统已安装的 `mc` 做共享 MinIO 的只读旁证刷新。该阶段只读取健康、桶列表摘要和 Admin info 摘要，不写对象、不改配置、不执行破坏性 Admin 操作。

本阶段使用临时 `mc` 配置目录，凭证只在运行时注入，不写入仓库，也不写入报告。

## 2. 执行方式

执行时使用临时配置目录：

```bash
mc -C /tmp/stage82-mc-config alias set stage82 <endpoint> <access-key> <secret-key>
mc -C /tmp/stage82-mc-config ready stage82
mc -C /tmp/stage82-mc-config ls --json stage82
mc -C /tmp/stage82-mc-config admin info --json stage82
```

执行后删除 `/tmp/stage82-mc-config`，避免把运行时凭证留在默认 `~/.mc` 配置里。

## 3. 本次只读证据摘要

| 项目 | 结果 |
| --- | --- |
| ready | 通过，集群 ready。 |
| mode | online。 |
| region | us-east-1。 |
| bucket count | 1。 |
| object count | 382。 |
| server count | 1。 |
| online disks | 1。 |
| offline disks | 0。 |

以上证据只说明共享 MinIO 当前可读、在线、可用于安全 live smoke。它不代表破坏性 Admin 写入已通过，也不减少 `destructive-blocked = 29`。

## 4. 凭据与敏感信息边界

- 文档不记录 access key、secret key、页面登录密码、token 或签名。
- 报告只记录聚合状态，不记录完整 `admin info` JSON。
- 只读命令不写入 bucket、对象、用户、策略或配置。
- 临时 `mc` 配置目录执行后删除。

## 5. 后续使用建议

后续只读旁证可以继续补充：

- `mc ready`。
- `mc ls --json` 的 bucket 数量摘要。
- `mc admin info --json` 的 online/offline、region、磁盘数量摘要。
- SDK live 测试的安全对象存储 smoke。

任何写入、删除、配置变更、tier/remote target/batch/site replication 操作，都必须回到独立 lab 门禁。
