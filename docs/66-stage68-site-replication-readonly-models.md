# 阶段 68：站点复制只读摘要模型深化

## 1. 本阶段目标

阶段 68 继续做非破坏性成熟度提升，聚焦站点复制的只读查询入口：`info`、`status`、`metainfo`。这些入口只读取站点复制配置、状态和元信息，不执行 add/remove/edit/resync 等写入操作，因此不需要独立破坏性 lab，也不能降低 `destructive-blocked` 计数。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag。

## 2. 新增模型与方法

| 模型 | 新方法 | 保留的通用方法 | 提取字段 |
| --- | --- | --- | --- |
| `AdminSiteReplicationInfoSummary` | `getSiteReplicationInfoSummary()` | `getSiteReplicationInfo()` | 是否启用、站点名称、站点数量、是否存在服务账号 access key、apiVersion。 |
| `AdminSiteReplicationStatusSummary` | `getSiteReplicationStatusSummary()` | `getSiteReplicationStatus()` | 是否启用、站点数量、最大 bucket/user/group/policy 数、各类 mismatch 明细数量、metrics 是否存在、apiVersion。 |
| `AdminSiteReplicationMetaInfoSummary` | `getSiteReplicationMetainfoSummary()` | `getSiteReplicationMetainfo()` | 是否启用、站点名称、deploymentID、bucket/policy/user/group/replication/ILM expiry 计数、apiVersion。 |

这些模型都继承 `AdminJsonResult`，继续保留 `rawJson()` 和 `values()`。站点复制响应结构可能随 MinIO 版本变化，摘要模型只提取稳定字段，不替代完整 JSON。

## 3. 安全边界

1. `getSiteReplicationInfoSummary()` 只返回 `serviceAccountAccessKeyPresent()`，不暴露服务账号 access key 内容。
2. 本阶段不触碰 `addSiteReplicationConfig()`、`removeSiteReplicationConfig()`、`editSiteReplicationConfig()`、peer join/remove/edit 或 resync 写入口。
3. 站点复制写入口仍属于 lab-only 或维护窗口能力，必须走独立可回滚环境。
4. 摘要模型只用于只读看板和排障提示，不能作为“站点复制写入能力已真实放行”的证据。

## 4. 使用示例

```java
AdminSiteReplicationInfoSummary info = admin.getSiteReplicationInfoSummary().block();
boolean enabled = info.enabled();
int sites = info.siteCount();

AdminSiteReplicationStatusSummary status = admin.getSiteReplicationStatusSummary().block();
int bucketMismatches = status.bucketStatsEntryCount();
boolean hasMetrics = status.metricsPresent();

AdminSiteReplicationMetaInfoSummary meta = admin.getSiteReplicationMetainfoSummary().block();
int bucketCount = meta.bucketCount();
int userCount = meta.userCount();
```

如果调用方需要完整站点、策略、bucket 或 IAM 明细，应继续读取：

```java
String rawStatus = admin.getSiteReplicationStatusSummary().block().rawJson();
Map<String, Object> fullMeta = admin.getSiteReplicationMetainfo().block().values();
```

## 5. 验证口径

阶段 68 至少需要验证：

- 新模型能解析 madmin-go 的 `SiteReplicationInfo`、`SRStatusInfo`、`SRInfo` 常见字段。
- 新方法命中原有 `site-replication/info`、`site-replication/status`、`site-replication/metainfo` 路由。
- 原通用 JSON 方法继续可用。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭证扫描继续通过。
