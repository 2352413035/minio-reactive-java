# 24 阶段 26 发布收口

## 1. 收口结论

阶段 26 给当前 SDK 一个可复查的发布候选口径：

1. **路由完整**：对照本地 `minio`，公开 HTTP 路由 233 / 233 已进入 SDK catalog。
2. **调用入口完整**：能力矩阵 `raw-fallback = 0`，所有 catalog 路由至少有 typed 或 advanced 兼容入口。
3. **常用路径可用**：对象存储、KMS、STS、Metrics、Health 和 Admin 安全只读/常见管理路径都有专用客户端入口。
4. **风险不伪装**：9 个 encrypted-blocked 和 29 个 destructive-blocked 继续显式暴露边界，不把高风险能力包装成普通成功路径。
5. **双分支一致**：JDK8 分支与 JDK17+ 分支共享相同 SDK 语义，高版本分支额外通过 JDK21/JDK25 compile。

阶段 26 不宣称“所有接口都已变成最终强类型模型”。它宣称的是：对标 MinIO 的路由、调用入口、文档口径、风险门禁和验证证据已经闭环，后续可以继续按产品 typed 成熟度迭代。

阶段 27 已在该候选口径之上补充 S3 ACL 与 Select typed 边界，因此当前 S3 product-typed 数字已提升到 72 / 77；其余结论不变。

## 2. 当前能力快照

能力快照以 `.omx/reports/capability-matrix.md` 为准：

| family | route-catalog | product-typed | advanced-compatible | raw-fallback | encrypted-blocked | destructive-blocked |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| s3 | 77 | 72 | 77 | 0 | 0 | 0 |
| admin | 128 | 43 | 128 | 0 | 9 | 29 |
| kms | 7 | 7 | 7 | 0 | 0 | 0 |
| sts | 7 | 4 | 7 | 0 | 0 | 0 |
| metrics | 6 | 6 | 6 | 0 | 0 | 0 |
| health | 8 | 8 | 0 | 0 | 0 | 0 |

## 3. 对用户的推荐说明

- 普通业务集成优先使用 `ReactiveMinioClient`。
- 管理端优先使用 `ReactiveMinioAdminClient` 的 L1/L2 typed 方法。
- 运维、临时凭证、指标和健康检查分别使用 KMS、STS、Metrics、Health 专用客户端。
- SDK 暂未产品化的新接口或特殊调用，可以使用 `ReactiveMinioRawClient` 兜底。
- 如果遇到 `EncryptedAdminResponse`，说明服务端返回的是 madmin 默认加密响应；这不是普通 JSON，必须等待 Crypto Gate Pass 后才能升级为明文 typed 解析。

## 4. 发布门禁状态

| 门禁 | 阶段 26 状态 |
| --- | --- |
| JDK8 单元测试 | 通过 |
| JDK8 真实 MinIO 集成测试 | 通过 |
| JDK17 单元测试 | 通过 |
| JDK17 真实 MinIO 集成测试 | 通过 |
| JDK21 compile | 通过 |
| JDK25 compile | 通过 |
| route parity | 233 / 233，missing 0，extra 0 |
| capability matrix | 双分支一致 |
| Crypto Gate | Gate Fail 边界已验证并加固 |
| destructive lab gate | 独立 lab 配置和共享环境拒绝逻辑已验证 |
| secret scan | 当前文件未写入用户提供的真实 MinIO 凭证 |
| `git diff --check` | 通过 |

## 5. 剩余风险

1. Crypto Gate 仍未 Pass，默认 madmin 加密响应不能明文 typed 解析。
2. destructive Admin 只能在独立 lab 验证，共享 MinIO 环境不应执行。
3. Admin product-typed 数量仍低于 route-catalog，因为很多接口需要环境、权限或长期字段稳定性确认。
4. STS 高级身份源（SSO、自定义 token、证书）仍需要独立身份源环境验证。
5. S3 advanced-compatible 中仍有部分低频子资源可以继续产品化。

## 6. 后续继续推进方式

下一轮计划不应再重复 route catalog 工作，而应优先提升产品 typed 成熟度：

1. S3 剩余高频能力：ACL、select、notification listen、object lambda/extract 等。
2. Admin L1/L2：policy attachment、IDP/LDAP/OpenID 只读配置、batch job/remote target 查询摘要。
3. STS：SSO、自定义 token、证书身份源测试环境与请求模型。
4. destructive lab：更多可回滚 fixture 和失败恢复流程。
5. Crypto Gate：完成依赖版本、许可证、安全公告、FIPS/Provider 与双分支测试矩阵审查。

## 7. 文档入口

- 总览：`README.md`
- 设计思想：`docs/04-minio-reactive-java-design.md`
- API 目录：`docs/09-minio-api-catalog.md`
- 加密边界：`docs/16-crypto-boundary-map.md`
- 发布就绪：`docs/17-release-readiness-report.md`
- 变更日志：`CHANGELOG.md`
