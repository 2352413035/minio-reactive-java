#!/usr/bin/env bash
set -euo pipefail

# 启动两个一次性 Docker MinIO，尝试 site replication add/edit/remove 与可选 site speedtest。
# site replication 请求体包含临时 root 凭证，只写入 /tmp 私有目录，不输出到终端或仓库。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "缺少命令：$1" >&2
    exit 1
  }
}

prop() {
  awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/, ""); print; exit}' "$2"
}

need_cmd docker
need_cmd curl
need_cmd mc
need_cmd python3
need_cmd awk

SITE_A_NAME="${MINIO_LAB_SITE_A_NAME:-lab-site-a-minio}"
SITE_B_NAME="${MINIO_LAB_SITE_B_NAME:-lab-site-b-minio}"
NETWORK_NAME="${MINIO_LAB_SITE_NETWORK:-minio-reactive-site-rep-net}"
SITE_A_API_PORT="${MINIO_LAB_SITE_A_API_PORT:-19600}"
SITE_A_CONSOLE_PORT="${MINIO_LAB_SITE_A_CONSOLE_PORT:-19601}"
SITE_B_API_PORT="${MINIO_LAB_SITE_B_API_PORT:-19602}"
SITE_B_CONSOLE_PORT="${MINIO_LAB_SITE_B_CONSOLE_PORT:-19603}"
SITE_A_RUN_DIR="${MINIO_LAB_SITE_A_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-site-a-XXXXXX)}"
SITE_B_RUN_DIR="${MINIO_LAB_SITE_B_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-site-b-XXXXXX)}"
FIXTURE_DIR="$(mktemp -d /tmp/minio-reactive-site-fixtures-XXXXXX)"

cleanup_on_setup_failure() {
  local exit_code=$?
  if [[ "$exit_code" -ne 0 ]]; then
    docker rm -f "$SITE_A_NAME" "$SITE_B_NAME" >/dev/null 2>&1 || true
    docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
    rm -rf "$SITE_A_RUN_DIR" "$SITE_B_RUN_DIR" "$FIXTURE_DIR"
  fi
  exit "$exit_code"
}
trap cleanup_on_setup_failure EXIT

docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true

MINIO_LAB_DOCKER_NAME="$SITE_A_NAME" \
MINIO_LAB_DOCKER_API_PORT="$SITE_A_API_PORT" \
MINIO_LAB_DOCKER_CONSOLE_PORT="$SITE_A_CONSOLE_PORT" \
MINIO_LAB_DOCKER_RUN_DIR="$SITE_A_RUN_DIR" \
  "$SCRIPT_DIR/start-docker-lab.sh" >/dev/null

MINIO_LAB_DOCKER_NAME="$SITE_B_NAME" \
MINIO_LAB_DOCKER_API_PORT="$SITE_B_API_PORT" \
MINIO_LAB_DOCKER_CONSOLE_PORT="$SITE_B_CONSOLE_PORT" \
MINIO_LAB_DOCKER_RUN_DIR="$SITE_B_RUN_DIR" \
  "$SCRIPT_DIR/start-docker-lab.sh" >/dev/null

docker network create "$NETWORK_NAME" >/dev/null
docker network connect "$NETWORK_NAME" "$SITE_A_NAME" >/dev/null
docker network connect "$NETWORK_NAME" "$SITE_B_NAME" >/dev/null

SITE_A_CONFIG="$SITE_A_RUN_DIR/lab.properties"
SITE_B_CONFIG="$SITE_B_RUN_DIR/lab.properties"
SITE_A_ACCESS_KEY="$(prop MINIO_LAB_ACCESS_KEY "$SITE_A_CONFIG")"
SITE_A_SECRET_KEY="$(prop MINIO_LAB_SECRET_KEY "$SITE_A_CONFIG")"
SITE_B_ACCESS_KEY="$(prop MINIO_LAB_ACCESS_KEY "$SITE_B_CONFIG")"
SITE_B_SECRET_KEY="$(prop MINIO_LAB_SECRET_KEY "$SITE_B_CONFIG")"
SITE_B_ENDPOINT="$(prop MINIO_LAB_ENDPOINT "$SITE_B_CONFIG")"
SITE_B_BUCKET="$(prop MINIO_LAB_BUCKET "$SITE_B_CONFIG")"

# MinIO site replication 初始化要求除一个站点外其它站点为空。
# start-docker-lab 为通用破坏性夹具默认创建 bucket；这里在目标站点先删除它，
# 避免把“目标站点已有 bucket”误判为 SDK 协议失败。
MC_CONFIG_DIR="$FIXTURE_DIR/mc-site-b" mc alias set site-b "$SITE_B_ENDPOINT" "$SITE_B_ACCESS_KEY" "$SITE_B_SECRET_KEY" >/dev/null
MC_CONFIG_DIR="$FIXTURE_DIR/mc-site-b" mc rb --force "site-b/${SITE_B_BUCKET}" >/dev/null 2>&1 || true

ADD_BODY="$FIXTURE_DIR/site-replication-add.json"
REMOVE_BODY="$FIXTURE_DIR/site-replication-remove.json"

python3 - "$ADD_BODY" "$SITE_A_NAME" "$SITE_A_ACCESS_KEY" "$SITE_A_SECRET_KEY" "$SITE_B_NAME" "$SITE_B_ACCESS_KEY" "$SITE_B_SECRET_KEY" <<'PY'
import json
import sys
path, a_name, a_access, a_secret, b_name, b_access, b_secret = sys.argv[1:]
body = [
    {"name": "lab-site-a", "endpoints": f"http://{a_name}:9000", "accessKey": a_access, "secretKey": a_secret},
    {"name": "lab-site-b", "endpoints": f"http://{b_name}:9000", "accessKey": b_access, "secretKey": b_secret},
]
with open(path, "w", encoding="utf-8") as out:
    json.dump(body, out, ensure_ascii=False)
PY
printf '{"all":true}\n' > "$REMOVE_BODY"
chmod 600 "$ADD_BODY" "$REMOVE_BODY"

cat >> "$SITE_A_CONFIG" <<EOF_CONFIG
MINIO_LAB_ALLOW_WRITE_FIXTURES=true
MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE=$ADD_BODY
MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE=$REMOVE_BODY
MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true
MINIO_LAB_SITE_REPLICATION_EDIT_FROM_INFO=${MINIO_LAB_SITE_REPLICATION_EDIT_FROM_INFO:-true}
MINIO_LAB_SITE_REPLICATION_EDIT_SITE_NAME=${MINIO_LAB_SITE_REPLICATION_EDIT_SITE_NAME:-lab-site-b}
MINIO_LAB_SITE_REPLICATION_EDIT_ENDPOINT=${MINIO_LAB_SITE_REPLICATION_EDIT_ENDPOINT:-http://${SITE_B_NAME}:9000}
MINIO_LAB_SITE_REPLICATION_EDIT_SYNC=${MINIO_LAB_SITE_REPLICATION_EDIT_SYNC:-disable}
MINIO_LAB_SKIP_SITE_REPLICATION_RAW_READD=${MINIO_LAB_SKIP_SITE_REPLICATION_RAW_READD:-true}
MINIO_LAB_ENABLE_SITE_SPEEDTEST_PROBE=${MINIO_LAB_ENABLE_SITE_SPEEDTEST_PROBE:-true}
MINIO_LAB_SPEEDTEST_SITE_DURATION_SECONDS=${MINIO_LAB_SPEEDTEST_SITE_DURATION_SECONDS:-2}
MINIO_LAB_EXPECT_SITE_SPEEDTEST_FAILURE=${MINIO_LAB_EXPECT_SITE_SPEEDTEST_FAILURE:-false}
MINIO_LAB_SITE_SPEEDTEST_EXPECTED_ERROR=${MINIO_LAB_SITE_SPEEDTEST_EXPECTED_ERROR:-not implemented|NotImplemented|replication|site}
EOF_CONFIG
chmod 600 "$SITE_A_CONFIG"

trap - EXIT

cd "$REPO_ROOT"
MINIO_LAB_CONFIG_FILE="$SITE_A_CONFIG" \
MINIO_LAB_RUN_ID="${MINIO_LAB_RUN_ID:-site-replication-lab-$(date -u +%Y%m%dT%H%M%SZ)}" \
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
  "$SCRIPT_DIR/run-destructive-tests.sh"

cat <<EOF_SUMMARY
site replication lab 已完成。
- Site A endpoint：http://127.0.0.1:$SITE_A_API_PORT
- Site B endpoint：http://127.0.0.1:$SITE_B_API_PORT
- 临时运行目录 A：$SITE_A_RUN_DIR
- 临时运行目录 B：$SITE_B_RUN_DIR
- 临时请求体目录：$FIXTURE_DIR

清理命令：
  docker rm -f $SITE_A_NAME $SITE_B_NAME
  docker network rm $NETWORK_NAME
  rm -rf $SITE_A_RUN_DIR $SITE_B_RUN_DIR $FIXTURE_DIR
EOF_SUMMARY
