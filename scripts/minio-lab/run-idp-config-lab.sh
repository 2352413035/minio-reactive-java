#!/usr/bin/env bash
set -euo pipefail

# 运行 IDP config add/update/delete 一次性实验矩阵。
# 该脚本会启动独立 Docker MinIO 与临时 OIDC discovery/JWKS 夹具，
# 配置和 dummy client_secret 只写入 /tmp 下的私有运行目录，不进入仓库。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "缺少命令：$1" >&2
    exit 1
  }
}

need_cmd docker
need_cmd curl
need_cmd mc
need_cmd python3

LAB_NAME="${MINIO_LAB_DOCKER_NAME:-minio-reactive-idp-config-lab}"
LAB_API_PORT="${MINIO_LAB_DOCKER_API_PORT:-19340}"
LAB_CONSOLE_PORT="${MINIO_LAB_DOCKER_CONSOLE_PORT:-19341}"
LAB_RUN_DIR="${MINIO_LAB_DOCKER_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-idp-lab-XXXXXX)}"
OIDC_PORT="${MINIO_LAB_OIDC_PORT:-19580}"
OIDC_HOST="${MINIO_LAB_OIDC_HOST:-}"
OIDC_PID=""

if [[ -z "$OIDC_HOST" ]]; then
  # 在普通 Linux 主机上，Docker 容器通常能访问宿主机网卡 IP；
  # 在 Codex/Docker-socket 环境里，这个 IP 也比 docker bridge gateway 更可靠。
  OIDC_HOST="$(hostname -I 2>/dev/null | awk '{print $1}')"
fi
if [[ -z "$OIDC_HOST" ]]; then
  OIDC_HOST="$(docker network inspect bridge -f '{{(index .IPAM.Config 0).Gateway}}')"
fi

cleanup_oidc() {
  if [[ -n "$OIDC_PID" ]]; then
    kill "$OIDC_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup_oidc EXIT

MINIO_LAB_DOCKER_NAME="$LAB_NAME" \
MINIO_LAB_DOCKER_API_PORT="$LAB_API_PORT" \
MINIO_LAB_DOCKER_CONSOLE_PORT="$LAB_CONSOLE_PORT" \
MINIO_LAB_DOCKER_RUN_DIR="$LAB_RUN_DIR" \
  "$SCRIPT_DIR/start-docker-lab.sh" >/dev/null

# shellcheck source=/dev/null
source "$LAB_RUN_DIR/env"
OIDC_DIR="$LAB_RUN_DIR/oidc-fixture"
mkdir -p "$OIDC_DIR/.well-known"

cat > "$OIDC_DIR/.well-known/openid-configuration" <<EOF_JSON
{"issuer":"http://${OIDC_HOST}:${OIDC_PORT}","authorization_endpoint":"http://${OIDC_HOST}:${OIDC_PORT}/authorize","token_endpoint":"http://${OIDC_HOST}:${OIDC_PORT}/token","userinfo_endpoint":"http://${OIDC_HOST}:${OIDC_PORT}/userinfo","jwks_uri":"http://${OIDC_HOST}:${OIDC_PORT}/keys","response_types_supported":["code"],"subject_types_supported":["public"],"id_token_signing_alg_values_supported":["RS256"],"scopes_supported":["openid","profile","email"],"claims_supported":["policy","sub","email"]}
EOF_JSON
cat > "$OIDC_DIR/keys" <<'EOF_JSON'
{"keys":[]}
EOF_JSON

python3 -u -m http.server "$OIDC_PORT" --bind 0.0.0.0 --directory "$OIDC_DIR" > "$LAB_RUN_DIR/oidc-http.log" 2>&1 &
OIDC_PID="$!"
sleep 1
curl -fsS "http://127.0.0.1:${OIDC_PORT}/.well-known/openid-configuration" >/dev/null

cat > "$LAB_RUN_DIR/idp-add.txt" <<EOF_CFG
enable=on config_url="http://${OIDC_HOST}:${OIDC_PORT}/.well-known/openid-configuration" client_id="minio-reactive-idp-lab" client_secret="stage123-dummy-secret" claim_name="policy" scopes="openid,profile,email" redirect_uri_dynamic="on"
EOF_CFG
cat > "$LAB_RUN_DIR/idp-update.txt" <<EOF_CFG
enable=on config_url="http://${OIDC_HOST}:${OIDC_PORT}/.well-known/openid-configuration" client_id="minio-reactive-idp-lab" client_secret="stage123-dummy-secret-2" claim_name="policy" scopes="openid,profile,email" redirect_uri_dynamic="on" display_name="minio-reactive-idp-lab"
EOF_CFG

cat >> "$LAB_CONFIG_FILE" <<EOF_CONFIG
MINIO_LAB_ALLOW_WRITE_FIXTURES=true
MINIO_LAB_IDP_TYPE=openid
MINIO_LAB_IDP_NAME=_
MINIO_LAB_ADD_IDP_CONFIG_BODY_FILE=$LAB_RUN_DIR/idp-add.txt
MINIO_LAB_UPDATE_IDP_CONFIG_BODY_FILE=$LAB_RUN_DIR/idp-update.txt
MINIO_LAB_DELETE_IDP_AFTER_TEST=true
MINIO_LAB_RESTART_IDP_AFTER_CONFIG_CHANGE=true
MINIO_LAB_DOCKER_NAME=$LAB_NAME
EOF_CONFIG

cd "$REPO_ROOT"
MINIO_LAB_CONFIG_FILE="$LAB_CONFIG_FILE" \
MINIO_LAB_RUN_ID="${MINIO_LAB_RUN_ID:-idp-config-lab-$(date -u +%Y%m%dT%H%M%SZ)}" \
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
  "$SCRIPT_DIR/run-destructive-tests.sh"

cat <<EOF_SUMMARY
IDP config lab 已完成。
- 容器名：$LAB_NAME
- MinIO endpoint：http://127.0.0.1:$LAB_API_PORT
- OIDC discovery：http://$OIDC_HOST:$OIDC_PORT/.well-known/openid-configuration
- 临时运行目录：$LAB_RUN_DIR
- 报告目录：$LAB_REPORT_DIR

清理命令：
  docker rm -f $LAB_NAME
  rm -rf $LAB_RUN_DIR
EOF_SUMMARY
