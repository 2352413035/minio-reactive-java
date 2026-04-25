# 阶段 119：IDP 配置对齐 minio-java / madmin 语义

## 背景

阶段 118 已把 `ADMIN_REPLICATION_DIFF` 的 query 语义对齐到 madmin。继续检查 IDP 配置接口时发现一个更重要的问题：同目录 `minio-java` 依赖的 madmin 协议对 IDP 配置并不是“普通 JSON 直传”。

在 madmin-go 中：

- `AddOrUpdateIDPConfig(...)` 会先用当前账号的 `secretKey` 调用 `EncryptData(...)`；
- 请求头固定为 `Content-Type: application/octet-stream`；
- 请求体内容是 MinIO config KV 片段，不是 JSON；
- `ListIDPConfig(...)` 与 `GetIDPConfig(...)` 会读取服务端加密响应并自动解密后再解析 JSON；
- `DeleteIDPConfig(...)` 不需要加密请求体，但仍会修改认证链路，必须在可恢复环境执行。

## 本阶段调整

1. `ReactiveMinioAdminClient` 的 IDP add/update 入口改为自动生成 madmin 兼容加密请求体，并强制发送 `application/octet-stream`。
2. `listIdpConfig(...)`、`listIdpConfigs(...)`、`getIdpConfig(...)`、`getIdpConfigInfo(...)` 会自动识别 madmin 加密响应并用当前客户端凭证解密；mock 或旧响应如果是明文 JSON，则继续兼容。
3. `name` 为空时按 madmin 默认配置名 `_` 处理；`ldap` 类型只允许默认配置名。
4. raw client 仍保持兜底调用器语义：它不会猜测业务加密规则。使用 raw 调用 IDP add/update 时，调用方必须自己构造加密 body 并传入 `application/octet-stream`。
5. 破坏性 lab 增加 IDP 夹具准备度检查、示例配置项、OpenID config KV 模板和可选集成测试矩阵。

## 破坏性边界

本阶段不把 `ADMIN_ADD_IDP_CONFIG`、`ADMIN_UPDATE_IDP_CONFIG`、`ADMIN_DELETE_IDP_CONFIG` 从破坏性边界中移除。原因是：

- IDP add/update/delete 会影响 MinIO 认证入口；
- 即使用 Docker 启动独立 MinIO，也仍需要临时 OIDC/LDAP 服务端、client id、client secret、redirect 配置和恢复策略；
- 没有真实 IDP 拓扑时，只能证明 SDK 的协议封装、mock 行为和准备度门禁，不能证明完整认证链路可用。

要降低这三个路由的边界，必须提供独立 lab 报告，至少包括：

1. 独立 MinIO endpoint，不占用共享环境；
2. 独立 OIDC 或 LDAP 夹具；
3. `MINIO_LAB_ALLOW_WRITE_FIXTURES=true`；
4. `MINIO_LAB_ADD_IDP_CONFIG_BODY_FILE` 指向仓库外私有 KV 请求体；
5. `MINIO_LAB_DELETE_IDP_AFTER_TEST=true`；
6. typed add/list/get/delete 与 raw delete/add/list 的 PASS 步骤；
7. 失败时可恢复 server config 的说明。

## 验证口径

- 单元测试验证专用 Admin 客户端的 IDP add/update 请求使用 `application/octet-stream`，且加密后长度大于明文请求体。
- 单元测试验证 list/get 能自动解密 madmin 加密响应并解析为现有摘要模型。
- 破坏性集成测试只在显式 IDP lab 夹具存在时执行，否则跳过；跳过不计为通过证据。
- `audit-fixtures.sh` 只输出变量是否设置，不输出配置体、client secret 或真实端点。
