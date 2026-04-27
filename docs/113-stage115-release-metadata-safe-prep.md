# 阶段 115 发布元数据安全准备清单

## 结论

阶段 115 只做正式发布前的安全准备，不修改版本号、不打 tag、不发布 Maven，也不把未确认的负责人信息写入 `pom.xml`。

当前 SDK 功能状态已经进入发布候选口径：

- 主对标项目：同目录 `minio-java`。
- 对象存储核心 API：59 / 59 同名覆盖。
- Admin 核心 API：24 / 24 同名覆盖。
- `*Args` builder：86 / 86 同名覆盖。
- route parity：233 / 233。
- product-typed：S3 77 / 77，Admin 128 / 128，KMS 7 / 7，STS 7 / 7，Metrics 6 / 6，Health 8 / 8。
- raw fallback：0。
- Crypto Gate：阶段 111 起已 Pass，阶段 112 起提供显式 `secretKey` 便捷解密入口；正式发布时仍要回归验证。
- 破坏性边界：`destructive-blocked = 29` 是运维/lab 风险分类，不是公开 API 缺口。

正式发布仍不能继续推进的原因是：许可证、SCM、开发者、发布仓库、签名、SBOM 与 tag 策略都属于发布负责人决策，SDK 代码不能擅自伪造。

## 本阶段不做的事情

| 禁止项 | 原因 |
| --- | --- |
| 把版本号从 `0.1.0-SNAPSHOT` 改成正式版 | 版本号需要发布负责人确认语义、兼容性和回滚策略。 |
| 在 `pom.xml` 中写入猜测的许可证 | 许可证是法律决策，不能由代码自动选择。 |
| 写入假的 SCM、developer、organization 或发布仓库 | 错误元数据会污染 Maven Central 或内部仓库。 |
| 配置真实签名密钥或 CI secret | 密钥只能由负责人在安全环境注入。 |
| 发布 Maven、创建 Git tag、推送远端 | 这些都是不可逆或半不可逆发布动作。 |

## POM 当前机器预检结果

`python3 scripts/report-pom-release-metadata.py --worktree . --format markdown --output <报告路径>` 当前结论为：基础坐标齐全，但正式发布元数据和发布插件仍未补齐。

| 类别 | 当前状态 |
| --- | --- |
| `groupId` / `artifactId` / `version` / `name` / `description` | 已存在。 |
| `url` | 缺失，需要负责人确认项目主页。 |
| `licenses` | 缺失，需要负责人确认许可证名称、URL 和分发方式。 |
| `scm` / `scm.url` / `scm.connection` | 缺失，需要负责人确认公开或私有仓库地址。 |
| `developers` | 缺失，需要负责人确认开发者或组织维护者身份。 |
| `issueManagement` | 缺失，需要负责人确认问题跟踪地址。 |
| `organization` | 缺失，需要负责人确认组织名称和主页。 |
| `distributionManagement` | 缺失，需要负责人确认 Maven Central、内部 Nexus/Artifactory 或其他目标仓库。 |
| `maven-source-plugin` | 缺失，需要确认是否随正式发布生成源码包。 |
| `maven-javadoc-plugin` | 缺失，需要确认 Javadoc 失败策略、中文注释编码和发布要求。 |
| `maven-gpg-plugin` | 缺失，需要确认签名方式、密钥来源和 CI secret 管理。 |
| `cyclonedx-maven-plugin` | 缺失，需要确认 SBOM 格式、输出位置和发布归档策略。 |

## 发布负责人必须提供的输入

| 输入 | 需要回答的问题 | 没有输入时的处理 |
| --- | --- | --- |
| 许可证 | 使用哪种许可证？许可证 URL 是什么？是否允许发布到公开 Maven 仓库？ | 不写入 `licenses`，继续保持 SNAPSHOT。 |
| SCM | Git 仓库地址、只读 connection、开发者 connection、tag 命名规则是什么？ | 不写入 `scm`，不创建 tag。 |
| 开发者或组织 | POM 中展示个人还是组织？是否允许公开邮箱？ | 不写入 `developers` / `organization`。 |
| 问题跟踪 | 使用 GitHub Issues、GitLab Issues、Jira 还是内部系统？ | 不写入 `issueManagement`。 |
| 发布仓库 | 发布到 Maven Central、内部仓库还是两者都发布？snapshot 与 release 仓库地址分别是什么？ | 不写入 `distributionManagement`，不 deploy。 |
| 签名 | 使用 GPG 还是 CI 托管签名？密钥 ID、passphrase、secret 名称和轮换策略是什么？ | 不启用签名 profile。 |
| SBOM | 是否必须生成 CycloneDX？是否要随 release artifact 一起发布？ | 不启用 SBOM 发布 profile。 |
| Javadoc | JDK8 与 JDK17+ 是否使用同一 Javadoc 策略？中文注释编码失败是否阻断发布？ | 不启用 javadoc 发布 profile。 |
| 版本号 | 下一版是 `0.1.0`、`0.1.0-RC1` 还是继续 SNAPSHOT？ | 保持 `0.1.0-SNAPSHOT`。 |
| 回滚 | 发布失败后如何撤回、作废 tag、修复版本号？ | 不执行正式发布。 |

## 负责人确认后的最小实施顺序

1. 先在两个分支分别生成 POM 元数据报告并归档到 `.omx/reports/`。
2. 只写入负责人确认过的 POM 元数据，不使用占位符或猜测值。
3. 添加 release profile 时默认不自动 deploy；先验证 `mvn -Prelease -DskipTests package` 或等价 dry-run。
4. 在 JDK8 分支运行 `mvn -q -DfailIfNoTests=true test`。
5. 在 JDK17+ 分支运行 `mvn -q -DfailIfNoTests=true test`。
6. 在 JDK21、JDK25 上运行 `mvn -q -DskipTests test-compile`。
7. 运行 Crypto Gate 回归：`scripts/madmin-fixtures/check-crypto-gate.sh`。
8. 运行能力和边界报告：minio-java parity、signature parity、route parity、capability matrix、POM 元数据、destructive boundary。
9. 确认工作区干净后，才允许创建 release tag 或执行 deploy。

## 与当前门禁的关系

- Crypto Gate 不再是未实现阻塞项；它已经是必须持续回归的安全能力。
- `destructive-blocked = 29` 不等于 SDK API 未实现；它要求独立 lab 或维护窗口证据来证明真实运维风险可控。
- POM 发布元数据缺失是真正阻止正式 Maven/tag 发布的剩余发布工程问题。
- 在发布负责人确认前，最安全的版本策略仍是 `0.1.0-SNAPSHOT`。

## 可直接交付负责人的模板

阶段 131 起，可以直接把 `docs/129-stage131-release-owner-template.md` 交给发布负责人填写。
它把版本号、POM 元数据、发布插件、签名、SBOM、仓库、tag 和回滚策略整理成一份可复制模板，避免继续口头来回确认。

## 本阶段验证命令

本阶段是文档和发布准备清单变更；验证时仍按发布候选口径跑完整回归，避免文档口径与脚本产物脱节：

```bash
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH \
mvn -q -DfailIfNoTests=true test

JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
mvn -q -DfailIfNoTests=true test

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH \
mvn -q -DskipTests test-compile

JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 \
PATH=/usr/lib/jvm/java-25-openjdk-amd64/bin:$PATH \
mvn -q -DskipTests test-compile

scripts/madmin-fixtures/check-crypto-gate.sh
python3 scripts/report-pom-release-metadata.py --worktree . --format markdown --output <报告路径>
python3 scripts/report-destructive-boundary.py --worktree . --format markdown --output <报告路径>
git diff --check
```
