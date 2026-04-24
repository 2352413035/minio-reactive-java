# 阶段 59：Admin 剩余高风险与 lab-only 产品边界

## 1. 本阶段结论

阶段 59 补齐 Admin catalog 中剩余的产品入口，使 Admin product-typed 从 113 / 128 提升到 128 / 128。这个结论表示：每个 Admin 路由都有用户可理解的专用入口、返回边界或风险说明；它不表示所有破坏性操作都可以在共享 MinIO 环境中真实执行。

## 2. 新增产品入口

| 能力域 | 新增方法 | 边界说明 |
| --- | --- | --- |
| IDP 配置 | `addIdpConfigEntry(...)`、`updateIdpConfigEntry(...)`、`deleteIdpConfigEntry(...)` | 身份源写入操作，要求明确 `type`、`name` 和必要请求体；SDK 不记录敏感字段。 |
| LDAP 服务账号 | `addLdapServiceAccountEntry(...)` | 只固定请求和文本结果边界，不保存服务端返回的敏感内容。 |
| Bucket quota | `setBucketQuotaConfig(...)` | 配置写入操作，只在独立 lab 中真实执行。 |
| Remote target | `setRemoteTargetConfig(...)`、`removeRemoteTargetConfig(...)` | 请求体可能包含远端配置或凭据，SDK 不解析、不记录。 |
| Replication diff | `runReplicationDiff(...)` | 可能消耗资源，只在维护窗口或独立 lab 中执行。 |
| Batch job | `startBatchJobRequest(...)`、`cancelBatchJobRequest(...)` | 默认使用 `application/yaml`，调用方必须保留请求体和回滚方案。 |
| Tier | `addTierConfig(...)`、`editTierConfig(...)`、`removeTierConfig(...)` | tier 写入/删除属于可恢复但高风险的环境配置能力。 |
| Site replication | `addSiteReplicationConfig(...)`、`removeSiteReplicationConfig(...)`、`editSiteReplicationConfig(...)`、`editSiteReplicationPeer(...)`、`removeSiteReplicationPeer(...)` | 站点复制变更继续固定 `api-version=1`，只在独立 lab 中真实执行。 |
| 锁维护 | `forceUnlockPaths(...)` | 强制解锁属于强破坏性维护操作，必须明确 `paths`。 |

## 3. 与 raw client 的关系

这些方法是 `ReactiveMinioAdminClient` 的产品入口，不把 raw 当作用户需要理解的主流程。测试仍会用 `ReactiveMinioRawClient` 调用相同 catalog 路由做交叉验证，目的只是证明 raw 兜底仍完整可用：当 MinIO 新增接口而 SDK 暂未封装时，用户可以先通过 raw 快速接入。

## 4. 能力矩阵变化

- Admin product-typed：113 / 128 -> 128 / 128。
- `raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 保持不变。
- `destructive-blocked = 29` 保持不变。

因此发布说明必须继续采用分层口径：路由对标完成、可调用入口完成、产品边界完成，但 Crypto Gate 和破坏性 lab 仍是单独门禁。

## 5. 验证要求

本阶段需要同时验证：

1. mock 单元测试覆盖新增专用 Admin 方法的路径、查询参数、HTTP 方法、Content-Type 和本地参数校验。
2. raw catalog 对同类路由仍能发起请求，证明兜底通路完整。
3. route parity 仍为 233 / 233。
4. capability matrix 显示 Admin product-typed 128 / 128。
5. Crypto Gate 仍为 Fail 状态，不能因为 product-typed 满格而绕过门禁。
6. 破坏性 lab 默认仍拒绝共享端点。

## 6. 后续方向

阶段 59 之后，不应再把主要工作定义为“补齐入口”。下一步应围绕三个方向继续推进：

1. **独立 lab 真实证据**：在可回滚 MinIO 环境里执行 tier、remote target、batch job、site replication、force-unlock 等写入矩阵，并保存本机报告。
2. **Crypto Gate Pass**：完成 owner/security/architect 三方批准、依赖审查和双分支测试矩阵后，才允许把 `EncryptedAdminResponse` 升级为明文模型。
3. **结果模型深化**：对已经稳定的文本或 JSON 响应继续拆成更细的中文结果模型，但不要重复新增平行入口。
