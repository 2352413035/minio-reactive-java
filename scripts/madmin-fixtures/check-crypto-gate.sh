#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)

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

echo "Crypto Gate Fail 状态已复核：fixture 通过，pom.xml 与源码均未新增默认响应解密依赖。"
echo "决策记录：docs/adr/001-madmin-default-encryption-dependency.md；阶段记录：docs/23-stage25-crypto-gate-review.md"
