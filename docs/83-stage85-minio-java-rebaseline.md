# 阶段 85：以 minio-java 为主对标重新建立基线

本阶段正式把 SDK 主对标项目从服务端路由表切回同目录 `minio-java`。`minio` 服务端源码继续用于确认协议和 MinIO 扩展路由，但不再作为 Java SDK 用户体验的主完成度口径。

## 当前对标基准

| 基准 | 用途 |
| --- | --- |
| `minio-java/api` | 对象存储 SDK API、`*Args` builder、返回模型、错误体验、上传下载工程细节。 |
| `minio-java/adminapi` | 官方 Java Admin API、Admin Crypto 自动解密、Admin 响应模型。 |
| `minio` 服务端 | HTTP 路由、Admin/KMS/STS/Metrics/Health 真实协议和 Docker lab 行为。 |
| `minio-reactive-java` | JDK8 响应式实现主分支。 |
| `minio-reactive-java-jdk17` | JDK17+ 同步分支。 |

## 本阶段新增能力

1. 新增 `scripts/report-minio-java-parity.py`，生成 `minio-java` 与当前响应式 SDK 的静态 API 对标报告。
2. 报告输出对象存储 API、Admin API、`*Args` 和 credentials provider 差距。
3. `ReactiveMinioClient` 增加一批 minio-java 同名方法包装，优先消除“已有功能但命名不对齐”的缺口：
   - `getBucketCors` / `setBucketCors` / `deleteBucketCors`
   - `deleteBucketNotification`
   - `getObjectLockConfiguration` / `setObjectLockConfiguration` / `deleteObjectLockConfiguration`
   - `enableObjectLegalHold` / `disableObjectLegalHold` / `isObjectLegalHoldEnabled`
4. 原有 `getBucketCorsConfiguration`、`setBucketCorsConfiguration`、`getBucketObjectLockConfiguration` 等响应式命名继续保留，避免破坏已有使用者。

## 当前报告结论

生成命令示例：

```bash
python3 scripts/report-minio-java-parity.py \
  --minio-java-root ../minio-java \
  --worktree . \
  --format markdown \
  --output /tmp/minio-java-parity.md
```

阶段 85 后 JDK8/JDK17+ 两个分支对象存储 API 口径一致：

| 口径 | 数量 |
| --- | ---: |
| minio-java 核心对象 API | 59 |
| 精确同名 | 51 |
| 别名或部分覆盖 | 0 |
| 缺失 | 8 |

仍缺失的对象存储 API：

- `appendObject`
- `composeObject`
- `downloadObject`
- `getPresignedPostFormData`
- `promptObject`
- `putObjectFanOut`
- `uploadObject`
- `uploadSnowballObjects`

Admin API 当前没有完全缺失项，但仍有 8 个方法只是别名或部分覆盖，需要继续对齐官方 adminapi 命名与强类型结果：

- `addUpdateGroup`
- `attachPolicy`
- `clearBucketQuota`
- `detachPolicy`
- `getServiceAccountInfo`
- `listServiceAccount`
- `removeGroup`
- `setPolicy`

更大的结构性差距仍然是：

- `minio-java` 有 86 个 `*Args` 类；当前响应式 SDK 还没有同名 Args 层。
- `minio-java` credentials provider 体系明显更完整；当前只具备 reactive/static/sts 基础层。
- `minio-java/adminapi/Crypto.java` 已经实现 Admin 加密自动解密；我们后续应把 Crypto Gate 从长期边界改为实现任务。

## 下一阶段建议

1. 阶段 86：优先补 `downloadObject` / `uploadObject`，把文件上传下载便捷方法与响应式流式方法结合起来。
2. 阶段 87：补 `composeObject`、`appendObject`、`putObjectFanOut`、`uploadSnowballObjects` 等对象写入高级能力。
3. 阶段 88：补 `getPresignedPostFormData` 与 `PostPolicy` 等价模型。
4. 阶段 89：启动 `*Args` builder 层，先覆盖 bucket/object/list/put/get/stat/remove 核心路径。
5. 阶段 90：对齐 Admin API 别名方法，并基于 `minio-java/adminapi/Crypto.java` 规划 BouncyCastle 依赖与自动解密实现。

## 边界

本阶段不声明项目完成；它只是把主对标口径修正到正确基准，并把当前差距变成机器可重复生成的报告。
