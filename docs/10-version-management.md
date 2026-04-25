# 10 版本管理说明

本项目维护两条并行 SDK 线：一条兼容 JDK8，一条面向 JDK17 及以上版本。

## 分支与工作区

| 分支 / 工作区 | Java 基线 | 用途 |
| --- | --- | --- |
| `master`，路径 `/dxl/minio-project/minio-reactive-java` | JDK8 | 面向仍需 Java 8 的用户。源码必须避免 Java 9 及以上才有的语言特性和标准库 API。 |
| `chore/jdk17-springboot3`，路径 `/dxl/minio-project/minio-reactive-java-jdk17` | JDK17+ | 面向 Spring Boot 3 和较新 Java 运行时。除构建基线不同外，SDK 行为应尽量与 `master` 保持一致。 |

## 日常开发流程

1. 默认先在 `master` 上实现功能，除非该改动只适用于 JDK17+。
2. 将源码、测试和文档同步到 `chore/jdk17-springboot3`。
3. 在 `master` 上使用 JDK8 跑单元测试和必要的真实 MinIO 集成测试。
4. 在 `chore/jdk17-springboot3` 上使用 JDK17 跑单元测试和必要的真实 MinIO 集成测试。
5. 如果改动范围较大，还要在 JDK17+ 分支上用 JDK21、JDK25 做编译验证。
6. 两条分支分别提交，提交信息使用 Lore Commit Protocol，说明约束、取舍、验证和未验证内容。
7. 未经明确要求，不主动 push 到远端。

## 当前本地版本检查点

JDK8 分支：

- `3bb1b32`：核心 S3 reactive SDK 能力检查点。
- `5053ca6`：MinIO 公开接口目录和原始响应式执行器检查点。
- `c9ddb1e`：强业务客户端基础检查点，已完成 catalog/raw 兜底、平级专用客户端、第一批 typed 模型、madmin PBKDF2/AES-GCM 写入方向和双分支验证。
- `9bbbae4`：阶段 51 独立 lab 门禁复核检查点。
- `e937e15`：阶段 52 发布复审与版本管理刷新检查点。
- `445818c`：阶段 53 Admin 维护操作产品边界检查点。
- `cfbaad2`：阶段 54 Admin 策略与复制轻量边界检查点。
- `454c17e`：阶段 55 Admin 配置高风险边界检查点。
- `1ce711f`：阶段 56 站点复制 peer lab-only 边界检查点。
- `a90ffe3`：阶段 57 服务类强破坏性边界检查点。
- `e789009`：阶段 58 Crypto/lab 阻塞复核检查点。
- `1fc6e39`：阶段 59 剩余 Admin 高风险产品边界检查点。
- `853f65f`：阶段 60 发布候选外部门禁复审检查点。
- `4d6604a`：阶段 61 mc 只读发布候选证据检查点。
- `d45e9bc`：阶段 62 用户面发布指南检查点。
- `6f4ec7b`：阶段 63 最终缺口审计检查点。
- `da74625`：阶段 64 正式发布前外部门禁检查点。
- `5895350`：阶段 65 发布候选交接边界检查点。
- `6d356ef`：阶段 66 只读 Admin 状态结果模型检查点。
- `bc7adb0`：阶段 67 只读 Admin 诊断摘要模型检查点。
- `5906894`：阶段 68 站点复制只读摘要模型检查点。
- `29db788`：阶段 69 Admin pool 只读摘要模型检查点。
- `6bd61cf`：阶段 70 batch job 只读状态摘要检查点。
- `1a57ee1`：阶段 71 replication MRF 只读摘要检查点。
- `f43a482`：阶段 72 IDP 配置加密边界检查点。
- `a64ffc2`：阶段 73 加密响应安全诊断检查点。
- `8c63bd8`：阶段 74 IDP 加密边界统计修正检查点。
- `3e6ae44`：阶段 75 新口径缺口审计检查点。
- `c0c3f50`：阶段 76 Crypto Gate 放行清单检查点。
- `21703d9`：阶段 77 破坏性 lab 准备度门禁检查点。

JDK17+ 分支：

- `7303e7e`：JDK17 线同步核心 S3 reactive SDK 能力检查点。
- `b9073fc`：JDK17 线同步 MinIO 公开接口目录和原始响应式执行器检查点。
- `7c598a1`：JDK17+ 线同步强业务客户端基础检查点，语义应与 JDK8 线保持一致。
- `f093021`：阶段 51 独立 lab 门禁复核检查点。
- `1d79d3e`：阶段 52 发布复审与版本管理刷新检查点。
- `da12ef7`：阶段 53 Admin 维护操作产品边界检查点。
- `7d66e88`：阶段 54 Admin 策略与复制轻量边界检查点。
- `5aedc1f`：阶段 55 Admin 配置高风险边界检查点。
- `fd5568b`：阶段 56 站点复制 peer lab-only 边界检查点。
- `d60703b`：阶段 57 服务类强破坏性边界检查点。
- `2e67c6d`：阶段 58 Crypto/lab 阻塞复核检查点。
- `abd69ad`：阶段 59 剩余 Admin 高风险产品边界检查点。
- `62ab598`：阶段 60 发布候选外部门禁复审检查点。
- `2a2fc8e`：阶段 61 mc 只读发布候选证据检查点。
- `ba52239`：阶段 62 用户面发布指南检查点。
- `e87138a`：阶段 63 最终缺口审计检查点。
- `c2cc543`：阶段 64 正式发布前外部门禁检查点。
- `0074292`：阶段 65 发布候选交接边界检查点。
- `18d3eb3`：阶段 66 只读 Admin 状态结果模型检查点。
- `b0d7723`：阶段 67 只读 Admin 诊断摘要模型检查点。
- `38f779a`：阶段 68 站点复制只读摘要模型检查点。
- `3a1f8ef`：阶段 69 Admin pool 只读摘要模型检查点。
- `6f2c2f7`：阶段 70 batch job 只读状态摘要检查点。
- `38ad441`：阶段 71 replication MRF 只读摘要检查点。
- `a2fded7`：阶段 72 IDP 配置加密边界检查点。
- `146369e`：阶段 73 加密响应安全诊断检查点。
- `4b39408`：阶段 74 IDP 加密边界统计修正检查点。
- `7c2f6ff`：阶段 75 新口径缺口审计检查点。
- `1b7f609`：阶段 76 Crypto Gate 放行清单检查点。
- `2b5c4fc`：阶段 77 破坏性 lab 准备度门禁检查点。

阶段 78 发布工程门禁口径：两条线继续使用 `0.1.0-SNAPSHOT`，不打正式 tag、不发布 Maven；route parity 233 / 233，product-typed 233 / 233，`raw-fallback = 0`。当前没有公开路由、产品入口或 raw-only 缺口；正式发布前必须先满足 Crypto Gate、独立 lab 和 Maven/tag 发布工程清单。阶段 77 已补充非破坏性 lab 准备度审计，但这不等于真实破坏性矩阵通过。

## 验证命令

JDK8 分支：

```bash
cd /dxl/minio-project/minio-reactive-java
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH \
mvn -q -DfailIfNoTests=true test

JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH \
MINIO_ENDPOINT=http://127.0.0.1:9000 \
MINIO_ACCESS_KEY=your-access-key \
MINIO_SECRET_KEY=your-secret-key \
MINIO_REGION=us-east-1 \
mvn -q -Dtest=LiveMinioIntegrationTest test
```

JDK17+ 分支：

```bash
cd /dxl/minio-project/minio-reactive-java-jdk17
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
mvn -q -DfailIfNoTests=true test

JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
MINIO_ENDPOINT=http://127.0.0.1:9000 \
MINIO_ACCESS_KEY=your-access-key \
MINIO_SECRET_KEY=your-secret-key \
MINIO_REGION=us-east-1 \
mvn -q -Dtest=LiveMinioIntegrationTest test
```

JDK17+ 分支的额外编译验证：

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH \
mvn -q -DskipTests test-compile

JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 \
PATH=/usr/lib/jvm/java-25-openjdk-amd64/bin:$PATH \
mvn -q -DskipTests test-compile
```

## 同步原则

- 新增公开 API 时，两条分支都要同步。
- 新增测试时，两条分支都要同步。
- 新增文档时，两条分支都要同步。
- 如果 JDK17+ 分支需要使用不同依赖版本，应只改构建配置，不应随意改变 SDK 对外语义。
- 如果某个改动无法兼容 JDK8，必须在提交信息中说明原因，并避免把它合入 `master`。

## 提交要求

每个提交都应说明：

- 为什么做这个改动。
- 受到哪些外部约束影响。
- 拒绝了哪些替代方案以及原因。
- 做过哪些验证。
- 哪些场景尚未验证。

这样可以避免后续重复探索同一个问题，也方便维护两条分支的长期一致性。
