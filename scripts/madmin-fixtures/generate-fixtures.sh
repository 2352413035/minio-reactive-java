#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../.." && pwd)
OUTPUT_DIR=${1:-"$REPO_ROOT/src/test/resources/madmin-fixtures"}
MADMIN_GO_VERSION=${MADMIN_GO_VERSION:-v3.0.109}
FIXTURE_SECRET=${FIXTURE_SECRET:-fixture-secret}
FIXTURE_PLAINTEXT=${FIXTURE_PLAINTEXT:-madmin fixture payload}

command -v go >/dev/null 2>&1 || {
  echo "未找到 go 命令，请先安装 Go 再生成 madmin fixture。" >&2
  exit 1
}

mkdir -p "$OUTPUT_DIR"
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

cat > "$TMP_DIR/go.mod" <<GOMOD
module fixturegen

go 1.21

require github.com/minio/madmin-go/v3 ${MADMIN_GO_VERSION}
GOMOD

cat > "$TMP_DIR/main.go" <<'GOCODE'
package main

import (
  "bytes"
  "encoding/base64"
  "fmt"
  "os"

  madmin "github.com/minio/madmin-go/v3"
  "github.com/secure-io/sio-go"
  "github.com/secure-io/sio-go/sioutil"
  "golang.org/x/crypto/argon2"
)

const (
  argon2idTime = 1
  argon2idMemory = 64 * 1024
  argon2idThreads = 4
)

func encryptForcedChaCha20(secret string, plain string) []byte {
  salt := sioutil.MustRandom(32)
  key := argon2.IDKey([]byte(secret), salt, argon2idTime, argon2idMemory, argon2idThreads, 32)
  stream, err := sio.ChaCha20Poly1305.Stream(key)
  if err != nil {
    panic(err)
  }
  nonce := sioutil.MustRandom(stream.NonceSize())
  out := bytes.NewBuffer(nil)
  out.Write(salt)
  out.WriteByte(0x01)
  out.Write(nonce)
  writer := stream.EncryptWriter(out, nonce, nil)
  if _, err := writer.Write([]byte(plain)); err != nil {
    panic(err)
  }
  if err := writer.Close(); err != nil {
    panic(err)
  }
  return out.Bytes()
}

func main() {
  secret := os.Getenv("FIXTURE_SECRET")
  plain := os.Getenv("FIXTURE_PLAINTEXT")
  out, err := madmin.EncryptData(secret, []byte(plain))
  if err != nil {
    panic(err)
  }
  fmt.Printf("%02x\n", out[32])
  fmt.Println(base64.StdEncoding.EncodeToString(out))
  forcedChaCha := encryptForcedChaCha20(secret, plain)
  fmt.Printf("%02x\n", forcedChaCha[32])
  fmt.Println(base64.StdEncoding.EncodeToString(forcedChaCha))
}
GOCODE

run_fixture() {
  local mode=$1
  local go_tags=()
  if [[ "$mode" == "pbkdf2" ]]; then
    go_tags=(-tags fips)
  fi
  (
    cd "$TMP_DIR"
    GOWORK=off go mod tidy >/dev/null
    FIXTURE_SECRET="$FIXTURE_SECRET" FIXTURE_PLAINTEXT="$FIXTURE_PLAINTEXT" GOWORK=off go run "${go_tags[@]}" .
  )
}

write_fixture() {
  local file=$1
  local algorithm=$2
  local command=$3
  local payload=$4
  cat > "$file" <<TEXT
# 由 madmin-go ${MADMIN_GO_VERSION} 使用 "${command}" 生成。
# secret: ${FIXTURE_SECRET}
# plaintext: ${FIXTURE_PLAINTEXT}
# algorithm: ${algorithm}
${payload}
TEXT
}

PBKDF_OUTPUT=$(run_fixture pbkdf2)
PBKDF_ID=$(printf '%s\n' "$PBKDF_OUTPUT" | sed -n '1p')
PBKDF_PAYLOAD=$(printf '%s\n' "$PBKDF_OUTPUT" | sed -n '2p')
if [[ "$PBKDF_ID" != "02" ]]; then
  echo "PBKDF2 fixture 期望算法 02，实际为 $PBKDF_ID" >&2
  exit 1
fi
write_fixture "$OUTPUT_DIR/pbkdf2-aesgcm-go.base64" "0x02 PBKDF2 + AES-GCM" "go run -tags fips ." "$PBKDF_PAYLOAD"

DEFAULT_OUTPUT=$(run_fixture default)
DEFAULT_ID=$(printf '%s\n' "$DEFAULT_OUTPUT" | sed -n '1p')
DEFAULT_PAYLOAD=$(printf '%s\n' "$DEFAULT_OUTPUT" | sed -n '2p')
FORCED_CHACHA_ID=$(printf '%s\n' "$DEFAULT_OUTPUT" | sed -n '3p')
FORCED_CHACHA_PAYLOAD=$(printf '%s\n' "$DEFAULT_OUTPUT" | sed -n '4p')
case "$DEFAULT_ID" in
  00)
    write_fixture "$OUTPUT_DIR/argon2id-aesgcm-go-default.base64" "0x00 Argon2id + AES-GCM" "go run ." "$DEFAULT_PAYLOAD"
    ;;
  01)
    write_fixture "$OUTPUT_DIR/argon2id-chacha20-go-default.base64" "0x01 Argon2id + ChaCha20-Poly1305" "go run ." "$DEFAULT_PAYLOAD"
    ;;
  *)
    echo "默认 fixture 生成了未知算法 ID: $DEFAULT_ID" >&2
    exit 1
    ;;
esac

if [[ "$FORCED_CHACHA_ID" != "01" ]]; then
  echo "强制 ChaCha20 fixture 期望算法 01，实际为 $FORCED_CHACHA_ID" >&2
  exit 1
fi
write_fixture "$OUTPUT_DIR/argon2id-chacha20-go-forced.base64" "0x01 Argon2id + ChaCha20-Poly1305" "go run . forced ChaCha20" "$FORCED_CHACHA_PAYLOAD"

cat > "$OUTPUT_DIR/fixture-metadata.json" <<JSON
{
  "madminGoVersion": "${MADMIN_GO_VERSION}",
  "fixtureSecret": "${FIXTURE_SECRET}",
  "fixturePlaintext": "${FIXTURE_PLAINTEXT}",
  "defaultAlgorithmId": "${DEFAULT_ID}",
  "forcedChaChaAlgorithmId": "${FORCED_CHACHA_ID}",
  "generatedBy": "scripts/madmin-fixtures/generate-fixtures.sh"
}
JSON

echo "已生成 madmin fixture 到: $OUTPUT_DIR"
