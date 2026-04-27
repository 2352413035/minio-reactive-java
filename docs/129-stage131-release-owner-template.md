# 阶段 131：正式发布负责人待填模板

## 用途

这份模板是给**发布负责人**直接填写的，不是 SDK 自动生成的最终发布结果。

它的目标只有一个：

> 把当前正式发布仍缺的 POM 元数据、发布插件、签名、SBOM、发布仓库、tag 和回滚策略，一次性收集完整。

填写完成前：

- 不改正式版本号
- 不写入猜测的 POM 元数据
- 不执行 `mvn deploy`
- 不创建 release tag

---

## 一、版本与发布目标

| 项目 | 待填写 |
| --- | --- |
| 目标发布版本 | `TBD` |
| 当前分支策略 | `master` = JDK8；`chore/jdk17-springboot3` = JDK17+ |
| 本次是否正式发布 | `TBD: yes/no` |
| 是否先发 RC / Beta | `TBD` |
| 对应 Git tag 命名规则 | `TBD` |
| 是否双分支同时打 tag | `TBD` |
| 是否要求 changelog freeze | `TBD` |

### 发布负责人确认

- [ ] 目标版本号已确认
- [ ] 是否需要 RC/Beta 已确认
- [ ] tag 命名规则已确认
- [ ] 双分支策略已确认

---

## 二、POM 元数据待填

### 1. 项目主页

| 字段 | 待填写 |
| --- | --- |
| `url` | `TBD` |

### 2. 许可证

| 字段 | 待填写 |
| --- | --- |
| `licenses.license.name` | `TBD` |
| `licenses.license.url` | `TBD` |
| `licenses.license.distribution` | `repo / manual / TBD` |
| 是否允许公开发布 | `TBD` |

### 3. SCM

| 字段 | 待填写 |
| --- | --- |
| `scm.url` | `TBD` |
| `scm.connection` | `TBD` |
| `scm.developerConnection` | `TBD` |
| `scm.tag` | `TBD` |

### 4. 开发者 / 组织

| 字段 | 待填写 |
| --- | --- |
| `developers[].name` | `TBD` |
| `developers[].email` | `TBD / omit` |
| `developers[].organization` | `TBD` |
| `developers[].organizationUrl` | `TBD` |
| `organization.name` | `TBD` |
| `organization.url` | `TBD` |

### 5. 问题跟踪

| 字段 | 待填写 |
| --- | --- |
| `issueManagement.system` | `GitHub / GitLab / Jira / TBD` |
| `issueManagement.url` | `TBD` |

### 6. 发布仓库

| 字段 | 待填写 |
| --- | --- |
| `distributionManagement.repository.id` | `TBD` |
| `distributionManagement.repository.url` | `TBD` |
| `distributionManagement.snapshotRepository.id` | `TBD` |
| `distributionManagement.snapshotRepository.url` | `TBD` |
| 是否发布到 Maven Central | `TBD` |
| 是否同时发布内部仓库 | `TBD` |

---

## 三、发布插件与产物策略

### 1. 必需插件

| 插件 | 是否启用 | 负责人说明 |
| --- | --- | --- |
| `maven-source-plugin` | `TBD` | `TBD` |
| `maven-javadoc-plugin` | `TBD` | `TBD` |
| `maven-gpg-plugin` | `TBD` | `TBD` |
| `cyclonedx-maven-plugin` | `TBD` | `TBD` |

### 2. 产物要求

| 产物 | 是否必须 | 备注 |
| --- | --- | --- |
| 主 jar | `yes/no` |
| sources jar | `yes/no` |
| javadoc jar | `yes/no` |
| `.asc` 签名 | `yes/no` |
| checksums | `yes/no` |
| SBOM | `yes/no` |
| 依赖清单 | `yes/no` |

### 3. Javadoc 策略

| 项目 | 待填写 |
| --- | --- |
| JDK8 分支 Javadoc 是否阻断发布 | `TBD` |
| JDK17+ 分支 Javadoc 是否阻断发布 | `TBD` |
| 中文注释编码策略 | `UTF-8 / TBD` |
| Javadoc warning 是否 fail build | `TBD` |

---

## 四、签名与凭证策略

| 项目 | 待填写 |
| --- | --- |
| 签名方式 | `GPG / CI KMS / other / TBD` |
| 密钥 ID | `TBD` |
| 密钥保管位置 | `TBD` |
| passphrase 注入方式 | `TBD` |
| CI secret 名称 | `TBD` |
| 本地 dry-run 是否允许签名 | `TBD` |
| 密钥轮换策略 | `TBD` |

### 安全确认

- [ ] 密钥不会写入仓库
- [ ] token / passphrase 不会写入文档
- [ ] CI secret 注入方式已确认
- [ ] 轮换/吊销流程已确认

---

## 五、SBOM 与合规策略

| 项目 | 待填写 |
| --- | --- |
| 是否必须生成 SBOM | `TBD` |
| SBOM 格式 | `CycloneDX / SPDX / TBD` |
| SBOM 输出位置 | `TBD` |
| 是否随 release artifact 一起上传 | `TBD` |
| 许可证扫描工具 | `TBD` |
| 第三方依赖审计要求 | `TBD` |

---

## 六、正式发布执行模板

### 负责人执行前检查

- [ ] `route parity` 报告最新且缺失/额外为 0
- [ ] `capability matrix` 最新
- [ ] `release-readiness` 最新
- [ ] JDK8 `mvn test` 通过
- [ ] JDK17 `mvn test` 通过
- [ ] JDK21/JDK25 `test-compile` 通过
- [ ] Crypto Gate 回归通过
- [ ] 破坏性 Admin 风险口径已确认
- [ ] 双分支工作区 clean

### 负责人可直接填写的执行记录

| 步骤 | 执行结果 | 备注 |
| --- | --- | --- |
| 写入 POM 元数据 | `TBD` | |
| 配置 source/javadoc/sign/SBOM | `TBD` | |
| 本地 dry-run | `TBD` | |
| CI dry-run | `TBD` | |
| release tag 创建 | `TBD` | |
| Maven 发布 | `TBD` | |
| 发布说明发布 | `TBD` | |

---

## 七、回滚与失败处理模板

| 场景 | 负责人填写 |
| --- | --- |
| tag 创建后但 deploy 失败，如何处理 | `TBD` |
| Maven Central / 私服发布失败，如何撤回 | `TBD` |
| 发现错误 POM 元数据，如何修复 | `TBD` |
| 发现错误签名或 SBOM，如何重发 | `TBD` |
| 双分支版本号不一致，如何回滚 | `TBD` |
| 是否需要紧急 hotfix 版本 | `TBD` |

---

## 八、最终批准栏

| 角色 | 姓名/标识 | 结论 |
| --- | --- | --- |
| 发布负责人 | `TBD` | `approve / reject / pending` |
| 安全负责人 | `TBD` | `approve / reject / pending` |
| 架构负责人 | `TBD` | `approve / reject / pending` |
| 运维/平台负责人 | `TBD` | `approve / reject / pending` |

---

## 九、推荐用法

如果你现在就要交给负责人，建议按下面顺序使用：

1. 先看 `docs/113-stage115-release-metadata-safe-prep.md`
2. 再填写本模板
3. 再对照 `docs/release-gates.md`
4. 最后回填 `docs/63-stage65-release-handoff.md` 的执行结果

这样可以避免“知道缺什么，但没有统一收集表”的问题。
