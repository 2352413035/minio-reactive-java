# 阶段 113：Crypto Pass 后发布就绪与破坏性边界再审计

## 当前结论

阶段 111 / 112 后，SDK 对标 `minio-java` 的主要技术缺口已经进一步收口：

- minio-java 主体 API 对标完成：对象存储核心 API `59 / 59`，Admin 核心 API `24 / 24`。
- `*Args` builder 对标完成：`86 / 86`。
- 服务端 route catalog 对照完成：`233 / 233`。
- `raw-fallback = 0`。
- Crypto Gate 已 Pass：`encrypted-blocked = 0`。
- 剩余 `destructive-blocked = 29` 是高风险操作分类计数，不是“缺 29 个接口”。

正式 Maven/tag 发布仍未完成，因为 POM 发布元数据、签名、SBOM、发布仓库和维护窗口策略需要负责人确认，不能由 SDK 代码自动伪造。

## 发布元数据预检

本阶段重新运行：

```bash
python3 minio-reactive-java/scripts/report-pom-release-metadata.py \
  --worktree minio-reactive-java \
  --worktree minio-reactive-java-jdk17 \
  --format markdown \
  --output .omx/reports/pom-release-metadata-stage113.md
```

双分支基础字段齐全：`groupId`、`artifactId`、`version`、`name`、`description`。

仍缺发布负责人材料：

| 类别 | 缺失项 | 处理方式 |
| --- | --- | --- |
| POM 元数据 | `url`、`licenses`、`scm`、`scm.url`、`scm.connection`、`developers`、`issueManagement`、`organization`、`distributionManagement` | 需要发布负责人确认公开仓库、许可证、组织身份和发布仓库。 |
| 发布插件 | `maven-source-plugin`、`maven-javadoc-plugin`、`maven-gpg-plugin`、`cyclonedx-maven-plugin` | 需要确认签名密钥、SBOM 策略和中央仓库发布流程。 |

当前不能直接把版本从 `0.1.0-SNAPSHOT` 改成正式版，也不能打 tag 或发布 Maven。

## `destructive-blocked = 29` 分类

### 已有独立 Docker lab typed/raw 证据的可回滚路径

| 路由 | 当前状态 | 证据来源 |
| --- | --- | --- |
| `ADMIN_SET_CONFIG_KV` | 已在独立 lab 做 typed/raw 写入与恢复 | 阶段 84 / 104 |
| `ADMIN_SET_BUCKET_QUOTA` | 已在独立 lab 做 typed/raw 写入与恢复 | 阶段 84 / 104 |
| `ADMIN_SET_REMOTE_TARGET` | 已在双容器 lab 做 typed/raw set/remove | 阶段 106 |
| `ADMIN_REMOVE_REMOTE_TARGET` | 已在双容器 lab 做 typed/raw set/remove | 阶段 106 |
| `ADMIN_START_BATCH_JOB` | 已在双容器 lab 做 typed/raw start/status/cancel | 阶段 108 |
| `ADMIN_CANCEL_BATCH_JOB` | 已在双容器 lab 做 typed/raw start/status/cancel | 阶段 108 |
| `ADMIN_ADD_TIER` | 已在双容器 lab 做 typed/raw add/remove | 阶段 107 |
| `ADMIN_EDIT_TIER` | 已在双容器 lab 做 typed/raw edit/remove | 阶段 110 |
| `ADMIN_REMOVE_TIER` | 已在双容器 lab 做 typed/raw add/edit/remove | 阶段 107 / 110 |
| `ADMIN_SITE_REPLICATION_ADD` | 已在双容器 lab 做 typed/raw add/remove | 阶段 109 |
| `ADMIN_SITE_REPLICATION_REMOVE` | 已在双容器 lab 做 typed/raw add/remove | 阶段 109 |

这些接口已经证明 SDK typed 与 raw 两条路径可用，但仍属于高风险 Admin 操作，不能在共享 MinIO 默认执行。

### 有产品入口但仍需专门拓扑或维护窗口的路径

| 路由 | 保留原因 |
| --- | --- |
| `ADMIN_SET_CONFIG` | 全量配置替换风险高，阶段 104 只对 config KV 做可回滚证据；全量配置需要专门恢复策略。 |
| `ADMIN_ADD_IDP_CONFIG` | 可能改动认证入口，需要独立身份提供方夹具和恢复策略。 |
| `ADMIN_UPDATE_IDP_CONFIG` | 可能影响登录链路，需要独立身份提供方夹具和恢复策略。 |
| `ADMIN_DELETE_IDP_CONFIG` | 可能删除认证入口，需要明确恢复材料。 |
| `ADMIN_REPLICATION_DIFF` | 可能消耗大量资源，应在维护窗口或专门复制拓扑执行。 |
| `ADMIN_SITE_REPLICATION_EDIT` | 真实 endpoint、deploymentID、带宽/同步策略变更需要更复杂拓扑。 |
| `ADMIN_SR_PEER_EDIT` | peer 级变更需要真实多站点部署与恢复计划。 |
| `ADMIN_SR_PEER_REMOVE` | peer 删除会影响站点复制拓扑，需要维护窗口。 |
| `ADMIN_SERVICE` | restart/stop 等服务控制操作会影响可用性。 |
| `ADMIN_SERVICE_V2` | restart/stop 等服务控制操作会影响可用性。 |
| `ADMIN_SERVER_UPDATE` | 服务端升级操作需要版本、回滚和维护窗口。 |
| `ADMIN_SERVER_UPDATE_V2` | 服务端升级操作需要版本、回滚和维护窗口。 |
| `ADMIN_FORCE_UNLOCK` | 强制解锁可能破坏正在进行的写入或锁语义。 |
| `ADMIN_SPEEDTEST` | 压测可能消耗集群资源。 |
| `ADMIN_SPEEDTEST_OBJECT` | 压测可能消耗对象存储资源。 |
| `ADMIN_SPEEDTEST_DRIVE` | 压测可能消耗磁盘资源。 |
| `ADMIN_SPEEDTEST_NET` | 压测可能消耗网络资源。 |
| `ADMIN_SPEEDTEST_SITE` | 压测可能影响站点复制链路。 |

这些不是 SDK 接口缺口，而是运维风险边界。后续若要减少 `destructive-blocked`，必须给出独立 lab 或维护窗口报告，而不是只增加代码。

## 后续可继续推进的事项

1. 发布负责人确认许可证、SCM、developer、organization、issueManagement、distributionManagement。
2. 发布负责人确认 source/javadoc/sign/SBOM 插件方案和签名密钥策略。
3. 如果要继续降低 `destructive-blocked`，优先做可回滚且不影响服务可用性的 `ADMIN_SET_CONFIG` 全量配置恢复实验；IDP、service/update、force-unlock、speedtest 仍应留给专门维护窗口。
4. 继续深化加密 Admin 响应的明文业务模型，但必须保留 `*Encrypted` 安全兜底。

## 本阶段判断

当前 SDK 已经达到“功能全面、响应式、与 minio-java 主体 API 对齐、Crypto Gate Pass、raw 兜底完整”的发布候选状态。距离正式发布还差的主要不是 SDK 代码接口，而是：

- 发布工程负责人材料。
- 剩余高风险 Admin 操作的运维/lab 证据。
- 正式版本号、tag、Maven 发布与回滚策略。
