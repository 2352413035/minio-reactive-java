# 阶段 78：发布工程预检与继续暂缓发布结论

## 1. 本阶段定位

阶段 78 只做发布工程预检，不修改 SDK API、不修改 `pom.xml` 版本号、不打 tag、不发布 Maven。

当前 SDK 的功能入口已经闭环：

- route parity：233 / 233。
- product-typed：233 / 233。
- raw-fallback：0。

但正式发布还不能放行，因为仍有两个外部门禁没有通过：

- `encrypted-blocked = 11`：Crypto Gate 仍是 Fail，没有三方批准和依赖审查证据。
- `destructive-blocked = 29`：没有独立、可回滚 lab 的 typed/raw 破坏性矩阵报告。

因此本阶段结论是：继续保持 `0.1.0-SNAPSHOT`，不做正式 tag 或 Maven 发布。

## 2. 发布候选与正式发布的区别

| 项目 | 当前发布候选 | 正式发布前必须补齐 |
| --- | --- | --- |
| 路由对标 | 已完成，233 / 233 | 发布前重新生成 route parity 报告。 |
| 产品入口 | 已完成，233 / 233 | 发布前重新生成 capability matrix。 |
| raw 兜底 | `raw-fallback = 0` | 保持 raw 只做兜底，不替代专用客户端。 |
| Crypto Gate | Fail，保留 `EncryptedAdminResponse` | 三方批准、依赖/许可证/安全审查、Go madmin fixture、四 JDK 验证。 |
| 破坏性 lab | 门禁和准备度审计已完成 | 独立可回滚 lab 真实执行 typed/raw 矩阵并生成无凭证报告。 |
| 版本号 | `0.1.0-SNAPSHOT` | 确认目标版本，更新双分支版本号并打 tag。 |
| Maven 发布 | 未执行 | 源码包、javadoc、pom 元数据、签名、校验和、许可证/SBOM、回滚策略。 |

## 3. 当前可以安全执行的发布预检

这些预检不需要写入 MinIO，也不会把外部门禁伪装成通过：

1. 双分支全量单元测试。
2. 双分支真实 MinIO 安全 live 测试。
3. JDK17+ 分支在 JDK21/JDK25 下 `test-compile`。
4. route parity 与 capability matrix 重新生成。
5. Crypto Gate Fail 检查。
6. 破坏性 lab 无环境变量和共享端点拒绝检查。
7. `audit-readiness.sh` 准备度审计。
8. `git diff --check`、变更文件凭据扫描、双分支同步检查。
9. 本地 `package` 预检，但不能上传仓库、不能签发正式版本。

## 4. 正式发布前的不可替代证据

正式发布必须拿到以下证据后才能继续：

### 4.1 Crypto Gate Pass 证据

- owner/security/architect 三方批准。
- Argon2id / ChaCha20 依赖候选版本、许可证、安全公告、维护活跃度记录。
- JDK8 与 JDK17+ 的依赖差异说明。
- Go `madmin-go` fixture 与 Java 解析结果互操作证据。
- Gate Pass 后仍保留 `EncryptedAdminResponse` 回退语义。

### 4.2 独立破坏性 lab 证据

- `scripts/minio-lab/audit-readiness.sh` 通过。
- `scripts/minio-lab/run-destructive-tests.sh` 真实执行。
- 报告中包含 typed 专用客户端和 raw catalog 双路径结果。
- 报告中包含恢复提示、执行步骤和 PASS/FAIL，但不包含凭证、token、签名或请求体。
- 任何失败都必须先恢复环境，再讨论是否重跑。

### 4.3 Maven/tag 发布证据

- 双分支工作区干净。
- 版本号从 `0.1.0-SNAPSHOT` 切换到明确目标版本。
- `CHANGELOG.md`、README、发布说明和已知限制同步。
- 源码包、javadoc 包、pom 元数据、签名、校验和、许可证或 SBOM 准备完毕。
- tag、回滚 tag、撤回或补丁发布策略已记录。

## 5. 本阶段结论

当前项目适合作为“功能入口完整、风险边界清楚”的发布候选继续验证，但不适合做正式 Maven/tag 发布。

后续如果没有外部 Crypto/lab 证据，应继续做非破坏性的成熟度工作，例如：

- 结果模型深化。
- 中文错误解释与示例增强。
- 只读 `mc` / live 旁证扩展。
- 发布材料预检和版本管理同步。

不能做的事：

- 不能用 route/product-typed 满格替代 Crypto Gate Pass。
- 不能用共享 MinIO 替代独立破坏性 lab。
- 不能在 `0.1.0-SNAPSHOT` 未冻结前打正式 tag。
