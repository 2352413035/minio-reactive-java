# madmin fixture 脚本

这里的脚本使用系统 `go` 与固定版本的 `madmin-go` 生成 / 校验 madmin fixture。

## 设计目标

1. 不再依赖临时 `/tmp/go`。
2. 固定 `madmin-go` 版本，避免 fixture 漂移无从解释。
3. Go 只作为互操作验证工具，不进入 Java SDK 运行时依赖。

## 生成 fixture

当前工作区根目录执行：

```bash
scripts/madmin-fixtures/generate-fixtures.sh
```

默认输出到：

- `src/test/resources/madmin-fixtures/`

产物包括：

- `pbkdf2-aesgcm-go.base64`
- `argon2id-aesgcm-go-default.base64` 或 `argon2id-chacha20-go-default.base64`
- `fixture-metadata.json`

## 校验 committed fixture

当前工作区根目录执行：

```bash
scripts/madmin-fixtures/verify-fixtures.sh
```

如果当前机器默认算法是 `0x01` 且仓库尚未提交对应 committed ChaCha20 fixture，脚本会先验证 Go 新生成 fixture，再输出 skip reason 提醒仓库资源尚未补齐。
