#!/usr/bin/env bash
set -euo pipefail

# 运行 service restart / server update 前置条件 / force-unlock 单节点边界 lab。
# 凭证和请求细节只写入 /tmp 私有目录，不输出到终端或仓库。

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

LAB_NAME="${MINIO_LAB_DOCKER_NAME:-minio-reactive-maintenance-lab}"
LAB_API_PORT="${MINIO_LAB_DOCKER_API_PORT:-19410}"
LAB_CONSOLE_PORT="${MINIO_LAB_DOCKER_CONSOLE_PORT:-19411}"
LAB_RUN_DIR="${MINIO_LAB_DOCKER_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-maintenance-XXXXXX)}"

MINIO_LAB_DOCKER_NAME="$LAB_NAME" \
MINIO_LAB_DOCKER_API_PORT="$LAB_API_PORT" \
MINIO_LAB_DOCKER_CONSOLE_PORT="$LAB_CONSOLE_PORT" \
MINIO_LAB_DOCKER_RUN_DIR="$LAB_RUN_DIR" \
MINIO_LAB_ENABLE_SERVICE_CONTROL_PROBES="${MINIO_LAB_ENABLE_SERVICE_CONTROL_PROBES:-true}" \
MINIO_LAB_ENABLE_MAINTENANCE_BOUNDARY_PROBES="${MINIO_LAB_ENABLE_MAINTENANCE_BOUNDARY_PROBES:-true}" \
MINIO_LAB_ENABLE_FORCE_UNLOCK_PROBE="${MINIO_LAB_ENABLE_FORCE_UNLOCK_PROBE:-true}" \
MINIO_LAB_EXPECT_FORCE_UNLOCK_FAILURE="${MINIO_LAB_EXPECT_FORCE_UNLOCK_FAILURE:-true}" \
MINIO_LAB_FORCE_UNLOCK_EXPECTED_ERROR="${MINIO_LAB_FORCE_UNLOCK_EXPECTED_ERROR:-not implemented|404|not found|versionmismatch|mode-server-xl-single}" \
  "$SCRIPT_DIR/start-docker-lab.sh" >/dev/null

# shellcheck source=/dev/null
source "$LAB_RUN_DIR/env"

cd "$REPO_ROOT"
MINIO_LAB_CONFIG_FILE="$LAB_CONFIG_FILE" \
MINIO_LAB_RUN_ID="${MINIO_LAB_RUN_ID:-maintenance-boundary-lab-$(date -u +%Y%m%dT%H%M%SZ)}" \
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
  "$SCRIPT_DIR/run-destructive-tests.sh"

cat <<EOF_SUMMARY
maintenance boundary lab 已完成。
- 容器名：$LAB_NAME
- MinIO endpoint：http://127.0.0.1:$LAB_API_PORT
- 临时运行目录：$LAB_RUN_DIR
- 报告目录：$LAB_REPORT_DIR

清理命令：
  docker rm -f $LAB_NAME
  rm -rf $LAB_RUN_DIR
EOF_SUMMARY
