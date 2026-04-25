# 阶段 107：tier add/remove 独立 lab 矩阵补证

## 背景

阶段 106 已证明 remote target set/remove 可以在一次性 Docker MinIO lab 中完成 typed/raw 写入恢复。tier add/edit/remove 同样属于高风险 Admin 写入，且请求体需要 madmin 加密；因此本阶段继续用独立 lab 补证 tier add/remove 的最小闭环。

## 模板修正

`tier-add-minio.json.example` 已调整为更接近真实 lab 的最小示例：

- tier 名称使用大写，例如 `REACTIVE-LAB-TIER`，因为 MinIO 服务端会拒绝非大写 tier 名称。
- MinIO 类型 tier 的 `Endpoint` 是源 MinIO 服务端进程视角可访问的 URL。Docker 双容器场景下通常需要填写目标容器在 Docker 网络内的 IP 或 DNS，而不是宿主机映射端口。
- bucket 名称仍保持小写，避免违反 S3 bucket 命名规则。

## 本阶段 lab 证据

本阶段使用双容器 Docker lab：一个 MinIO 作为被测源端，一个 MinIO 作为 tier 远端目标。真实凭证和请求体只存在于 `/tmp` 私有目录，执行后已清理。

JDK8 与 JDK17+ 分支均执行：

```text
DestructiveAdminIntegrationTest#shouldWriteAndRestoreTierAndRemoteTargetOnlyInsideVerifiedLab
```

已验证 tier 子矩阵：

1. 专用 Admin 客户端使用加密 body 执行 `ADMIN_ADD_TIER`。
2. typed `listTiers` 能读取新增 tier。
3. raw `ADMIN_REMOVE_TIER` 删除专用客户端写入的 tier。
4. raw 使用显式加密 body 执行 `ADMIN_ADD_TIER`。
5. finally 中专用 Admin 客户端删除 raw 写入的 tier。
6. 恢复后再次 typed `listTiers`，确认客户端仍可读取 tier 列表。

## 仍未完成的高风险矩阵

本阶段只补证 tier add/remove 最小闭环，没有执行：

- tier edit 凭证更新。
- batch job start/status/cancel。
- site replication add/edit/remove。

`destructive-blocked` 继续保持 `29`，表示这些接口仍属于只能在独立 lab 或维护窗口执行的破坏性能力，而不是共享环境可随意运行的普通接口。
