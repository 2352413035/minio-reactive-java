# 阶段 75：Stage 74 后 SDK 缺口审计

## 1. 审计结论

阶段 75 基于阶段 74 的新统计口径重新审计 SDK 当前状态。结论是：当前没有公开路由缺口、没有 raw-only 调用缺口、没有产品入口缺口；剩余工作主要是外部门禁和发布工程，不应该通过新增重复 API 或降低统计口径来宣称完成。

当前基线：

| 口径 | 结果 |
| --- | ---: |
| route parity | 233 / 233 |
| catalog 缺失 | 0 |
| catalog 额外 | 0 |
| product-typed | 233 / 233 |
| raw-fallback | 0 |
| encrypted-blocked | 11 |
| destructive-blocked | 29 |

## 2. 为什么 encrypted-blocked 是 11

阶段 74 已确认 `ADMIN_LIST_IDP_CONFIG` 和 `ADMIN_GET_IDP_CONFIG` 也属于 madmin 默认加密响应。当前 11 个加密边界是：

1. `ADMIN_LIST_USERS`
2. `ADMIN_ADD_SERVICE_ACCOUNT`
3. `ADMIN_INFO_SERVICE_ACCOUNT`
4. `ADMIN_LIST_SERVICE_ACCOUNTS`
5. `ADMIN_GET_CONFIG`
6. `ADMIN_GET_CONFIG_KV`
7. `ADMIN_LIST_CONFIG_HISTORY_KV`
8. `ADMIN_LIST_IDP_CONFIG`
9. `ADMIN_GET_IDP_CONFIG`
10. `ADMIN_INFO_ACCESS_KEY`
11. `ADMIN_LIST_ACCESS_KEYS_BULK`

这些接口已经有 `EncryptedAdminResponse` 或等价产品边界，但 Crypto Gate Pass 前不能升级为明文 typed 模型。

## 3. 为什么 destructive-blocked 仍是 29

29 个破坏性或高风险 Admin 能力已经有产品边界、lab-only 包装或维护窗口说明，但共享 MinIO 环境不能作为真实写入验证环境。它们需要独立、可回滚、非共享的 MinIO lab 证明：

- typed 专用客户端路径可用；
- raw catalog 兜底路径可用；
- 操作后可以恢复；
- 报告中记录每一步 PASS/FAIL。

在没有该证据前，不能把这些接口写成“普通 live 已验证通过”。

## 4. 当前仍可做的非破坏性工作

后续如果继续推进，应优先选择：

1. Crypto Gate Pass 前置材料补齐，例如依赖审查 checklist、失败回退测试清单。
2. 独立 lab 运行手册或报告模板强化，但不在共享环境执行破坏性写入。
3. 用户示例和中文诊断补充，帮助使用者理解专用客户端、raw fallback、加密边界和 lab-only 边界。
4. 发布工程准备，例如 tag/Maven 发布 checklist；但正式 tag 必须等待外部门禁满足。

## 5. 当前不应做的事

1. 不应新增重复 API 来“提高覆盖率”，因为 route/product/raw 覆盖已经满格。
2. 不应把 `EncryptedAdminResponse` 伪装成明文配置或凭证模型。
3. 不应在共享端点执行 decommission、site replication 写入、batch job start/cancel、service update、force unlock 等危险操作。
4. 不应打正式 tag 或发布 Maven，除非 Crypto Gate 和独立 lab 证据都已经满足。

## 6. 下一阶段建议

阶段 76 应聚焦 Crypto Gate Pass 的可执行准备：不新增依赖，只把批准材料、候选依赖、许可证/安全公告审查、JDK8/JDK17/JDK21/JDK25 测试矩阵、失败回退语义整理成最终 checklist。
