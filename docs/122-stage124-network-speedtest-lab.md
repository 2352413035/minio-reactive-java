# 阶段 124：net/site speedtest 独立 lab 边界复核

## 背景

阶段 123 后，破坏性 Admin 剩余证据为 10，其中资源压测类还剩：

- `ADMIN_SPEEDTEST_NET`
- `ADMIN_SPEEDTEST_SITE`

阶段 124 的目标是：在独立 Docker lab 下同时验证 typed/raw 路径，并明确这两个接口在当前单节点 lab 上的真实服务端行为。

## 本阶段实现

1. `DestructiveAdminIntegrationTest` 增加 net/site speedtest 可选探测：
   - `MINIO_LAB_ENABLE_NET_SPEEDTEST_PROBE`
   - `MINIO_LAB_SPEEDTEST_NET_DURATION_SECONDS`
   - `MINIO_LAB_EXPECT_NET_SPEEDTEST_FAILURE`
   - `MINIO_LAB_NET_SPEEDTEST_EXPECTED_ERROR`
   - `MINIO_LAB_ENABLE_SITE_SPEEDTEST_PROBE`
   - `MINIO_LAB_SPEEDTEST_SITE_DURATION_SECONDS`
   - `MINIO_LAB_EXPECT_SITE_SPEEDTEST_FAILURE`
   - `MINIO_LAB_SITE_SPEEDTEST_EXPECTED_ERROR`
2. 预期失败模式支持关键字匹配（`|` / `,` 分隔多个关键字），用于记录“服务端前置条件未满足”证据，不把失败伪装成通过。
3. 新增脚本 `scripts/minio-lab/run-network-speedtest-lab.sh`，一键启动一次性 Docker lab 并执行 net/site 探测。
4. `audit-fixtures.sh`、`write-report.sh`、`lab.example.properties` 与 `scripts/minio-lab/README.md` 同步增加 net/site 参数、审计和报告口径。
5. `scripts/report-destructive-boundary.py` 更新 net/site 说明：当前单节点 lab 返回 `501 NotImplemented`，边界不下降。

## 实验与证据

### A. 预期失败采证模式（可重复）

执行方式：

```bash
scripts/minio-lab/run-network-speedtest-lab.sh
```

默认启用：

- `MINIO_LAB_EXPECT_NET_SPEEDTEST_FAILURE=true`
- `MINIO_LAB_EXPECT_SITE_SPEEDTEST_FAILURE=true`
- 关键字默认 `NotImplemented|...`

结果：typed/raw 两条路径都按“失败且命中关键字”记为 PASS（采证通过）。

证据文件：

- JDK8：
  - `.omx/reports/stage124-jdk8-net-site-speedtest-lab.md`
  - `.omx/reports/stage124-jdk8-net-site-speedtest-lab.steps`
- JDK17：
  - `.omx/reports/stage124-jdk17-net-site-speedtest-lab.md`
  - `.omx/reports/stage124-jdk17-net-site-speedtest-lab.steps`

### B. 直接探测模式（EXPECT=false）

为了确认真实服务端响应，本阶段额外执行了 net-only 与 site-only 的直探（关闭 EXPECT）：

- `ADMIN_SPEEDTEST_NET`：typed 路径报 `HTTP 501 / NotImplemented`
- `ADMIN_SPEEDTEST_SITE`：typed 路径报 `HTTP 501 / NotImplemented`
- raw 同路由也一致返回同类错误

证据文件：

- JDK8：
  - `.omx/reports/stage124-jdk8-net-speedtest-direct-fail.log`
  - `.omx/reports/stage124-jdk8-site-speedtest-direct-fail.log`
  - `.omx/reports/stage124-jdk8-net-direct-fail-report.md`
  - `.omx/reports/stage124-jdk8-site-direct-fail-report.md`
- JDK17：
  - `.omx/reports/stage124-jdk17-net-speedtest-direct-fail.log`
  - `.omx/reports/stage124-jdk17-site-speedtest-direct-fail.log`
  - `.omx/reports/stage124-jdk17-net-direct-fail-report.md`
  - `.omx/reports/stage124-jdk17-site-direct-fail-report.md`

## 结论

1. SDK 侧 typed/raw 路由映射正确，探测链路完整。
2. 当前单节点 Docker lab 下，`ADMIN_SPEEDTEST_NET` 与 `ADMIN_SPEEDTEST_SITE` 均返回 `NotImplemented`，不是 SDK 路由缺失问题。
3. 因此本阶段不降低 `destructive-blocked` 统计；这两项继续保留在“资源压测”边界，等待具备服务端实现和合适窗口的环境再次验证。
