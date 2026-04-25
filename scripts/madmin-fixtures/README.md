# madmin fixture 脚本

这里的脚本使用系统 `go` 与固定版本的 `madmin-go` 生成 / 校验 madmin fixture。

## 设计目标

1. 不依赖临时 `/tmp/go`，直接使用系统 Go 环境。
2. 固定 `madmin-go` 版本，避免 fixture 漂移无从解释。
3. Go 只作为互操作验证工具，不进入 Java SDK 运行时依赖。
4. 阶段 111 起配合 Bouncy Castle 依赖验证 Crypto Gate Pass：Argon2id + AES-GCM、Argon2id + ChaCha20-Poly1305 与 PBKDF2 + AES-GCM 都必须可解密。

## 生成 fixture

当前工作区根目录执行：

```bash
scripts/madmin-fixtures/generate-fixtures.sh
```

默认输出到：

- `src/test/resources/madmin-fixtures/`

产物包括：

- `pbkdf2-aesgcm-go.base64`
- `argon2id-aesgcm-go-default.base64` 或当前硬件默认算法对应文件
- `argon2id-chacha20-go-forced.base64`
- `fixture-metadata.json`

`argon2id-chacha20-go-forced.base64` 使用 madmin-go 依赖的 `sio-go` 与 `argon2` 按同一格式强制生成，用于在 Native AES 机器上也能稳定验证 ChaCha20-Poly1305 路径。

## 校验 committed fixture

当前工作区根目录执行：

```bash
scripts/madmin-fixtures/verify-fixtures.sh
```

脚本会先验证仓库提交的 fixture，再临时生成一组 fixture 验证当前 Go 工具链与 Java 解密实现兼容。

## Crypto Gate Pass 校验

```bash
scripts/madmin-fixtures/check-crypto-gate.sh
```

该脚本会校验：

1. `crypto-gate-status.properties` 处于 Pass 状态。
2. `pom.xml` 中声明 `org.bouncycastle:bcprov-jdk18on:1.82`。
3. Java 源码使用已审计的 Bouncy Castle crypto 边界。
4. committed fixture 与新生成 fixture 都能被当前 Java 测试正确解密。
