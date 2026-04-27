#!/usr/bin/env bash
set -euo pipefail

# 启动四节点分布式 Docker MinIO，用于验证需要 erasure/distributed 拓扑的
# net speedtest 与 force-unlock typed/raw 路径。

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

LAB_PREFIX="${MINIO_LAB_DIST_PREFIX:-minio-reactive-dist}"
NETWORK_NAME="${MINIO_LAB_DIST_NETWORK:-${LAB_PREFIX}-net}"
API_BASE="${MINIO_LAB_DIST_API_BASE:-19500}"
CONSOLE_BASE="${MINIO_LAB_DIST_CONSOLE_BASE:-19520}"
LAB_RUN_DIR="${MINIO_LAB_DIST_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-dist-XXXXXX)}"
LAB_CONFIG_FILE="$LAB_RUN_DIR/lab.properties"
LAB_ENV_FILE="$LAB_RUN_DIR/env"
LAB_MC_DIR="$LAB_RUN_DIR/mc"
LAB_REPORT_DIR="$LAB_RUN_DIR/reports"
LAB_BUCKET="${MINIO_LAB_DOCKER_BUCKET:-reactive-lab-bucket}"
LAB_ACCESS_KEY="distadmin$(date +%s)"
LAB_SECRET_KEY="$(python3 - <<'PY'
import secrets
import string
alphabet = string.ascii_letters + string.digits
print('dist-' + ''.join(secrets.choice(alphabet) for _ in range(32)))
PY
)"

cleanup_on_setup_failure() {
  local exit_code=$?
  if [[ "$exit_code" -ne 0 ]]; then
    for i in 1 2 3 4; do
      docker rm -f "${LAB_PREFIX}-${i}" >/dev/null 2>&1 || true
    done
    docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
    rm -rf "$LAB_RUN_DIR"
  fi
  exit "$exit_code"
}
trap cleanup_on_setup_failure EXIT

mkdir -p "$LAB_RUN_DIR" "$LAB_MC_DIR" "$LAB_REPORT_DIR"
chmod 700 "$LAB_RUN_DIR"

docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
docker network create "$NETWORK_NAME" >/dev/null

for i in 1 2 3 4; do
  docker rm -f "${LAB_PREFIX}-${i}" >/dev/null 2>&1 || true
done

for i in 1 2 3 4; do
  api_port=$((API_BASE + i - 1))
  console_port=$((CONSOLE_BASE + i - 1))
  docker run -d --name "${LAB_PREFIX}-${i}" \
    --network "$NETWORK_NAME" \
    -p "127.0.0.1:${api_port}:9000" \
    -p "127.0.0.1:${console_port}:9001" \
    -e MINIO_ROOT_USER="$LAB_ACCESS_KEY" \
    -e MINIO_ROOT_PASSWORD="$LAB_SECRET_KEY" \
    minio/minio server --console-address ':9001' \
      "http://${LAB_PREFIX}-1/data" \
      "http://${LAB_PREFIX}-2/data" \
      "http://${LAB_PREFIX}-3/data" \
      "http://${LAB_PREFIX}-4/data" >/dev/null
done

LAB_ENDPOINT="http://127.0.0.1:${API_BASE}"
for i in $(seq 1 120); do
  if curl -fsS "$LAB_ENDPOINT/minio/health/ready" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ "$i" -eq 120 ]]; then
    echo "分布式 Docker MinIO lab 未在预期时间内 ready。" >&2
    for node in 1 2 3 4; do
      docker logs "${LAB_PREFIX}-${node}" --tail 60 >&2 || true
    done
    exit 1
  fi
done

for i in $(seq 1 60); do
  if MC_CONFIG_DIR="$LAB_MC_DIR" mc alias set distlab "$LAB_ENDPOINT" "$LAB_ACCESS_KEY" "$LAB_SECRET_KEY" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ "$i" -eq 60 ]]; then
    echo "分布式 Docker MinIO lab 已 ready 但 mc alias 初始化仍失败。" >&2
    exit 1
  fi
done

for i in $(seq 1 30); do
  if MC_CONFIG_DIR="$LAB_MC_DIR" mc mb --ignore-existing "distlab/${LAB_BUCKET}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ "$i" -eq 30 ]]; then
    echo "分布式 Docker MinIO lab 无法创建测试 bucket。" >&2
    exit 1
  fi
done

cat > "$LAB_CONFIG_FILE" <<EOF_CONFIG
MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true
MINIO_LAB_ENDPOINT=$LAB_ENDPOINT
MINIO_LAB_ACCESS_KEY=$LAB_ACCESS_KEY
MINIO_LAB_SECRET_KEY=$LAB_SECRET_KEY
MINIO_LAB_REGION=us-east-1
MINIO_LAB_CAN_RESTORE=true
MINIO_LAB_TEST_CONFIG_KV=api requests_max=10
MINIO_LAB_RESTORE_CONFIG_KV=api requests_max=0
MINIO_LAB_BUCKET=$LAB_BUCKET
MINIO_LAB_TEST_BUCKET_QUOTA_JSON={"quota":1048576,"quotatype":"hard"}
MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON={"quota":0,"quotatype":"hard"}
MINIO_LAB_REMOTE_TARGET_TYPE=replication
MINIO_LAB_ENABLE_SPEEDTEST_PROBES=true
MINIO_LAB_SPEEDTEST_OBJECT_SIZE=${MINIO_LAB_SPEEDTEST_OBJECT_SIZE:-1048576}
MINIO_LAB_SPEEDTEST_OBJECT_CONCURRENCY=${MINIO_LAB_SPEEDTEST_OBJECT_CONCURRENCY:-1}
MINIO_LAB_SPEEDTEST_OBJECT_DURATION_SECONDS=${MINIO_LAB_SPEEDTEST_OBJECT_DURATION_SECONDS:-2}
MINIO_LAB_ENABLE_NET_SPEEDTEST_PROBE=true
MINIO_LAB_SPEEDTEST_NET_DURATION_SECONDS=${MINIO_LAB_SPEEDTEST_NET_DURATION_SECONDS:-2}
MINIO_LAB_EXPECT_NET_SPEEDTEST_FAILURE=false
MINIO_LAB_ENABLE_SITE_SPEEDTEST_PROBE=false
MINIO_LAB_ENABLE_SERVICE_CONTROL_PROBES=false
MINIO_LAB_ENABLE_MAINTENANCE_BOUNDARY_PROBES=false
MINIO_LAB_SERVER_UPDATE_URL=${MINIO_LAB_SERVER_UPDATE_URL:-http://127.0.0.1:1/minio-release-info}
MINIO_LAB_SERVER_UPDATE_EXPECTED_ERROR=${MINIO_LAB_SERVER_UPDATE_EXPECTED_ERROR:-method not allowed|in-place update|connection refused|connect}
MINIO_LAB_ENABLE_FORCE_UNLOCK_PROBE=true
MINIO_LAB_FORCE_UNLOCK_PATHS=${MINIO_LAB_FORCE_UNLOCK_PATHS:-${LAB_BUCKET}/no-such-lock}
MINIO_LAB_EXPECT_FORCE_UNLOCK_FAILURE=false
MINIO_LAB_ALLOW_WRITE_FIXTURES=false
MINIO_LAB_REPORT_DIR=$LAB_REPORT_DIR
MINIO_LAB_MC_ALIAS=distlab
EOF_CONFIG
chmod 600 "$LAB_CONFIG_FILE"

cat > "$LAB_ENV_FILE" <<EOF_ENV
LAB_PREFIX=$LAB_PREFIX
LAB_NETWORK=$NETWORK_NAME
LAB_ENDPOINT=$LAB_ENDPOINT
LAB_BUCKET=$LAB_BUCKET
LAB_RUN_DIR=$LAB_RUN_DIR
LAB_CONFIG_FILE=$LAB_CONFIG_FILE
LAB_MC_DIR=$LAB_MC_DIR
LAB_REPORT_DIR=$LAB_REPORT_DIR
EOF_ENV
chmod 600 "$LAB_ENV_FILE"

trap - EXIT

cd "$REPO_ROOT"
MINIO_LAB_CONFIG_FILE="$LAB_CONFIG_FILE" \
MINIO_LAB_RUN_ID="${MINIO_LAB_RUN_ID:-distributed-maintenance-lab-$(date -u +%Y%m%dT%H%M%SZ)}" \
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
  "$SCRIPT_DIR/run-destructive-tests.sh"

cat <<EOF_SUMMARY
distributed maintenance lab 已完成。
- 容器前缀：$LAB_PREFIX
- MinIO endpoint：$LAB_ENDPOINT
- 临时运行目录：$LAB_RUN_DIR
- 报告目录：$LAB_REPORT_DIR

清理命令：
  docker rm -f ${LAB_PREFIX}-1 ${LAB_PREFIX}-2 ${LAB_PREFIX}-3 ${LAB_PREFIX}-4
  docker network rm $NETWORK_NAME
  rm -rf $LAB_RUN_DIR
EOF_SUMMARY
