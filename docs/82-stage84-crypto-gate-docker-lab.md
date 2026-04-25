# 阶段 84：Crypto Gate 解释与 Docker 独立破坏性 lab 证据

本阶段回应两个问题：

1. Crypto Gate 到底挡住了哪些功能，MinIO 自己是怎么加密/解密的，我们是否一定可以解密。
2. 在本机 Docker 中启动一个不占用共享端口的独立 MinIO lab，真实执行可回滚破坏性测试，并同时验证专用 typed 客户端和 `ReactiveMinioRawClient` 兜底路径。

## 1. Crypto Gate 是什么

Crypto Gate 不是一个 MinIO 服务端开关，而是 SDK 内部的发布门禁：只要真实 MinIO 返回的是 madmin 加密载荷，而当前 Java SDK 还不能安全、完整、可审计地解密它，就不能把该接口伪装成普通 JSON 或明文 typed 模型。

当前被 Crypto Gate 约束的是 **敏感 Admin 响应或敏感 Admin 写入体**。这些能力本身已经有接口入口，但默认响应必须先停在 `EncryptedAdminResponse` 边界上。

## 2. 当前涉及的功能

当前能力矩阵里统计的加密响应边界为 11 个：

| catalog 名称 | SDK 入口 | 说明 |
| --- | --- | --- |
| `ADMIN_LIST_USERS` | `listUsersEncrypted()` | 用户列表包含敏感 IAM 信息，MinIO 用 madmin 加密返回。 |
| `ADMIN_GET_CONFIG` | `getConfigEncrypted()` | 全量配置可能含 secret、token、远端目标等敏感值。 |
| `ADMIN_GET_CONFIG_KV` | `getConfigKvEncrypted(key)` | 单个配置子系统可能含敏感值。 |
| `ADMIN_LIST_CONFIG_HISTORY_KV` | `listConfigHistoryKvEncrypted(...)` | 配置历史可能含旧 secret。 |
| `ADMIN_LIST_IDP_CONFIG` | `listIdpConfigsEncrypted(type)` | IDP 配置读取属于敏感响应。 |
| `ADMIN_GET_IDP_CONFIG` | `getIdpConfigEncrypted(type, name)` | 单个 IDP 配置读取属于敏感响应。 |
| `ADMIN_ADD_SERVICE_ACCOUNT` | `addServiceAccount(...)` | 新建服务账号会返回敏感凭证。 |
| `ADMIN_INFO_SERVICE_ACCOUNT` | `getServiceAccountInfoEncrypted(...)` | 服务账号详情可能含敏感字段。 |
| `ADMIN_LIST_SERVICE_ACCOUNTS` | `listServiceAccountsEncrypted()` | 服务账号列表属于敏感响应。 |
| `ADMIN_INFO_ACCESS_KEY` | `getAccessKeyInfoEncrypted(...)` | access key 详情属于敏感响应。 |
| `ADMIN_LIST_ACCESS_KEYS_BULK` | `listAccessKeysEncrypted(...)` | 批量 access key 信息属于敏感响应。 |

另外，配置写入、IDP 写入、tier 写入、remote target 写入、站点复制等部分 Admin 写入接口在 MinIO 服务端会要求请求体先用 madmin 兼容格式加密。这类写入不是“加密响应边界”，但同样需要正确处理加密格式。

## 3. MinIO 项目中是怎么处理的

对照本地 `minio` 源码和 `madmin-go`：

- 服务端敏感响应会调用 `madmin.EncryptData(password, data)` 后再返回。
- 服务端敏感写入体会调用 `madmin.DecryptData(password, body)` 解析请求。
- 加密数据格式是：`32 字节 salt + 1 字节算法 ID + 8 字节 nonce + DARE/SIO 加密数据`。
- 算法 ID 目前包括：
  - `0x00`：Argon2id + AES-GCM。
  - `0x01`：Argon2id + ChaCha20-Poly1305。
  - `0x02`：PBKDF2 + AES-GCM，主要用于 FIPS 场景。
- 大多数敏感响应的 `password` 来自当前请求凭证的 secret key；有些站点复制、IAM 存储或内部流程会使用 active/global credential 或专门传入的 encryption key。

所以这里的“解密”不是拿到响应就一定能解，而是必须同时满足密钥、算法、格式和依赖条件。

## 4. 我们解密需要什么条件

要把 `EncryptedAdminResponse` 升级为明文 typed 模型，至少需要：

1. **正确的 password/key**：通常是发起该 Admin 请求所用凭证的 secret key；如果凭证不匹配、凭证轮换、响应来自特殊内部流程，就会解密失败。
2. **支持服务端选择的算法**：当前 Java 代码已经能识别三种算法，也能解密 PBKDF2 + AES-GCM；但默认 MinIO 非 FIPS 场景通常会走 Argon2id + AES-GCM 或 Argon2id + ChaCha20-Poly1305，这两个当前只识别、不解密。
3. **经过批准的 crypto 依赖**：Argon2id、ChaCha20-Poly1305、SIO/DARE 流式格式需要选型、许可证审查、安全审查，并同时兼容 JDK8 与 JDK17+ 分支。
4. **Go fixture 互操作证据**：必须证明 Go `madmin.EncryptData` 产生的真实夹具可被 Java 解密，Java 产生的写入加密体也能被 MinIO 接受。
5. **失败回退语义**：即使未来 Gate Pass，遇到未知算法、错误 secret、损坏响应或未来格式，也必须回退到安全诊断，不能泄露 secret、token 或半截明文。

结论：**不是一定可以解密**。MinIO 服务端一般不需要额外打开某个“加密响应配置”才能返回这些载荷；但客户端想解密，必须有正确 secret、算法支持和通过审查的实现。当前 SDK 的正确行为是：能识别并解释加密响应，不把默认 Argon2id/ChaCha20 响应伪装成明文。

## 5. 本阶段 Docker 独立 lab 怎么做

本阶段按用户要求启动了一个新的 Docker MinIO，不占用已有共享 MinIO 的 `9000/9001`：

| 项目 | 值 |
| --- | --- |
| 容器名 | `minio-reactive-destructive-lab` |
| 镜像 | `minio/minio` |
| API 端口 | `http://127.0.0.1:19000` |
| Console 端口 | `http://127.0.0.1:19001` |
| 测试 bucket | `reactive-lab-bucket` |
| 凭证处理 | 只写入 `/tmp` 运行时临时文件，不进入仓库、不进入报告。 |

执行前发现并修复了两个真实问题：

1. `run-destructive-tests.sh` 原先把 `MINIO_LAB_ENDPOINT` 复制到通用 `MINIO_ENDPOINT`，导致用例内部再次跑 `verify-env.sh` 时误判“lab 与共享环境相同”。现在破坏性测试只读取 `MINIO_LAB_*`，不会污染通用 live 环境变量。
2. bucket quota 示例使用了错误字段 `type`。对照 madmin-go 的 `BucketQuota`，真实字段应为 `quotatype`，已修正示例与 README。

同时，本阶段把 config KV 和 bucket quota 的破坏性测试升级为 **typed + raw 双路径**：

- config KV：先用 `ReactiveMinioAdminClient` 写入/探测，再用 `ReactiveMinioRawClient` 通过 `ADMIN_SET_CONFIG_KV` / `ADMIN_HELP_CONFIG_KV` 兜底验证；raw 写入体显式用 lab secret 构造 madmin 加密体。
- bucket quota：先用 `ReactiveMinioAdminClient` 写入/读取，再用 `ReactiveMinioRawClient` 通过 `ADMIN_SET_BUCKET_QUOTA` / `ADMIN_GET_BUCKET_QUOTA` 兜底验证。
- finally 中同时保留 raw 恢复和 typed 恢复，避免某一路失败后资源残留。

## 6. 本阶段验证证据

JDK8 分支：

```text
DestructiveAdminIntegrationTest: Tests run: 6, Failures: 0, Errors: 0, Skipped: 2
报告：/tmp/minio-reactive-stage84-jdk8-reports/destructive-lab-stage84-jdk8-raw-*.md
```

JDK17+ 分支：

```text
DestructiveAdminIntegrationTest: Tests run: 6, Failures: 0, Errors: 0, Skipped: 2
报告：/tmp/minio-reactive-stage84-jdk17-reports/destructive-lab-stage84-jdk17-raw-*.md
```

本阶段实际 PASS 的 typed/raw 步骤包括：

| 范围 | 已验证步骤 |
| --- | --- |
| config | typed set/get-help、raw set/get-help、raw restore、typed restore。 |
| bucket quota | typed set/get、raw set/get、raw restore、typed restore。 |
| remote target 探测 | typed list、raw list。 |

仍然跳过的 2 类用例是：

1. tier/remote target 真实写入夹具。
2. batch job/site replication 实验矩阵。

它们需要额外的远端存储、复制目标或 batch/site 私有请求体，不应该在没有夹具的情况下伪造通过。因此本阶段只能说明 Docker 独立 lab 已经证明 **config、bucket quota 和 remote target 只读探测** 的 typed/raw 路径可用；不能把全部 29 个破坏性边界一次性清零。

## 7. 对后续计划的影响

- `destructive-blocked = 29` 不应直接降为 0，因为还有 tier 写入、remote target 写入、batch job、site replication 等高风险矩阵未提供完整夹具。
- 现在可以把独立 Docker lab 固定为后续破坏性验证的默认方式：每次需要破坏性写入，就新起独立容器或容器组，完成后删除。
- Crypto Gate 仍保持 Fail：本阶段没有新增 Argon2id/ChaCha20 解密依赖，也没有把 `EncryptedAdminResponse` 改成明文模型。
