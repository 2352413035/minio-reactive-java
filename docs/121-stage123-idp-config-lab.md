# 阶段 123：IDP 配置 独立 lab 补证

阶段 123 使用一次性 Docker MinIO 和临时 OpenID discovery/JWKS 夹具，补齐 `ADMIN_ADD_IDP_CONFIG`、`ADMIN_UPDATE_IDP_CONFIG`、`ADMIN_DELETE_IDP_CONFIG` 的 typed/raw 真实执行证据。

## 为什么必须有 OIDC 夹具

MinIO 服务端在新增或更新 OpenID IDP 配置时会读取 `config_url`，并继续请求 discovery 文档中的 `jwks_uri`。因此这类接口不能只靠离线 KV 文本验证，也不能在共享 MinIO 上随意执行；必须给独立 lab 提供一个 MinIO 容器能访问的 discovery/JWKS 服务。

阶段 123 的临时夹具只返回最小 discovery JSON 和空 JWKS 列表，用来证明服务端能够完成配置校验。dummy `client_secret` 只写入 `/tmp` 运行目录，不写入仓库。

## 非动态配置重启语义

IDP 配置属于 MinIO 非动态身份源配置。`add/update/delete` 返回成功后，配置已经写入服务端配置存储，但内存中的身份源索引通常需要重启后才会刷新。

因此 `DestructiveAdminIntegrationTest` 新增了可选开关：

```properties
MINIO_LAB_RESTART_IDP_AFTER_CONFIG_CHANGE=true
MINIO_LAB_DOCKER_NAME=minio-reactive-idp-config-lab
```

打开后，IDP 实验矩阵会在 add、delete、raw add 后重启一次性 Docker lab，并等待 `/minio/health/ready`，再做下一步读取或更新。这样验证的是 MinIO 真实语义，而不是把“需要重启”误判成 SDK 调用失败。

## 新增脚本

新增脚本：

```bash
scripts/minio-lab/run-idp-config-lab.sh
```

它会自动：

1. 启动一次性 Docker MinIO；
2. 在本机启动临时 OIDC discovery/JWKS HTTP 夹具；
3. 生成 `/tmp` 私有 IDP add/update 配置体；
4. 设置 `MINIO_LAB_RESTART_IDP_AFTER_CONFIG_CHANGE=true`；
5. 执行 `DestructiveAdminIntegrationTest` 并生成步骤报告。

如果 Docker 容器无法访问自动选择的 OIDC 主机地址，可通过 `MINIO_LAB_OIDC_HOST` 覆盖。

## 本阶段证据

本阶段 JDK8/JDK17 都验证了以下步骤：

- typed `addIdpConfigEntry`
- typed `getIdpConfigInfo` reload 后读取配置
- typed `updateIdpConfig`
- raw `ADMIN_UPDATE_IDP_CONFIG`
- raw `ADMIN_DELETE_IDP_CONFIG`
- raw `ADMIN_ADD_IDP_CONFIG`
- raw add reload 后 typed get/list 复核
- finally typed delete 恢复

证据文件：

- JDK8：`.omx/reports/stage123-jdk8-idp-config-lab.md`
- JDK17：`.omx/reports/stage123-jdk17-idp-config-lab.md`

## 边界变化

阶段 122 后破坏性 Admin 统计为：

- 总数：29
- 已有独立 lab 证据：16
- 仍需证据：13

阶段 123 后：

- 总数：29
- 已有独立 lab 证据：19
- 仍需证据：10
- 拓扑或身份提供方剩余：3
- 维护窗口剩余：5
- 资源压测剩余：2

这不代表 IDP 配置可以在共享环境随意执行。SDK 只是证明了在独立、可重建、可重启的 lab 中，专用 Admin 客户端和 raw 兜底路径都能按 MinIO 真实协议工作。
