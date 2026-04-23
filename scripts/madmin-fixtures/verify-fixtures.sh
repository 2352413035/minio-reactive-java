#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

cd "$REPO_ROOT"
# 先验证仓库已提交的 fixture 资源本身可被当前 Java 测试读取。
mvn -q \
  -Dtest=MadminEncryptionSupportTest#shouldDecryptPbkdf2AesGcmMadminGoFixture+shouldDiagnoseUnsupportedDefaultArgon2idAesGcmFixture+shouldDiagnoseUnsupportedDefaultArgon2idChaChaFixtureWhenPresent \
  test

# 再验证当前 Go 工具链新生成的 fixture 是否与 Java 兼容。
"$SCRIPT_DIR/generate-fixtures.sh" "$TMP_DIR/generated" >/dev/null
DEFAULT_ID=$(python3 - <<'PY' "$TMP_DIR/generated/fixture-metadata.json"
import json, sys
print(json.load(open(sys.argv[1], encoding='utf-8'))['defaultAlgorithmId'])
PY
)
if [[ "$DEFAULT_ID" == "00" ]]; then
  MADMIN_FIXTURE_DIR="$TMP_DIR/generated" mvn -q \
    -Dtest=MadminEncryptionSupportTest#shouldDecryptPbkdf2AesGcmMadminGoFixture+shouldDiagnoseUnsupportedDefaultArgon2idAesGcmFixture \
    test
elif [[ "$DEFAULT_ID" == "01" ]]; then
  MADMIN_FIXTURE_DIR="$TMP_DIR/generated" mvn -q \
    -Dtest=MadminEncryptionSupportTest#shouldDecryptPbkdf2AesGcmMadminGoFixture+shouldDiagnoseUnsupportedDefaultArgon2idChaChaFixtureWhenPresent \
    test
  if [[ ! -f "$REPO_ROOT/src/test/resources/madmin-fixtures/argon2id-chacha20-go-default.base64" ]]; then
    echo "当前硬件默认算法为 0x01（Argon2id + ChaCha20-Poly1305），仓库尚未提交 committed ChaCha20 fixture；按计划允许记录 skip reason。" >&2
  fi
else
  echo "未知默认算法 ID: $DEFAULT_ID" >&2
  exit 1
fi

echo "madmin fixture 校验通过（已验证 committed fixture 与 Go 新生成 fixture 都能被当前 Java 测试正确处理）。"
