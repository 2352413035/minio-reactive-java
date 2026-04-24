# 阶段 44：错误解释与异常体验统一

阶段 44 的目标是让 typed 客户端和 raw 兜底在失败时给出更容易理解的中文诊断，而不是只暴露英文底层异常。

## 1. 本阶段变化

- `ReactiveMinioException` 的默认异常消息改为中文，包含协议族、HTTP 状态、错误码、错误信息、requestId、接口名、HTTP 方法、路径和诊断建议。
- Admin/KMS/STS/Metrics/Health 等非 S3 协议族仍会映射到各自的 `ReactiveMinio*Exception`，但消息不再使用 `request failed with HTTP status` 这类英文描述。
- raw 兜底调用的本地校验错误改为中文：
  - 缺少必填 query 参数。
  - 调用方传入签名器管理的 header。
  - 单段路径变量包含 `/`。
  - 路径变量使用 `.` 或 `..`。
  - 不支持的 HTTP 方法。

## 2. 仍然保留的排障字段

中文消息只是给人读的摘要，调用方仍然可以继续读取结构化字段：

- `protocol()`
- `statusCode()`
- `code()`
- `errorMessage()`
- `requestId()`
- `rawBody()`
- `endpointName()`
- `method()`
- `path()`
- `diagnosticHint()`

这保证了业务代码可以继续按错误码或状态码分支，不需要解析中文文本。

## 3. 验证重点

- Admin JSON 错误仍解析为 `ReactiveMinioAdminException`。
- 纯文本错误会在中文消息里显示 `响应体片段=`。
- 本地请求构造失败会在发送到 MinIO 前抛出中文 `IllegalArgumentException`。
- route parity、capability matrix、Crypto Gate 和 live 测试不受影响。
