# 阶段 50：敏感导入与 metadata 恢复边界

## 1. 本阶段结论

阶段 50 为 IAM 和 bucket metadata 导入类接口补充明确的产品边界，但不把它们变成共享环境默认可执行能力。

新增方法：

- `importIamArchive(byte[] archiveBytes, String contentType)`
- `importIamV2Archive(byte[] archiveBytes, String contentType)`
- `importBucketMetadataArchive(byte[] archiveBytes, String contentType)`

这些方法返回 `AdminTextResult`，只保留服务端响应文本和来源，不记录导入包内容。无 `contentType` 重载默认使用 `application/octet-stream`。

## 2. 为什么仍然属于 lab/维护窗口能力

导入 IAM 或 bucket metadata 可能覆盖用户、策略、服务账号、bucket 策略、复制、通知等配置。它们不是普通只读接口，不能在共享 MinIO live 测试中执行，也不能在没有备份和回滚计划的情况下调用。

因此阶段 50 的处理原则是：

1. SDK 提供更清晰的强类型入口，减少调用方手写 raw 的概率。
2. 文档明确这些方法只应在独立可回滚 lab 或维护窗口执行。
3. 单元测试只用 mock WebClient 验证路径、content type、返回边界和 raw 兜底，不执行真实导入。
4. 旧的 `importIam(...)`、`importIamV2(...)`、`importBucketMetadata(...)` 字符串入口保留但标记为 `@Deprecated`。

## 3. 推荐用法

```java
byte[] archive = Files.readAllBytes(path);
AdminTextResult result = admin.importIamArchive(archive, "application/zip").block();
```

调用方必须自行负责：

- 备份当前环境。
- 确认可回滚。
- 控制维护窗口。
- 不把导入包内容、secret、token、签名写入日志。
- 如需真实验证，使用 `scripts/minio-lab/` 下的独立 lab 流程。

## 4. 与 raw 兜底的关系

raw 仍保留给排障和特殊参数：

```java
String text = raw.executeToString(
    MinioApiCatalog.byName("ADMIN_IMPORT_IAM"),
    Collections.emptyMap(),
    Collections.emptyMap(),
    Collections.emptyMap(),
    archive,
    "application/octet-stream").block();
```

产品入口和 raw 路径在单元测试中交叉验证，但真实导入只允许在独立 lab 中证明。

## 5. 能力矩阵影响

Admin product-typed 从 78 / 128 提升到 81 / 128。`destructive-blocked = 29` 不减少，因为导入类接口仍需要独立 lab 真实证据；`encrypted-blocked = 9` 不变。

## 6. 验证命令

```bash
mvn -q -Dtest=ReactiveMinioSpecializedClientsTest test
python3 scripts/report-capability-matrix.py --worktree . --format markdown --output ../.omx/reports/capability-matrix-stage50-jdk8.md
git diff --check
```

JDK17+ 分支同步后继续执行 JDK17 全量测试、真实 MinIO smoke、JDK21/JDK25 test-compile、Crypto Gate 和 secret scan。
