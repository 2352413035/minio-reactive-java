# 阶段 126：剩余运维接口 Docker lab 复核

## 背景

用户明确：剩余运维类接口可以继续尝试验证；Docker 环境可以自行搭建。若某些接口需要集群、多集群或维护窗口而无法充分验证，不再反复纠缠，但必须把真实结果和去留风险说清楚。同时，用户最常用的桶、文件/对象管理和高频只读 Admin 操作必须优先保持稳定。

## 本阶段新增能力

1. `DestructiveAdminIntegrationTest` 增加：
   - `ADMIN_SERVICE` / `ADMIN_SERVICE_V2` 的 typed/raw restart 探测，每次重启后等待 `/minio/health/ready`。
   - `ADMIN_SERVER_UPDATE` / `ADMIN_SERVER_UPDATE_V2` 的 typed/raw 前置条件失败探测，记录官方 Docker 镜像拒绝原地升级的结果，不伪造成升级成功。
   - `ADMIN_FORCE_UNLOCK` 的 typed/raw 单节点预期失败与分布式真实通过两种路径。
   - site replication add 后的 `ADMIN_SPEEDTEST_SITE` typed/raw 探测。
   - `ADMIN_SITE_REPLICATION_EDIT` 从 `site-replication/info` 读取真实 `deploymentID` 后自动生成 edit 请求体，覆盖 typed/raw。
2. 新增可复现脚本：
   - `scripts/minio-lab/run-maintenance-boundary-lab.sh`
   - `scripts/minio-lab/run-distributed-maintenance-lab.sh`
   - `scripts/minio-lab/run-site-replication-lab.sh`（默认跳过已知脆弱的 raw re-add 回放）
3. 修复 `expectLabFailure`：如果调用意外成功，测试会真正失败，不再被自己的 `AssertionFailedError` 误记为预期失败。

## 重新分类结果

阶段 126 后，破坏性边界报告从“已有独立 lab 证据 19 / 29”推进到：

- 已有独立 lab 证据：25
- 拓扑或身份提供方：2
- 维护窗口：2

新增独立 lab 证据：

- `ADMIN_SERVICE`
- `ADMIN_SERVICE_V2`
- `ADMIN_FORCE_UNLOCK`
- `ADMIN_SPEEDTEST_NET`
- `ADMIN_SPEEDTEST_SITE`
- `ADMIN_SITE_REPLICATION_EDIT`

仍未充分放行：

- `ADMIN_SERVER_UPDATE`
- `ADMIN_SERVER_UPDATE_V2`
- `ADMIN_SR_PEER_EDIT`
- `ADMIN_SR_PEER_REMOVE`

## 验证证据

- 单节点维护 lab：
  - service restart typed/raw PASS
  - server update typed/raw expected failure PASS
  - force-unlock typed/raw expected failure PASS
- 四节点分布式 erasure lab：
  - net speedtest typed/raw PASS
  - force-unlock typed/raw PASS
- 双站点 site replication lab：
  - site replication add PASS
  - site speedtest typed/raw PASS
  - site replication edit typed/raw PASS
  - site replication remove/restore PASS

## 结论

SDK 发布候选能力继续保持可用；核心桶/对象路径已由阶段 125 live 测试锁住。正式发布仍不能自动放行，因为 server update 需要真实升级/回滚策略，SR peer edit/remove 是内部 peer 维护接口，仍建议由用户决定是否保留公开入口或降级为高级/内部接口。
