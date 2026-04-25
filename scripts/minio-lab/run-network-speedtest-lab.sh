#!/usr/bin/env bash
set -euo pipefail

# 运行 net/site speedtest 独立 lab 探测。
# 默认采用“预期失败采证”模式，记录单节点或无复制拓扑时的服务端前置条件；
# 如需尝试真实通过，可把 MINIO_LAB_EXPECT_*_SPEEDTEST_FAILURE 改为 false。

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

LAB_NAME="${MINIO_LAB_DOCKER_NAME:-minio-reactive-net-site-speedtest-lab}"
LAB_API_PORT="${MINIO_LAB_DOCKER_API_PORT:-19360}"
LAB_CONSOLE_PORT="${MINIO_LAB_DOCKER_CONSOLE_PORT:-19361}"
LAB_RUN_DIR="${MINIO_LAB_DOCKER_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-net-site-speedtest-XXXXXX)}"

MINIO_LAB_DOCKER_NAME="$LAB_NAME" \
MINIO_LAB_DOCKER_API_PORT="$LAB_API_PORT" \
MINIO_LAB_DOCKER_CONSOLE_PORT="$LAB_CONSOLE_PORT" \
MINIO_LAB_DOCKER_RUN_DIR="$LAB_RUN_DIR" \
MINIO_LAB_ENABLE_SPEEDTEST_PROBES="${MINIO_LAB_ENABLE_SPEEDTEST_PROBES:-true}" \
MINIO_LAB_ENABLE_DRIVE_SPEEDTEST_PROBE="${MINIO_LAB_ENABLE_DRIVE_SPEEDTEST_PROBE:-false}" \
  "$SCRIPT_DIR/start-docker-lab.sh" >/dev/null

# shellcheck source=/dev/null
source "$LAB_RUN_DIR/env"

cat >> "$LAB_CONFIG_FILE" <<EOF_CONFIG
MINIO_LAB_ENABLE_NET_SPEEDTEST_PROBE=${MINIO_LAB_ENABLE_NET_SPEEDTEST_PROBE:-true}
MINIO_LAB_SPEEDTEST_NET_DURATION_SECONDS=${MINIO_LAB_SPEEDTEST_NET_DURATION_SECONDS:-2}
MINIO_LAB_EXPECT_NET_SPEEDTEST_FAILURE=${MINIO_LAB_EXPECT_NET_SPEEDTEST_FAILURE:-true}
MINIO_LAB_NET_SPEEDTEST_EXPECTED_ERROR=${MINIO_LAB_NET_SPEEDTEST_EXPECTED_ERROR:-NotImplemented|distributed}
MINIO_LAB_ENABLE_SITE_SPEEDTEST_PROBE=${MINIO_LAB_ENABLE_SITE_SPEEDTEST_PROBE:-true}
MINIO_LAB_SPEEDTEST_SITE_DURATION_SECONDS=${MINIO_LAB_SPEEDTEST_SITE_DURATION_SECONDS:-2}
MINIO_LAB_EXPECT_SITE_SPEEDTEST_FAILURE=${MINIO_LAB_EXPECT_SITE_SPEEDTEST_FAILURE:-true}
MINIO_LAB_SITE_SPEEDTEST_EXPECTED_ERROR=${MINIO_LAB_SITE_SPEEDTEST_EXPECTED_ERROR:-NotImplemented|replication}
EOF_CONFIG

cd "$REPO_ROOT"
MINIO_LAB_CONFIG_FILE="$LAB_CONFIG_FILE" \
MINIO_LAB_RUN_ID="${MINIO_LAB_RUN_ID:-net-site-speedtest-lab-$(date -u +%Y%m%dT%H%M%SZ)}" \
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
  "$SCRIPT_DIR/run-destructive-tests.sh"

cat <<EOF_SUMMARY
net/site speedtest lab 已完成。
- 容器名：$LAB_NAME
- MinIO endpoint：http://127.0.0.1:$LAB_API_PORT
- 临时运行目录：$LAB_RUN_DIR
- 报告目录：$LAB_REPORT_DIR

清理命令：
  docker rm -f $LAB_NAME
  rm -rf $LAB_RUN_DIR
EOF_SUMMARY
