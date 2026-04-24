# 阶段 38：示例和用户体验收口

阶段 38 的目标是让用户进入 SDK 时先看到清晰、分层、可运行的中文示例，而不是零散测试类或只适合开发者临时验证的代码。

## 1. 示例入口矩阵

| 示例 | 覆盖客户端 | 适用场景 |
| --- | --- | --- |
| `ReactiveMinioLiveExample` | `ReactiveMinioClient` | 对象存储主流程：建桶、上传、查元数据、下载、删除。 |
| `ReactiveMinioTypedAdminExample` | `ReactiveMinioAdminClient` | 管理端 typed 只读摘要和加密边界解释。 |
| `ReactiveMinioRawFallbackExample` | `ReactiveMinioRawClient` | SDK 尚未封装新接口时的 catalog 兜底调用。 |
| `ReactiveMinioOpsExample` | `ReactiveMinioMetricsClient`、`ReactiveMinioHealthClient` | 健康检查和 Prometheus 指标采集。 |
| `ReactiveMinioSecurityExample` | `ReactiveMinioKmsClient`、`ReactiveMinioStsClient` | KMS 状态检查和 STS 临时凭证申请。 |

## 2. 删除的旧示例

阶段 38 删除了 `TestCreateBucket` 与 `TestGetBucketLocation` 两个临时类。它们的问题是：

1. 类名像测试但位于 main examples 包，容易误导用户。
2. bucket 名称和本机端点写死，不符合当前 SDK 的配置方式。
3. 注释和格式不符合当前中文示例标准。

删除后，README 中只保留面向用户的五个正式示例入口。

## 3. 错误解释原则

示例代码遵循三条规则：

1. 对象存储示例明确打印 S3 错误状态码和响应体，方便定位权限、bucket、对象路径问题。
2. Admin 示例不把加密响应伪装成明文模型，而是打印算法和是否加密。
3. KMS / STS / Metrics 这类可选服务不可用时，输出中文解释并保留原始异常消息。

## 4. 当前边界

示例不是破坏性 Admin 实验入口。所有 config、tier、remote target、batch job、site replication 写入仍只能通过 `scripts/minio-lab/` 下的独立 lab 门禁执行。
