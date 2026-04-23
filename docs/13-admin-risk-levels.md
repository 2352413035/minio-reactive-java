# 13 Admin 接口风险分级

MinIO Admin API 覆盖用户、策略、配置、站点复制、tier、批处理、性能测试等能力。SDK 不能把所有接口都包装成“低成本便捷方法”，否则会让调用方误触发高风险操作。

## 风险等级

| 等级 | 类型 | 示例 | SDK 策略 | 测试策略 |
| --- | --- | --- | --- | --- |
| L1 | 安全只读 | server info、storage info、account info、list/info 类 | 优先提供 typed 方法 | 可默认单元测试和安全 live 测试 |
| L2 | 可回滚写 | add/delete user、policy、group、service account | 提供 typed request/response，必须清楚命名 | live 测试要创建临时资源并清理 |
| L3 | 需要隔离环境 | set config、IDP、bucket quota、remote target、replication、tier、batch job | 需要显式 request 对象和中文风险注释 | 默认跳过，只在 `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true` 时运行 |
| L4 | 不默认便捷化 | server update、service restart/stop、site replication destructive op、force unlock、speedtest | 保留 advanced/raw，除非有明确需求 | 不在共享环境默认执行 |

## 当前配置写接口边界

`setConfigKvText(...)` 和 `setConfigText(...)` 已经能生成 madmin 兼容加密请求体，但它们会修改服务端配置。当前集成测试不会默认执行真实配置写入。后续如果要测试这些接口，必须满足：

1. 使用独立、可回滚的 MinIO 环境。
2. 测试前读取原配置。
3. 写入测试配置后立即恢复原配置。
4. 测试失败也要执行恢复逻辑。
5. 通过 `MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true` 显式开启。
6. 执行前运行 `scripts/minio-lab/verify-env.sh`。

## 使用建议

- 业务项目优先使用 L1/L2 typed 方法。
- L3/L4 场景应先在测试环境验证，再迁移到生产。
- 如果 SDK 尚未提供 typed 方法，可以使用 `ReactiveMinioRawClient` 兜底，但调用方需要自行承担协议和风险确认。
