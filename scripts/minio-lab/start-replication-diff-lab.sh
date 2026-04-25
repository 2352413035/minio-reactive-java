#!/usr/bin/env bash
set -euo pipefail

# 启动两个一次性 Docker MinIO，并配置 bucket replication。
# 用途：为 ADMIN_REPLICATION_DIFF 提供真实复制拓扑；脚本不会输出 access key 或 secret key。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SOURCE_NAME="${MINIO_LAB_REPL_SOURCE_NAME:-minio-reactive-repl-src}"
TARGET_NAME="${MINIO_LAB_REPL_TARGET_NAME:-minio-reactive-repl-dst}"
NETWORK_NAME="${MINIO_LAB_REPL_NETWORK:-minio-reactive-repl-net}"
SOURCE_API_PORT="${MINIO_LAB_REPL_SOURCE_API_PORT:-19200}"
SOURCE_CONSOLE_PORT="${MINIO_LAB_REPL_SOURCE_CONSOLE_PORT:-19201}"
TARGET_API_PORT="${MINIO_LAB_REPL_TARGET_API_PORT:-19202}"
TARGET_CONSOLE_PORT="${MINIO_LAB_REPL_TARGET_CONSOLE_PORT:-19203}"
SOURCE_RUN_DIR="${MINIO_LAB_REPL_SOURCE_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-repl-src-XXXXXX)}"
TARGET_RUN_DIR="${MINIO_LAB_REPL_TARGET_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-repl-dst-XXXXXX)}"
MC_DIR="${MINIO_LAB_REPL_MC_DIR:-$(mktemp -d /tmp/minio-reactive-repl-mc-XXXXXX)}"
REPLICATION_PREFIX="${MINIO_LAB_REPLICATION_DIFF_PREFIX:-stage122/}"
OBJECT_FILE="$SOURCE_RUN_DIR/replication-diff-object.txt"

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "缺少命令：$1" >&2
    exit 1
  }
}

prop() {
  awk -F= -v k="$1" '$1==k {sub(/^[^=]*=/, ""); print; exit}' "$2"
}

cleanup_on_failure() {
  local exit_code=$?
  if [[ "$exit_code" -ne 0 ]]; then
    docker rm -f "$SOURCE_NAME" "$TARGET_NAME" >/dev/null 2>&1 || true
    docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
    rm -rf "$SOURCE_RUN_DIR" "$TARGET_RUN_DIR" "$MC_DIR"
  fi
  exit "$exit_code"
}
trap cleanup_on_failure EXIT

need_cmd docker
need_cmd mc
need_cmd awk
need_cmd curl
need_cmd python3

mkdir -p "$SOURCE_RUN_DIR" "$TARGET_RUN_DIR" "$MC_DIR"
chmod 700 "$SOURCE_RUN_DIR" "$TARGET_RUN_DIR" "$MC_DIR"

docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true

MINIO_LAB_DOCKER_NAME="$SOURCE_NAME" MINIO_LAB_DOCKER_API_PORT="$SOURCE_API_PORT" MINIO_LAB_DOCKER_CONSOLE_PORT="$SOURCE_CONSOLE_PORT" MINIO_LAB_DOCKER_RUN_DIR="$SOURCE_RUN_DIR" "$SCRIPT_DIR/start-docker-lab.sh"

MINIO_LAB_DOCKER_NAME="$TARGET_NAME" MINIO_LAB_DOCKER_API_PORT="$TARGET_API_PORT" MINIO_LAB_DOCKER_CONSOLE_PORT="$TARGET_CONSOLE_PORT" MINIO_LAB_DOCKER_RUN_DIR="$TARGET_RUN_DIR" "$SCRIPT_DIR/start-docker-lab.sh"

docker network create "$NETWORK_NAME" >/dev/null
docker network connect "$NETWORK_NAME" "$SOURCE_NAME" >/dev/null
docker network connect "$NETWORK_NAME" "$TARGET_NAME" >/dev/null

SOURCE_CONFIG="$SOURCE_RUN_DIR/lab.properties"
TARGET_CONFIG="$TARGET_RUN_DIR/lab.properties"
SOURCE_ENDPOINT="$(prop MINIO_LAB_ENDPOINT "$SOURCE_CONFIG")"
TARGET_ENDPOINT="$(prop MINIO_LAB_ENDPOINT "$TARGET_CONFIG")"
SOURCE_ACCESS_KEY="$(prop MINIO_LAB_ACCESS_KEY "$SOURCE_CONFIG")"
SOURCE_SECRET_KEY="$(prop MINIO_LAB_SECRET_KEY "$SOURCE_CONFIG")"
TARGET_ACCESS_KEY="$(prop MINIO_LAB_ACCESS_KEY "$TARGET_CONFIG")"
TARGET_SECRET_KEY="$(prop MINIO_LAB_SECRET_KEY "$TARGET_CONFIG")"
SOURCE_BUCKET="$(prop MINIO_LAB_BUCKET "$SOURCE_CONFIG")"
TARGET_BUCKET="$(prop MINIO_LAB_BUCKET "$TARGET_CONFIG")"

MC_CONFIG_DIR="$MC_DIR" mc alias set repl-src "$SOURCE_ENDPOINT" "$SOURCE_ACCESS_KEY" "$SOURCE_SECRET_KEY" >/dev/null
MC_CONFIG_DIR="$MC_DIR" mc alias set repl-dst "$TARGET_ENDPOINT" "$TARGET_ACCESS_KEY" "$TARGET_SECRET_KEY" >/dev/null
MC_CONFIG_DIR="$MC_DIR" mc version enable "repl-src/$SOURCE_BUCKET" >/dev/null
MC_CONFIG_DIR="$MC_DIR" mc version enable "repl-dst/$TARGET_BUCKET" >/dev/null

# remote-bucket 必须使用源 MinIO 容器可访问的 endpoint。这里使用 Docker 网络里的目标容器名，
# 不使用宿主机 127.0.0.1 映射端口，避免服务端复制任务连到自身。
REMOTE_BUCKET_URL="http://${TARGET_ACCESS_KEY}:${TARGET_SECRET_KEY}@${TARGET_NAME}:9000/${TARGET_BUCKET}"
MC_CONFIG_DIR="$MC_DIR" mc replicate add "repl-src/$SOURCE_BUCKET"   --remote-bucket "$REMOTE_BUCKET_URL"   --priority 1   --replicate 'existing-objects,delete,delete-marker,metadata-sync'   >/dev/null

printf 'stage122 replication diff lab object
' > "$OBJECT_FILE"
MC_CONFIG_DIR="$MC_DIR" mc cp "$OBJECT_FILE" "repl-src/$SOURCE_BUCKET/${REPLICATION_PREFIX}probe.txt" >/dev/null

replicated="false"
for _ in $(seq 1 40); do
  if MC_CONFIG_DIR="$MC_DIR" mc stat "repl-dst/$TARGET_BUCKET/${REPLICATION_PREFIX}probe.txt" >/dev/null 2>&1; then
    replicated="true"
    break
  fi
  sleep 1
done

if [[ "$replicated" != "true" ]]; then
  echo "复制对象未在预期时间内到达目标 MinIO；不会生成 replication diff lab 配置。" >&2
  exit 1
fi

cat >> "$SOURCE_CONFIG" <<EOF_CONFIG
MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE=true
MINIO_LAB_REPLICATION_DIFF_PREFIX=$REPLICATION_PREFIX
MINIO_LAB_REPLICATION_DIFF_ARN=
EOF_CONFIG
chmod 600 "$SOURCE_CONFIG"

cat > "$SOURCE_RUN_DIR/replication-diff-env" <<EOF_ENV
MINIO_LAB_CONFIG_FILE=$SOURCE_CONFIG
MINIO_LAB_REPL_SOURCE_NAME=$SOURCE_NAME
MINIO_LAB_REPL_TARGET_NAME=$TARGET_NAME
MINIO_LAB_REPL_NETWORK=$NETWORK_NAME
MINIO_LAB_REPL_SOURCE_RUN_DIR=$SOURCE_RUN_DIR
MINIO_LAB_REPL_TARGET_RUN_DIR=$TARGET_RUN_DIR
MINIO_LAB_REPL_MC_DIR=$MC_DIR
EOF_ENV
chmod 600 "$SOURCE_RUN_DIR/replication-diff-env"

trap - EXIT
cat <<EOF_SUMMARY
Replication diff 双 MinIO lab 已启动并完成初始对象复制。
- Source endpoint：$SOURCE_ENDPOINT
- Target endpoint：$TARGET_ENDPOINT
- Source bucket：$SOURCE_BUCKET
- Target bucket：$TARGET_BUCKET
- Prefix：$REPLICATION_PREFIX
- Source lab 配置：$SOURCE_CONFIG
- Source 运行目录：$SOURCE_RUN_DIR
- Target 运行目录：$TARGET_RUN_DIR

运行验证示例：
  MINIO_LAB_CONFIG_FILE=$SOURCE_CONFIG scripts/minio-lab/run-destructive-tests.sh

清理命令：
  docker rm -f $SOURCE_NAME $TARGET_NAME
  docker network rm $NETWORK_NAME
  rm -rf $SOURCE_RUN_DIR $TARGET_RUN_DIR $MC_DIR
EOF_SUMMARY
