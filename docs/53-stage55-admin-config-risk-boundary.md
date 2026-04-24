# 阶段 55：Admin 配置高风险边界

## 1. 本阶段目标

阶段 55 把配置 KV 删除、配置历史清理、配置历史恢复补充为明确的产品文本边界。它们仍然是高风险 Admin 操作：SDK 只固定调用语义和响应类型，不替调用方判断配置内容是否安全。

## 2. 新增产品方法

| 方法 | 对应路由 | 返回 | 边界说明 |
| --- | --- | --- | --- |
| `deleteConfigKvEntry(...)` | `ADMIN_DELETE_CONFIG_KV` | `AdminTextResult` | 要求非空请求体；执行前必须完成配置备份。 |
| `clearConfigHistoryEntry(...)` | `ADMIN_CLEAR_CONFIG_HISTORY_KV` | `AdminTextResult` | 要求明确 `restoreId`；不在共享 live 中真实执行。 |
| `restoreConfigHistoryEntry(...)` | `ADMIN_RESTORE_CONFIG_HISTORY_KV` | `AdminTextResult` | 要求明确 `restoreId`；调用方必须确认回滚窗口。 |

## 3. 安全边界

- SDK 不读取、不保存、不记录真实配置值。
- 配置删除请求体不能为空，避免误发空恢复/删除请求。
- 配置历史清理和恢复必须传入非空 `restoreId`。
- 本阶段只做 mock 与 raw 兜底交叉验证，不在共享 MinIO 上真实修改配置。

## 4. 当前能力矩阵变化

- Admin product-typed：94 / 128 -> 97 / 128。
- `raw-fallback = 0` 保持不变。
- `encrypted-blocked = 9` 保持不变。
- `destructive-blocked = 29` 保持不变。

## 5. 后续建议

下一阶段如果继续处理服务控制、服务端升级、token 吊销或站点复制 peer 写入，必须继续使用高风险命名、中文风险注释、mock/raw 交叉验证和独立 lab 门禁。
