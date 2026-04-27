# 阶段 127：高频只读 Admin 发布前加固

## 背景

阶段 126 已把大部分剩余破坏性 Admin 接口补到独立 Docker lab 证据。接下来发布前整理的重点不再是继续硬冲高风险接口，而是把用户最常见的路径再锁一遍：桶、文件/对象，以及常用只读 Admin 视图。

## 本阶段范围

只做最小必要加固，不新增生产实现：

1. 继续沿用已有 `LiveMinioIntegrationTest` 作为行为锁。
2. 新增高频只读 Admin live 证据：
   - `/minio/health/ready`
   - `storage info`
   - `data usage`
   - `account info`
   - `config help`
3. 对上述只读 Admin 视图同时验证：
   - 专用 typed 客户端
   - raw catalog 兜底读链路

## 结果

- 高频桶/文件路径仍由阶段 125 的 live 用例覆盖。
- 高频只读 Admin 视图现在也有独立 live typed/raw 证据。
- 本阶段没有新增 route、没有改生产行为、没有改风险边界分类。

## 结论

正式发布前，SDK 最常用用户路径已经形成两层保护：

1. 对象存储主路径：真实 live 验证。
2. 常见只读运维视图：真实 live typed/raw 验证。

剩余是否保留 `ADMIN_SERVER_UPDATE*` 与 `ADMIN_SR_PEER_*`，仍属于发布策略和运维边界决策，不应再混同为“核心常用路径未验证”。
