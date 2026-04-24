# 22 阶段 24：destructive lab 扩展

## 1. 背景

Admin destructive 接口不能在共享 MinIO 环境中默认验证。阶段 24 的目标是把实验参数从零散环境变量推进到独立 lab 配置文件，同时为 bucket quota、tier、remote target、batch job 预留可回滚或 lab-only fixture。

## 2. 新增/调整内容

- 新增 `scripts/minio-lab/load-config.sh`：读取简单 `KEY=VALUE` 配置文件，不执行 shell 代码。
- 新增 `scripts/minio-lab/lab.example.properties`：提供本机私有配置模板，真实凭证不得提交。
- `verify-env.sh` 与 `run-destructive-tests.sh` 支持 `MINIO_LAB_CONFIG_FILE`，也会自动读取未提交的 `scripts/minio-lab/lab.properties`。
- `DestructiveAdminIntegrationTest` 支持从 lab 配置文件读取参数。
- 新增可选测试：bucket quota write + restore、tier verify、remote target list、batch job 只读探测。

## 3. 配置文件入口

推荐方式：

```bash
cp scripts/minio-lab/lab.example.properties scripts/minio-lab/lab.properties
# 编辑 lab.properties，填入独立 lab 信息。
scripts/minio-lab/run-destructive-tests.sh
```

也可以把配置文件放在仓库外：

```bash
export MINIO_LAB_CONFIG_FILE=/secure/path/minio-lab.properties
scripts/minio-lab/run-destructive-tests.sh
```

## 4. 风险边界

- `verify-env.sh` 继续拒绝 `http://127.0.0.1:9000`、`http://localhost:9000`、`http://0.0.0.0:9000`，并拒绝与 `MINIO_ENDPOINT` 相同的 lab endpoint。
- bucket quota 写入必须提供测试值和恢复值；测试 finally 中总是尝试恢复。
- tier、remote target、batch job fixture 只在明确配置时执行，默认跳过。
- 这些 lab 证据不能替代共享 live 测试；它们只证明高风险 Admin 能力在独立可回滚环境中的可用性。

## 5. 验证要求

- 默认 `mvn test` 必须继续跳过 破坏性测试，不修改共享 MinIO。
- `verify-env.sh` 在缺少授权或指向共享 endpoint 时必须失败。
- `run-destructive-tests.sh` 在独立 lab 配置存在时统一加载配置并执行 `DestructiveAdminIntegrationTest`。
