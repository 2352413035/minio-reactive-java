#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)
STATUS_FILE="$SCRIPT_DIR/crypto-gate-status.properties"

require_status_line() {
  local expected="$1"
  if ! grep -Fxq "$expected" "$STATUS_FILE"; then
    echo "Crypto Gate Fail：状态文件缺少预期行：$expected" >&2
    exit 1
  fi
}

if [[ ! -f "$STATUS_FILE" ]]; then
  echo "Crypto Gate Fail：缺少状态文件 scripts/madmin-fixtures/crypto-gate-status.properties。" >&2
  exit 1
fi

require_status_line "CRYPTO_GATE_STATUS=fail"
require_status_line "CRYPTO_GATE_OWNER_APPROVED=false"
require_status_line "CRYPTO_GATE_SECURITY_APPROVED=false"
require_status_line "CRYPTO_GATE_ARCHITECT_APPROVED=false"
require_status_line "CRYPTO_GATE_DECISION_DOC=docs/30-stage32-crypto-gate-independent-review.md"

"$SCRIPT_DIR/verify-fixtures.sh"

POM_DEP_PATTERN='(bouncycastle|bcprov|bcpkix|bcutil|argon2|chacha20|tink|nacl|libsodium|jna)'
SOURCE_IMPORT_PATTERN='^import +(org\.bouncycastle|de\.mkammerer|com\.google\.crypto\.tink|com\.sun\.jna|jnr\.ffi|org\.libsodium|org\.abstractj\.kalium|com\.iwebpp\.crypto)'

if grep -Eiq "$POM_DEP_PATTERN" "$REPO_ROOT/pom.xml"; then
  echo "Crypto Gate Fail：pom.xml 出现未批准的 crypto 依赖候选。" >&2
  exit 1
fi

if find "$REPO_ROOT/src" -name '*.java' -print0 \
  | xargs -0 grep -nE "$SOURCE_IMPORT_PATTERN"; then
  echo "Crypto Gate Fail：源码出现未批准的 crypto/JNA 依赖 import。" >&2
  exit 1
fi

echo "Crypto Gate Fail 状态已复核：状态文件、fixture、pom.xml 与源码 import 均未放行默认响应解密。"
echo "决策记录：docs/adr/001-madmin-default-encryption-dependency.md；阶段记录：docs/23-stage25-crypto-gate-review.md、docs/30-stage32-crypto-gate-independent-review.md；放行准备清单：docs/43-stage45-crypto-gate-pass-prep.md"
