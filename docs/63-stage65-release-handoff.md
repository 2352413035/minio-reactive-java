# 阶段 65：发布说明草案与阻塞交接

## 1. 本阶段定位

阶段 65 不新增 API、不改变版本号、不打 tag、不发布 Maven。它把阶段 64 的外部门禁进一步整理成“可以交给发布、运维、安全和后续开发继续执行”的交接材料，避免后续把已完成的 SDK 对标工作和仍需外部环境/审批的工作混在一起。

当前最准确的状态是：

- SDK 入口对标已经闭环：route parity 233 / 233。
- 产品化入口已经闭环：S3、Admin、KMS、STS、Metrics、Health 均有专用客户端或明确边界。
- `ReactiveMinioRawClient` 仍是新增接口、特殊请求和 catalog 兜底调用器，不替代专用客户端。
- 当前仍是 `0.1.0-SNAPSHOT` 发布候选口径，不是正式 1.0。
- `destructive-blocked = 29` 仍是风险计数，不允许通过文档口径“抹掉”；但阶段 129 已明确：剩余 4 个接口按发布策略降级/隐藏，不再作为普通用户主路径阻塞。

## 2. 对外发布说明草案

如果需要对外描述当前版本，可以使用下面口径：

> minio-reactive-java 已对照本地 MinIO 服务端公开路由完成响应式 SDK 入口覆盖。SDK 提供平级专用客户端：`ReactiveMinioClient` 负责对象存储，`ReactiveMinioAdminClient` 负责管理端，`ReactiveMinioKmsClient` 负责 KMS，`ReactiveMinioStsClient` 负责 STS，`ReactiveMinioMetricsClient` 负责监控，`ReactiveMinioHealthClient` 负责健康检查；`ReactiveMinioRawClient` 作为 catalog 兜底调用器保留。当前版本适合作为功能入口和风险边界闭环的发布候选继续验证，正式发布前还需要完成 Crypto Gate、独立破坏性 lab 和 Maven/tag 发布工程材料。

对外说明中必须同时写明：

1. 这是 `0.1.0-SNAPSHOT`，不是最终正式版。
2. Crypto Gate Pass 前，默认 madmin 加密响应继续返回 `EncryptedAdminResponse`。
3. 破坏性 Admin 写入能力只能在独立、可回滚 lab 中验证，不能在共享 MinIO 环境默认执行。
4. 用户常规集成应优先使用专用客户端；只有 SDK 尚未提供更细入口、或需要特殊请求时，才使用 raw 兜底。
5. `ADMIN_SERVER_UPDATE*` 仅按高级/维护窗口接口处理；`ADMIN_SR_PEER_*` 默认不在普通用户主文档暴露。

## 3. 阻塞交接表

| 事项 | 当前状态 | 需要谁继续推进 | 完成标准 |
| --- | --- | --- | --- |
| Crypto Gate Pass | 未放行 | 业务负责人、安全审查、架构审查 | 三方批准、依赖/许可证/安全公告/FIPS/Provider 审查通过，双分支 JDK8/JDK17/JDK21/JDK25 测试通过，Go madmin fixture 与 Java 结果互操作。 |
| 独立破坏性 lab | 未配置 | 运维或测试环境负责人 | `verify-env.sh` 通过，确认不是共享端点，具备恢复方案，typed/raw 破坏性矩阵运行并生成无凭证报告。 |
| Maven/tag 发布工程 | 未执行 | 发布负责人 | 版本号确定，源码包/javadoc/pom/签名/校验和/许可证或 SBOM/发布说明/回滚策略齐全，双分支工作区干净。 |
| 结果模型深化 | 可继续 | SDK 开发 | 在不重复 catalog、不降低风险门禁的前提下，把稳定 JSON/text 响应继续拆成更细中文业务模型。 |
| 只读旁证扩展 | 可继续 | SDK 开发或测试 | 使用 `mc`、服务端只读接口和 live smoke 增加证据，但不写凭证、不执行共享环境破坏性操作。 |

## 4. 后续执行顺序建议

如果外部环境和审批暂时不可用，后续只做不改变风险状态的工作：

1. 继续补结果模型和中文错误解释。
2. 继续补示例、README、排障文档。
3. 继续运行 route/capability、单元测试、live 安全测试和 `mc` 只读旁证。
4. 保持 `encrypted-blocked` 与 `destructive-blocked` 计数不变。

如果独立 lab 已经准备好，优先执行：

1. 设置本机私有 `scripts/minio-lab/lab.properties` 或环境变量。
2. 运行 `scripts/minio-lab/verify-env.sh`。
3. 运行 `scripts/minio-lab/run-destructive-tests.sh`。
4. 检查报告中 typed/raw 步骤均有 PASS/FAIL、异常类型和恢复提示，且不包含凭证、token、签名或请求体。
5. 只有真实报告通过后，才能讨论减少 `destructive-blocked`。

如果 Crypto Gate 获得三方批准，优先执行：

1. 固化批准记录和依赖候选版本。
2. 添加最小必要依赖，并解释 JDK8 与 JDK17+ 分支维护方式。
3. 使用 Go madmin fixture 生成互操作证据。
4. 运行全 JDK 矩阵和 live 安全测试。
5. 更新 crypto 文档后，才能讨论减少 `encrypted-blocked`。

## 5. 发布负责人可直接复用的检查清单

正式发布前逐项勾选：

- [ ] `docs/10-version-management.md` 已确认目标版本号和双分支策略。
- [ ] `CHANGELOG.md` 已写明能力、限制和升级/回滚说明。
- [ ] `.omx/reports/route-parity-jdk8.md` 与 `.omx/reports/route-parity-jdk17.md` 均为缺失 0、额外 0。
- [ ] `.omx/reports/capability-matrix-jdk8.md` 与 `.omx/reports/capability-matrix-jdk17.md` 均显示产品入口满格、`raw-fallback = 0`。
- [ ] Crypto Gate 状态已从 Fail 变为 Pass，且有审查证据。
- [ ] 独立破坏性 lab 报告已生成并完成恢复确认。
- [ ] JDK8/JDK17 单元测试与真实 MinIO live 测试通过。
- [ ] JDK21/JDK25 `test-compile` 通过。
- [ ] 源码包、javadoc 包、pom 元数据、签名、校验和、许可证或 SBOM 已准备。
- [ ] 文档和报告不包含真实凭证、token、签名或敏感请求体。
- [ ] tag 前两个工作区均干净，提交信息符合 Lore Commit Protocol。

## 6. 本阶段验证边界

阶段 65 只验证交接材料和状态一致性，不把外部门禁伪装成已通过。可接受的验证包括：

- 文档同步检查。
- `git diff --check`。
- 真实凭证扫描。
- route/capability 报告重新生成或复核。
- 非破坏性编译/测试。
- lab 门禁拒绝共享端点。
- Crypto Gate Fail 继续保持。

如果任何验证发现真实 API、专用客户端入口或 catalog 缺口，应回到实现阶段修复；如果只剩 Crypto/lab/发布工程，则继续按本交接表推进。
