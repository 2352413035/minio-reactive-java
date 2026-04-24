# 阶段 48：Admin 诊断/压测/探测接口边界

## 1. 本阶段结论

阶段 48 为 Admin 中不会直接修改配置、但可能消耗集群资源的诊断/压测/探测接口增加产品化文本边界：

- `runClientDevnull()` / `runClientDevnullExtraTime()`
- `runSiteReplicationDevnull()` / `runSiteReplicationNetperf()`
- `runSpeedtest()` / `runObjectSpeedtest()` / `runDriveSpeedtest()` / `runNetworkSpeedtest()` / `runSiteSpeedtest()`

这些方法返回 `AdminTextResult`，只固定来源和原始文本，不猜测 MinIO 不同版本的诊断文本结构。

## 2. 风险边界

这些接口虽然不是配置写入，但可能触发压测、网络探测、对象读写探测或 site replication 诊断流量。因此：

1. 共享 MinIO live 测试不执行真实压测。
2. 需要在独立维护窗口或实验环境中调用。
3. 调用方应自行设置 Reactor 超时、取消策略和访问权限。
4. 返回文本可能包含环境诊断信息，不应无筛选写入公开日志。

## 3. 与 raw 兜底的关系

专用客户端给普通用户提供清晰入口：

```java
AdminTextResult result = admin.runNetworkSpeedtest().block();
```

raw 兜底仍可用于未封装参数或排障：

```java
String text = raw.executeToString(MinioApiCatalog.byName("ADMIN_SPEEDTEST")).block();
```

阶段 48 单元测试同时验证专用入口和 raw 兜底能构造同一路由。

## 4. 能力矩阵影响

Admin product-typed 从 66 / 128 提升到 75 / 128。`destructive-blocked = 29` 不变，因为 speedtest 类接口仍然属于需要明确风险窗口的高资源诊断能力。`encrypted-blocked = 9` 也不变。

## 5. 验证命令

```bash
mvn -q -Dtest=ReactiveMinioSpecializedClientsTest test
python3 scripts/report-capability-matrix.py --worktree . --format markdown --output ../.omx/reports/capability-matrix-stage48-jdk8.md
git diff --check
```

JDK17+ 分支同步后还要执行 JDK17 全量测试、真实 MinIO smoke、JDK21/JDK25 test-compile、Crypto Gate 和 secret scan。
