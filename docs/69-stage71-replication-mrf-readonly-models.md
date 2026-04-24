# 阶段 71：Replication MRF backlog 只读摘要模型

## 1. 本阶段目标

阶段 71 继续做非破坏性成熟度提升，聚焦 `replication/mrf` 只读诊断入口。MinIO 服务端的该接口按行输出 JSON，并可能穿插空格 keep-alive，因此不能简单假设它永远是一个普通 JSON object。

本阶段新增 `AdminReplicationMrfSummary`，用于把 backlog 流中的每一条 MRF 记录提取成安全摘要，同时保留原始文本，方便调用方继续做版本差异排障。

本阶段不新增 MinIO 路由，不改变 catalog，不改变版本号，不发布 Maven，不打 tag，也不修改任何复制配置。

## 2. 新增模型与方法

| 模型 | 新方法 | 保留的通用方法 | 提取字段 |
| --- | --- | --- | --- |
| `AdminReplicationMrfSummary` | `getReplicationMrfSummary(bucket)` | `getReplicationMrfInfo(bucket)`、`replicationMrf(bucket)` | nodeName、bucket、object、versionId、retryCount、error，并汇总 entryCount、errorCount、totalRetryCount。 |

`AdminReplicationMrfSummary` 兼容三类输入：

1. MinIO 真实接口常见的按行 JSON 流。
2. 空格或空行 keep-alive。
3. 测试环境或旧封装里使用的单个 JSON object / JSON array。

## 3. 使用示例

```java
AdminReplicationMrfSummary mrf = admin.getReplicationMrfSummary("bucket-a").block();
int backlog = mrf.entryCount();
int failedLines = mrf.errorCount();
int retryTotal = mrf.totalRetryCount();
```

如果需要完整响应或服务端新增字段：

```java
String raw = admin.getReplicationMrfSummary("bucket-a").block().rawText();
AdminJsonResult legacy = admin.getReplicationMrfInfo("bucket-a").block();
```

## 4. 安全边界

1. `getReplicationMrfSummary(bucket)` 只读取 MRF backlog，不触发复制配置写入。
2. 该模型用于排障和看板，不代表 replication diff、remote target 或 site replication 写入类操作已经在共享环境放行。
3. 如果 MinIO 后续在 MRF 记录中增加字段，调用方可先使用 `rawText()` 或 raw client 兜底，SDK 再按稳定字段补强。

## 5. 验证口径

阶段 71 至少需要验证：

- 摘要模型能跳过 keep-alive 空白行。
- 摘要模型能解析按行 JSON，并兼容大小写字段名。
- 专用方法命中原 `replication/mrf` 路由并保留 `bucket` 查询参数校验。
- 原 `getReplicationMrfInfo(bucket)` 继续可用。
- route parity 与 capability matrix 不退化。
- JDK8/JDK17 全量测试与 live 测试通过。
- JDK17+ 分支 JDK21/JDK25 `test-compile` 通过。
- Crypto Gate Fail、破坏性 lab 拒绝门禁和凭据扫描继续通过。
