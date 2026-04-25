# 阶段 108：batch job 独立 lab 矩阵补证

## 目标

本阶段补齐 batch job 的可回滚实验矩阵证据，重点确认：

1. `start-job` 返回的 JSON 能被强类型结果模型解析出 `jobId`。
2. `status-job` 按 MinIO madmin 语义使用 `jobId` 查询参数读取状态。
3. `cancel-job` 按 MinIO 服务端与 madmin-go 语义使用 `DELETE /cancel-job?id=<jobId>`，不再依赖旧式 YAML cancel 请求体。
4. 专用 `ReactiveMinioAdminClient` 与 `ReactiveMinioRawClient` 都能在独立 Docker lab 中完成同一批接口的 typed/raw 验证。

## 本阶段改动

- 新增 `AdminBatchJobStartResult`，保留原始 JSON，并提取 `jobId`、`type`、`user`、`started`。
- `ReactiveMinioAdminClient` 新增 `startBatchJobInfo(...)` 与 `cancelBatchJobRequest(String jobId)`，并修正 `cancelBatchJob(String jobId)` 使用 `id` 查询参数。
- 保留旧式 `cancelBatchJob(byte[] body, String contentType)` 兼容入口，但文档和 lab 流程不再把 cancel 请求体作为必需项。
- 破坏性 lab 的 batch 矩阵改为：typed start → typed list/status → raw cancel → finally typed cancel → typed list 复核。
- 更新 `scripts/minio-lab` 中文说明、模板和报告生成逻辑，避免继续要求用户预填 batch cancel 请求体。

## 独立 Docker lab 证据

本阶段使用两个一次性 MinIO 容器构建 source/target：

- source 仅暴露在本机隔离端口，作为 `start-job` 所在 MinIO。
- target 通过 Docker 网络容器名让 source 服务端可达。
- source bucket 内放入一组 `batch-prefix/` 测试对象。
- batch replicate YAML 放在 `/tmp` 私有目录，凭证只存在运行时临时文件中，未写入仓库。
- 运行结束后删除两个容器、Docker 网络和临时目录。

JDK8 与 JDK17+ 分支均得到相同 typed/raw 明细：

| 步骤 | 结果 |
| --- | --- |
| typed `startBatchJobInfo` | PASS |
| typed `listBatchJobsInfo` | PASS |
| typed `getBatchJobStatusInfo(jobId)` | PASS |
| raw `ADMIN_CANCEL_BATCH_JOB?id=<jobId>` | PASS |
| finally typed `cancelBatchJob(jobId)` | PASS |
| typed `listBatchJobsInfo` 恢复后复核 | PASS |

两次运行均确认没有遗留 `minio-reactive-batch-*` 或 `minio-reactive-destructive-lab` 容器。

## 仍保留的边界

- batch job 已有独立 lab typed/raw 证据，但它仍属于高风险 Admin 操作；不能在共享 MinIO 或生产环境自动执行。
- 当前能力矩阵里的 `destructive-blocked` 仍按风险路由分类统计，不能仅因某个私有 lab 通过就把破坏性边界清零。
- site replication 多站点 add/edit/remove 仍需要独立多站点 lab 证据。
- Crypto Gate 与正式 Maven/tag 发布材料仍是发布前外部门禁。

## 验证命令

本阶段至少执行并通过：

```bash
# JDK8 单元/集成回归
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH \
mvn -q -DfailIfNoTests=true test

# JDK8/JDK17+ 独立 Docker batch lab
MINIO_LAB_CONFIG_FILE=/tmp/<私有-lab>/lab.properties \
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH \
scripts/minio-lab/run-destructive-tests.sh

MINIO_LAB_CONFIG_FILE=/tmp/<私有-lab>/lab.properties \
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
scripts/minio-lab/run-destructive-tests.sh
```

实际验证使用的临时配置、凭证和 YAML 均位于 `/tmp`，未提交到仓库。
