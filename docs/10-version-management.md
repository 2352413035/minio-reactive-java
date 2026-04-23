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

JDK17+ 分支：

- `7303e7e`：JDK17 线同步核心 S3 reactive SDK 能力检查点。
- `b9073fc`：JDK17 线同步 MinIO 公开接口目录和原始响应式执行器检查点。
- `7c598a1`：JDK17+ 线同步强业务客户端基础检查点，语义应与 JDK8 线保持一致。

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
mvn -q -DskipTests compile

JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 \
PATH=/usr/lib/jvm/java-25-openjdk-amd64/bin:$PATH \
mvn -q -DskipTests compile
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
