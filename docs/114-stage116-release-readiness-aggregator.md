# 阶段 116 发布就绪总览机器报告

## 背景

阶段 115 已把 Maven/tag 发布前必须由负责人确认的元数据拆成清单，但发布判断仍分散在多个报告中。阶段 116 新增 `scripts/report-release-readiness.py`，把功能覆盖、Crypto Gate、POM 元数据和破坏性边界聚合成一份总览。

## 脚本用途

`report-release-readiness.py` 只读取源码和已有报告脚本，不连接 MinIO，不执行写入，不发布 Maven，也不修改 `pom.xml`。

它聚合以下口径：

- `report-minio-java-parity.py`：对象 API、Admin API、Args、credentials provider 名称级对标。
- `report-minio-java-signature-parity.py`：缺失方法、重载较少项、Args builder 入口和响应式内部边界。
- `report-route-parity.py`：MinIO 服务端路由与 SDK catalog 对标。
- `report-capability-matrix.py`：product-typed、raw-fallback、encrypted-blocked、destructive-blocked。
- `report-pom-release-metadata.py`：POM 发布元数据和 release 插件缺口。
- `report-destructive-boundary.py`：29 个破坏性 Admin 路由的风险分类。

## 使用方式

```bash
python3 scripts/report-release-readiness.py \
  --worktree . \
  --minio-java-root /dxl/minio-project/minio-java \
  --minio-root /dxl/minio-project/minio \
  --format markdown \
  --output /dxl/minio-project/.omx/reports/release-readiness-jdk8.md
```

双分支可在任意一个脚本副本中一次性生成：

```bash
python3 minio-reactive-java/scripts/report-release-readiness.py \
  --worktree minio-reactive-java \
  --worktree minio-reactive-java-jdk17 \
  --minio-java-root minio-java \
  --minio-root minio \
  --format markdown \
  --output .omx/reports/release-readiness-combined.md
```

## 当前判定

当前聚合报告的关键结论是：

| 门禁 | 当前结论 |
| --- | --- |
| SDK 发布候选 | 就绪。minio-java 对象/Admin API、Args、credentials、签名级缺口、route parity、raw fallback 与 Crypto Gate 均已收口。 |
| 正式 Maven/tag 发布 | 未就绪。POM 发布元数据、source/javadoc/sign/SBOM 插件、发布仓库与回滚策略仍需负责人确认。 |
| 破坏性 Admin 证据 | 29 个风险路由中 11 个已有独立 lab 证据，其余仍需独立 lab 或维护窗口。 |

## 维护要求

- 新增公开 API、Args 或 credentials provider 后，必须重新运行该聚合脚本。
- 修改 `pom.xml` 发布元数据或 release profile 后，必须重新运行该聚合脚本。
- 降低 `destructive-blocked` 前，必须先更新 `report-destructive-boundary.py` 的证据说明，并附 lab/维护窗口报告。
- 聚合报告显示“正式 Maven/tag 发布就绪 = 否”时，不得打正式 tag，不得执行 `mvn deploy`。
