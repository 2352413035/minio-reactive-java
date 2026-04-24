# 阶段 42：site replication peer IDP 安全摘要

阶段 42 的目标是继续提升 Admin 站点复制相关接口的产品化程度，但不突破破坏性实验环境边界。`site-replication/peer/idp-settings` 是 GET 只读接口，适合进入强类型客户端；peer join、peer edit、peer remove、resync、bucket ops、IAM item 和 bucket meta 仍然可能改变站点复制拓扑或 peer 状态，继续留在 advanced / lab 路径。

## 1. 新增产品入口

| 方法 | 返回模型 | 说明 |
| --- | --- | --- |
| `getSiteReplicationPeerIdpSettings()` | `AdminSiteReplicationPeerIdpSettings` | 读取 peer IDP 设置摘要，便于判断 LDAP/OpenID 配置是否存在。 |

## 2. 敏感字段边界

MinIO 的 IDP 设置响应来自 madmin-go 的 `IDPSettings`，其中 OpenID provider 可能包含 `ClientID`、`HashedClientSecret` 等字段。即使这些字段不是明文密码，也不应该默认进入业务日志或普通摘要对象。

因此 `AdminSiteReplicationPeerIdpSettings` 故意不继承 `AdminJsonResult`，也不提供 `rawJson()`：

- 暴露 LDAP 是否启用、LDAP 用户/用户组搜索 base/filter。
- 暴露 OpenID 是否启用、区域、角色名称列表和角色数量。
- 暴露 claim provider 是否存在。
- 不暴露客户端标识、哈希密钥、完整 provider 结构和原始 JSON。

如果调用方确实需要完整原始响应，可以明确使用 advanced 方法 `srPeerIdpSettings()` 或 `ReactiveMinioRawClient`，并自行承担日志脱敏和访问控制责任。使用 raw 路径时建议显式传入 `api-version=1` 查询参数，和 madmin-go 的请求保持一致。

## 3. 协议版本修正

对照 madmin-go，site replication 主要接口会携带 `api-version=1` 查询参数。阶段 42 已在专用客户端调用中补齐该值，覆盖：

- site replication add/remove/info/metainfo/status/edit/resync。
- peer join/bucket-ops/iam-item/bucket-meta/idp-settings/edit/remove。
- state edit。

`devnull` 与 `netperf` 是独立诊断路径，保留原有请求形态。

## 4. 能力矩阵变化

阶段 42 后，Admin product-typed 从 63 / 128 提升到 64 / 128。`encrypted-blocked = 9`、`destructive-blocked = 29` 不变。

这代表新增一个用户友好的只读产品入口，不代表 site replication 的写入类接口已经可以在共享环境随意执行。写入能力仍必须通过独立 lab 夹具和 typed/raw 双路径报告证明。
