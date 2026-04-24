# 阶段 57：服务控制、升级与 token 吊销边界

## 1. 本阶段目标

阶段 57 将服务控制、服务端升级、token 吊销补充为明确的高风险产品文本边界。这些接口影响范围很大，SDK 只固定方法命名、输入校验和响应边界，不替调用方决定是否应该执行。

## 2. 新增产品方法

| 方法 | 对应路由 | 返回 | 边界说明 |
| --- | --- | --- | --- |
| `executeServiceControl(...)` | `ADMIN_SERVICE` | `AdminTextResult` | 可能停止、重启或改变服务状态。 |
| `executeServiceControlV2(...)` | `ADMIN_SERVICE_V2` | `AdminTextResult` | v2 服务控制，携带 `type=2`。 |
| `startServerUpdate(...)` | `ADMIN_SERVER_UPDATE` | `AdminTextResult` | `updateURL` 必须显式传入。 |
| `startServerUpdateV2(...)` | `ADMIN_SERVER_UPDATE_V2` | `AdminTextResult` | v2 服务端升级，携带 `type=2`。 |
| `revokeUserProviderTokens(...)` | `ADMIN_REVOKE_TOKENS` | `AdminTextResult` | 会吊销指定用户提供方 token。 |

## 3. 安全边界

- 不在共享 live 中真实执行服务控制、升级或 token 吊销。
- action、updateURL、userProvider 必须非空。
- raw client 仍可用 catalog 兜底调用同一 endpoint，但产品方法用更明确的高风险名称引导用户。
- `destructive-blocked` 不减少，除非独立 lab 给出真实恢复证据。

## 4. 当前能力矩阵变化

- Admin product-typed：103 / 128 -> 108 / 128。
- `raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 保持不变。
- `destructive-blocked = 29` 保持不变。

## 5. 后续建议

阶段 58 已重新梳理 Crypto Gate 与独立 lab 阻塞，并把既有加密响应产品边界纳入 product-typed 统计；若没有三方 Crypto Gate 批准或独立 lab 配置，仍应继续保持 blocked 状态并做发布复审。
