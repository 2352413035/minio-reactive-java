# 阶段 130：高频桶 / 文件 Client 方法进一步实证

## 背景

用户额外强调：最终最常用的桶、文件/对象操作必须再充分验证，尤其是“上传文件”这类真实业务最常走的方法。

阶段 125 已经把核心桶/对象 live 路径锁住；阶段 127 已补高频只读 Admin 视图。本阶段继续把最常用 `ReactiveMinioClient` 方法做得更显式。

## 本阶段增强点

在 `LiveMinioIntegrationTest` 的高频桶/文件用例中，进一步显式验证：

1. `makeBucket(...)`
2. `bucketExists(...)`
3. `listBuckets()`
4. `getBucketLocation(...)`
5. `uploadObject(...)`
6. `statObject(...)`
7. `downloadObject(...)`
8. `removeObject(...)`
9. `removeBucket(...)`

其中，“上传文件 -> HEAD/stat -> 下载到本地文件 -> 删除对象 -> 删除 bucket” 形成了更接近真实用户使用顺序的闭环。

## 覆盖矩阵

| 高频方法 | 证据 |
| --- | --- |
| `makeBucket` | 真实 live 创建 bucket |
| `bucketExists` | 创建前 `false`、创建后 `true`、删除后 `false` |
| `listBuckets` | 创建后立即可见 |
| `getBucketLocation` | 返回非空 region |
| `putObject` | 文本对象写入 |
| `uploadObject` | 本地文件上传到对象 |
| `statObject` | 对上传后的对象做 HEAD/metadata 验证 |
| `getObject` / `getObjectAsString` / `getObjectRange` | 读取全文与区间内容 |
| `downloadObject` | 下载到本地文件，且验证默认不覆盖 / 显式覆盖 |
| `listObjectsPage` | 分页列对象 |
| `removeObject` | 删除单对象后不可再 stat |
| `removeBucket` | bucket 清空后删除，随后 `bucketExists=false` |

## 结论

到阶段 130 为止，面向最终用户最常用的对象存储路径已经形成三层证据：

1. **核心桶/对象 live 路径**（阶段 125）
2. **高频只读 Admin live 路径**（阶段 127）
3. **高频桶 / 文件 Client 方法显式闭环**（阶段 130）

因此，普通用户最常走的 bucket / 文件操作，不再只是“间接覆盖”，而是已经被明确写成真实可执行的 live 验证链路。

## 本轮真实执行证据

- JDK8 分支：`LiveMinioIntegrationTest` 在一次性 Docker MinIO 上真实执行，`tests=5, failures=0, errors=0, skipped=0`。
- JDK17 分支：`LiveMinioIntegrationTest` 在同一一次性 Docker MinIO 上真实执行，`tests=5, failures=0, errors=0, skipped=0`。

这次真实回归还顺手纠正了两个过严断言：

1. 默认 region 下 `getBucketLocation()` 允许返回空字符串。
2. `data usage` / `account info` 对新建空 bucket 的计数不应强制要求 `>= 1`，应以 typed/raw 一致性为准。
