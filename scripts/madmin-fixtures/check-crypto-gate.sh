#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)

"$SCRIPT_DIR/verify-fixtures.sh"

if grep -Eiq '(bouncycastle|bcprov|argon2|chacha20|tink|nacl|libsodium)' "$REPO_ROOT/pom.xml"; then
  echo "Crypto Gate Fail：pom.xml 出现未批准的 crypto 依赖候选。" >&2
  exit 1
fi

echo "Crypto Gate Fail 状态已复核：fixture 通过，且 pom.xml 未新增默认响应解密依赖。"
