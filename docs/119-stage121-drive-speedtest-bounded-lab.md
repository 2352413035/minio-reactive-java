# 阶段 121：drive speedtest 小文件独立 lab 补证

## 背景

阶段 120 已经为 `ADMIN_SPEEDTEST` 与 `ADMIN_SPEEDTEST_OBJECT` 补齐 typed/raw 有界证据，并禁用强类型客户端的无参/空 body speedtest 入口。剩余资源压测中，`ADMIN_SPEEDTEST_DRIVE` 不依赖多站点拓扑，只需要严格限制磁盘测试文件大小，因此可以继续在一次性 Docker lab 中验证。

## 本阶段调整

1. `DestructiveAdminIntegrationTest` 的 drive speedtest 探测从 typed 单路径扩展为 typed/raw 双路径。
2. `audit-fixtures.sh` 增加 `drive speedtest bounded typed/raw 探测` 准备度行，要求显式开关和正数 `blocksize/filesize`。
3. `report-destructive-boundary.py` 将 `ADMIN_SPEEDTEST_DRIVE` 归入“已有独立 lab 证据”。
4. 继续保留强类型无参/空 body speedtest 入口禁用策略；调用方必须使用 `AdminDriveSpeedtestOptions` 或 raw client 显式 query。

## 独立 lab 参数

本次补证使用一次性 Docker MinIO，参数如下：

- `MINIO_LAB_ENABLE_DRIVE_SPEEDTEST_PROBE=true`
- `serial=true`
- `blocksize=4096`
- `filesize=8192`

报告路径：

- JDK8：`.omx/reports/stage121-jdk8-drive-speedtest-lab.md`
- JDK17：`.omx/reports/stage121-jdk17-drive-speedtest-lab.md`

## 边界更新

`ADMIN_SPEEDTEST_DRIVE` 已有独立 lab typed/raw 证据，因此从“资源压测”移动到“已有独立 lab 证据”。

仍不降低以下边界：

- `ADMIN_SPEEDTEST_NET`：需要分布式 erasure 或多节点网络拓扑；
- `ADMIN_SPEEDTEST_SITE`：需要 site replication 拓扑。
