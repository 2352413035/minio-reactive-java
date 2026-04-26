# 阶段 125：核心桶与对象/文件操作真实 MinIO 验证

## 背景

本阶段根据最新优先级调整：剩余少量运维型、破坏性或强拓扑依赖的 Admin 接口，如果独立环境搭建成本过高且不是普通业务集成关键路径，可以作为受限项保留；SDK 必须优先保证用户最常用的桶、对象和本地文件上传下载能力在真实 MinIO 上可用。

## 本阶段新增验证

在 `LiveMinioIntegrationTest` 中新增 `shouldVerifyUserBucketAndFileFlowsWithTypedAndRawClients`，覆盖：

- 桶创建、桶存在性检查、raw HEAD bucket 状态确认。
- 强类型对象写入、Flux 流式读取、按 Range 下载、整对象读取。
- 本地文件上传到对象。
- 对象下载到本地文件。
- 下载目标已存在时的覆盖保护。
- 显式覆盖下载。
- ListObjectsV2 单页列举。
- Multipart upload 创建后终止，并确认未产生残留对象。
- raw client 通过 catalog 直接 PUT/GET/HEAD/LIST/DELETE 对象，证明 raw 仍能作为新增接口或排障时的兜底调用器。
- 删除对象后确认再读取元数据返回 404。

## 设计边界

- `ReactiveMinioClient` 仍是普通业务优先使用的强类型客户端，负责提供更明确的方法名、参数、异常和行为约束。
- `ReactiveMinioRawClient` 不替代强类型客户端，只用于 SDK 暂未封装的新接口、特殊排障、或需要直接按 catalog 调用 HTTP 接口的场景。
- 非关键破坏性 Admin 能力继续保留在受限验证清单中，不影响桶/对象核心业务路径的发布候选判断。

## 验证命令

JDK8 分支：

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH \
MINIO_ENDPOINT=http://127.0.0.1:9000 \
MINIO_ACCESS_KEY=*** \
MINIO_SECRET_KEY=*** \
MINIO_REGION=us-east-1 \
mvn -q -Dtest=LiveMinioIntegrationTest test
```

JDK17 分支：

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
MINIO_ENDPOINT=http://127.0.0.1:9000 \
MINIO_ACCESS_KEY=*** \
MINIO_SECRET_KEY=*** \
MINIO_REGION=us-east-1 \
mvn -q -Dtest=LiveMinioIntegrationTest test
```

## 结果

- JDK8：`LiveMinioIntegrationTest` 4 个测试全部通过。
- JDK17：`LiveMinioIntegrationTest` 4 个测试全部通过。

