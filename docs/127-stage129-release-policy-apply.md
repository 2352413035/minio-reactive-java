# 阶段 129：将剩余 4 个运维接口的发布策略真正落地

## 背景

阶段 128 已经给出决策矩阵，用户也已经明确接受该策略：

- `ADMIN_SERVER_UPDATE`
- `ADMIN_SERVER_UPDATE_V2`
- `ADMIN_SR_PEER_EDIT`
- `ADMIN_SR_PEER_REMOVE`

不再把它们当作普通用户高频路径的发布阻塞，而是按风险级别分层处理。

## 本阶段动作

1. 更新发布门禁文档 `docs/release-gates.md`
2. 更新用户面快速使用指南 `docs/60-stage62-user-facing-release-guide.md`
3. 更新发布交接文档 `docs/63-stage65-release-handoff.md`
4. 更新发布总览脚本 `scripts/report-release-readiness.py`

## 最终策略

### 1. `ADMIN_SERVER_UPDATE` / `ADMIN_SERVER_UPDATE_V2`

- 保留 SDK 代码入口
- 不作为普通用户常用能力承诺
- 对外口径统一为：**高级/维护窗口接口**

### 2. `ADMIN_SR_PEER_EDIT` / `ADMIN_SR_PEER_REMOVE`

- 保留 SDK 代码入口
- 不在普通用户主文档强调
- 对外口径统一为：**internal / advanced**

## 这意味着什么

### 对用户

用户最常用的桶、文件/对象和常见只读 Admin 路径，已经可以作为主能力说明；
这 4 个接口不再混入“普通使用路径”的发布判断。

### 对发布

- `destructive-blocked` 仍保留为风险计数
- 但发布总览不再把这 4 个接口当作“普通用户发布阻塞”
- 真正还阻塞正式 Maven/tag 发布的，主要是负责人提供的发布工程材料

### 对未来

如果未来要重新宣称这 4 个接口“可安全使用”，仍然必须补：

- 独立 lab 报告，或
- 维护窗口/回滚演练报告

## 结论

阶段 129 解决的是“如何对外说”，不是“伪造已经验证”：

- 代码入口保留
- 风险边界保留
- 用户面暴露级别下调
- 发布口径与实际证据对齐
