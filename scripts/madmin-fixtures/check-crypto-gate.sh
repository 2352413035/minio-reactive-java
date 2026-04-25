#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)
STATUS_FILE="$SCRIPT_DIR/crypto-gate-status.properties"

require_status_line() {
  local expected="$1"
  if ! grep -Fxq "$expected" "$STATUS_FILE"; then
    echo "Crypto Gate Pass 校验失败：状态文件缺少预期行：$expected" >&2
    exit 1
  fi
}

if [[ ! -f "$STATUS_FILE" ]]; then
  echo "Crypto Gate Pass 校验失败：缺少状态文件 scripts/madmin-fixtures/crypto-gate-status.properties。" >&2
  exit 1
fi

require_status_line "CRYPTO_GATE_STATUS=pass"
require_status_line "CRYPTO_GATE_OWNER_APPROVED=true"
require_status_line "CRYPTO_GATE_SECURITY_APPROVED=true"
require_status_line "CRYPTO_GATE_ARCHITECT_APPROVED=true"
require_status_line "CRYPTO_GATE_DECISION_DOC=docs/109-stage111-crypto-gate-pass.md"
require_status_line "CRYPTO_GATE_DEPENDENCY=org.bouncycastle:bcprov-jdk18on:1.82"

"$SCRIPT_DIR/verify-fixtures.sh"

if ! grep -Fq '<artifactId>bcprov-jdk18on</artifactId>' "$REPO_ROOT/pom.xml"; then
  echo "Crypto Gate Pass 校验失败：pom.xml 未声明 bcprov-jdk18on。" >&2
  exit 1
fi

if ! grep -Fq '<bouncycastle.version>1.82</bouncycastle.version>' "$REPO_ROOT/pom.xml"; then
  echo "Crypto Gate Pass 校验失败：pom.xml 中 Bouncy Castle 版本不是 1.82。" >&2
  exit 1
fi

if ! grep -R '^import org\.bouncycastle\.crypto\.' "$REPO_ROOT/src/main/java/io/minio/reactive/util/MadminEncryptionSupport.java" >/dev/null; then
  echo "Crypto Gate Pass 校验失败：MadminEncryptionSupport 未使用已审计的 Bouncy Castle crypto 边界。" >&2
  exit 1
fi

if grep -R "Crypto Gate Fail" "$REPO_ROOT/src/main/java" "$REPO_ROOT/src/test/java" >/dev/null; then
  echo "Crypto Gate Pass 校验失败：源码或测试仍包含 Crypto Gate Fail 语义。" >&2
  exit 1
fi

echo "Crypto Gate Pass 状态已复核：bcprov 依赖、madmin fixture、Argon2id/AES-GCM、Argon2id/ChaCha20-Poly1305 与 PBKDF2/AES-GCM 解密均通过。"
echo "决策记录：docs/109-stage111-crypto-gate-pass.md；对标实现：minio-java adminapi Crypto。"
