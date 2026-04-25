# 阶段 120：speedtest 有界参数与独立 Docker lab 补证

## 背景

MinIO 的 speedtest 系列接口会触发真实资源消耗，服务端还会在运行期间短暂 freeze S3 API。因此这些接口不能在共享环境默认执行。阶段 120 的目标是：先把 SDK 的参数语义对齐到 madmin/server，再只在一次性 Docker lab 中用小资源窗口收集 typed/raw 证据。

## 本阶段调整

1. 新增 `AdminSpeedtestOptions`：覆盖 object/cluster speedtest 的 `size`、`duration`、`concurrent`、`bucket`、`autotune`、`noclear`、`enableSha256`、`enableMultipart` 与 `storage-class` query。
2. 新增 `AdminDriveSpeedtestOptions`：覆盖 drive speedtest 的 `serial`、`blocksize`、`filesize` query，并强制调用方显式传入正数 `blocksize/filesize`，避免漏填后触发服务端默认 1GiB 文件压测。
3. `ReactiveMinioAdminClient` 新增有界重载：
   - `runSpeedtest(AdminSpeedtestOptions)` / `speedtest(AdminSpeedtestOptions)`
   - `runObjectSpeedtest(AdminSpeedtestOptions)` / `speedtestObject(AdminSpeedtestOptions)`
   - `runDriveSpeedtest(AdminDriveSpeedtestOptions)` / `speedtestDrive(AdminDriveSpeedtestOptions)`
   - `runNetworkSpeedtest(Duration)` / `speedtestNet(Duration)`
   - `runSiteSpeedtest(Duration)` / `speedtestSite(Duration)`
4. 旧式无参/空 body 的 speedtest 强类型入口已标记 `@Deprecated` 并直接返回中文错误，不再允许从强类型客户端触发服务端默认大资源压测；需要完全自定义时使用 `ReactiveMinioRawClient` 显式传 query。
5. 破坏性 lab 增加 `MINIO_LAB_ENABLE_SPEEDTEST_PROBES` 与有界对象压测参数。
6. 执行一次性 Docker lab，JDK8 与 JDK17 分支均验证：
   - typed `runSpeedtest(...)`
   - raw `ADMIN_SPEEDTEST`
   - typed `runObjectSpeedtest(...)`
   - raw `ADMIN_SPEEDTEST_OBJECT`

## 独立 lab 参数

本次补证使用：

- endpoint：一次性 Docker MinIO，非共享环境；
- object size：`1048576` 字节；
- concurrency：`1`；
- duration：`2s`；
- bucket：本次 lab 临时 bucket；
- drive speedtest：未启用，继续保留资源压测边界。

报告路径：

- JDK8：`.omx/reports/stage120-jdk8-speedtest-lab.md`
- JDK17：`.omx/reports/stage120-jdk17-speedtest-lab.md`

## 边界更新

`ADMIN_SPEEDTEST` 与 `ADMIN_SPEEDTEST_OBJECT` 已有独立 lab typed/raw 证据，因此在破坏性边界报告中归入“已有独立 lab 证据”。

其余 speedtest 路由仍不降低边界：

- `ADMIN_SPEEDTEST_DRIVE`：即使可传小文件，也会触发磁盘读写，后续单独补证；
- `ADMIN_SPEEDTEST_NET`：服务端要求分布式 erasure 环境，单节点 lab 不构成证据；
- `ADMIN_SPEEDTEST_SITE`：需要 site replication 拓扑，普通 lab 不构成证据。
