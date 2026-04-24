# 阶段 47：Admin 敏感导出二进制边界

## 1. 本阶段结论

阶段 47 将两个只读但敏感的 Admin 导出接口从字符串兼容入口补充为产品级二进制边界：

- `ReactiveMinioAdminClient.exportIamData()`
- `ReactiveMinioAdminClient.exportBucketMetadataData()`

这两个方法都返回 `AdminBinaryResult`，保留 `source()`、`bytes()`、`size()` 和 `isEmpty()`，不把导出包解码成字符串。

## 2. 为什么不用 `Mono<String>`

IAM 导出和 bucket metadata 导出都可能是压缩包、备份包或版本相关的二进制内容。继续让用户走 `exportIam()` / `exportBucketMetadata()` 的 `Mono<String>` 兼容入口，容易造成三个问题：

1. 二进制内容被错误字符集破坏。
2. 调用方误以为响应是普通 JSON 或文本。
3. 敏感配置被直接写入日志。

因此阶段 47 的产品入口只暴露二进制包装，并在文档里明确：导出包可能包含用户、策略、服务账号、bucket 策略、复制、通知等配置，调用方必须按备份密件处理。

## 3. 与 raw 兜底的关系

专用客户端路径用于普通业务集成：

```java
AdminBinaryResult iam = admin.exportIamData().block();
AdminBinaryResult metadata = admin.exportBucketMetadataData().block();
```

raw 兜底仍然可用，适合 SDK 未来未及时封装的新参数或排障场景：

```java
byte[] bytes = raw.executeToBytes(MinioApiCatalog.byName("ADMIN_EXPORT_IAM"), ...).block();
```

两条路径在测试中使用同一个 mock WebClient 交叉验证，证明专用客户端更易用，raw 仍然具备通用性。

## 4. 能力矩阵影响

Admin product-typed 从 64 / 128 提升到 66 / 128。`encrypted-blocked = 9` 和 `destructive-blocked = 29` 不变。

## 5. 验证命令

```bash
mvn -q -Dtest=ReactiveMinioSpecializedClientsTest test
python3 scripts/report-capability-matrix.py --worktree . --format markdown --output ../.omx/reports/capability-matrix-stage47-jdk8.md
git diff --check
```

JDK17+ 分支同步后继续执行同等单元测试、能力矩阵、全量测试、真实 MinIO smoke 和 JDK21/JDK25 compile。
