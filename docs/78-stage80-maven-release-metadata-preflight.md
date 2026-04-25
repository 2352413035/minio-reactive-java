# 阶段 80：Maven 发布元数据预检

## 1. 本阶段目标

阶段 80 只做 Maven 发布元数据预检，不修改版本号、不发布 Maven、不打 tag，也不擅自补许可证或发布仓库信息。

新增脚本：

```text
scripts/report-pom-release-metadata.py
```

它会审计双分支 `pom.xml` 中的发布元数据和发布插件，输出 markdown/json 报告。脚本只读取 POM，不修改文件。

## 2. 当前预检结论

当前两个分支都具备基础坐标：

- `groupId`
- `artifactId`
- `version`
- `name`
- `description`

当前两个分支都缺少正式发布常见材料：

- `url`
- `licenses`
- `scm`
- `developers`
- `issueManagement`
- `organization`
- `distributionManagement`
- `maven-source-plugin`
- `maven-javadoc-plugin`
- `maven-gpg-plugin`
- `cyclonedx-maven-plugin` 或等价 SBOM 方案

这些缺失项并不影响当前 `0.1.0-SNAPSHOT` 本地开发和测试，但会阻塞正式 Maven 发布。

## 3. 为什么不直接补齐

部分元数据必须由项目负责人确认，开发代理不能擅自决定：

- 许可证类型和许可证 URL。
- 对外公开仓库 URL、SCM connection、issue 管理地址。
- developers / organization 是否公开以及使用哪个身份。
- 发布仓库、签名密钥、staging 策略和撤回策略。
- SBOM 格式、签名要求和制品校验要求。

因此本阶段只形成可执行缺口报告，不把未确认信息写入 POM。

## 4. 报告生成命令

```bash
python3 scripts/report-pom-release-metadata.py \
  --worktree . \
  --format markdown \
  --output ../.omx/reports/pom-release-metadata-jdk8.md
```

在项目根目录统一生成双分支报告时使用：

```bash
python3 minio-reactive-java/scripts/report-pom-release-metadata.py \
  --worktree minio-reactive-java \
  --worktree minio-reactive-java-jdk17 \
  --format markdown \
  --output .omx/reports/pom-release-metadata.md

python3 minio-reactive-java/scripts/report-pom-release-metadata.py \
  --worktree minio-reactive-java \
  --worktree minio-reactive-java-jdk17 \
  --format json \
  --output .omx/reports/pom-release-metadata.json
```

## 5. 正式发布前的下一步

发布负责人确认元数据后，再按以下顺序进入实现：

1. 确认许可证与公开仓库信息。
2. 补 `url`、`licenses`、`scm`、`developers`、`issueManagement` 等 POM 元数据。
3. 选择 source/javadoc/sign/SBOM 插件方案。
4. 双分支执行 `mvn test package`、JDK21/JDK25 `test-compile`、live 安全测试和凭据扫描。
5. 只有 Crypto Gate 与独立 lab 也具备证据后，才允许讨论正式 tag/Maven 发布。

## 6. 本阶段不做的事

- 不把 `0.1.0-SNAPSHOT` 改成正式版本。
- 不添加签名密钥、发布仓库或真实负责人身份。
- 不发布 Maven。
- 不降低 `encrypted-blocked` 或 `destructive-blocked`。
