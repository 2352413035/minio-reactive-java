#!/usr/bin/env bash
set -euo pipefail

# 启动一次性 Docker MinIO 破坏性实验环境。
# 脚本会生成临时 root 凭证并写入 /tmp 下的私有配置文件，不会把凭证输出到终端或仓库。

LAB_NAME="${MINIO_LAB_DOCKER_NAME:-minio-reactive-destructive-lab}"
LAB_API_PORT="${MINIO_LAB_DOCKER_API_PORT:-19000}"
LAB_CONSOLE_PORT="${MINIO_LAB_DOCKER_CONSOLE_PORT:-19001}"
LAB_BUCKET="${MINIO_LAB_DOCKER_BUCKET:-reactive-lab-bucket}"
LAB_RUN_DIR="${MINIO_LAB_DOCKER_RUN_DIR:-$(mktemp -d /tmp/minio-reactive-lab-XXXXXX)}"
LAB_CONFIG_FILE="$LAB_RUN_DIR/lab.properties"
LAB_ENV_FILE="$LAB_RUN_DIR/env"
LAB_MC_DIR="$LAB_RUN_DIR/mc"
LAB_REPORT_DIR="$LAB_RUN_DIR/reports"
LAB_ACCESS_KEY="labadmin$(date +%s)"
LAB_SECRET_KEY="$(python3 - <<'PY'
import secrets
import string
alphabet = string.ascii_letters + string.digits
print('lab-' + ''.join(secrets.choice(alphabet) for _ in range(32)))
PY
)"

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

mkdir -p "$LAB_RUN_DIR" "$LAB_MC_DIR" "$LAB_REPORT_DIR"
chmod 700 "$LAB_RUN_DIR"

docker rm -f "$LAB_NAME" >/dev/null 2>&1 || true
docker run -d --name "$LAB_NAME" \
  -p "127.0.0.1:${LAB_API_PORT}:9000" \
  -p "127.0.0.1:${LAB_CONSOLE_PORT}:9001" \
  -e MINIO_ROOT_USER="$LAB_ACCESS_KEY" \
  -e MINIO_ROOT_PASSWORD="$LAB_SECRET_KEY" \
  minio/minio server /data --console-address ':9001' >/dev/null

for i in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${LAB_API_PORT}/minio/health/ready" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ "$i" -eq 60 ]]; then
    echo "Docker MinIO lab 未在预期时间内 ready。" >&2
    docker logs "$LAB_NAME" --tail 80 >&2 || true
    exit 1
  fi
done

MC_CONFIG_DIR="$LAB_MC_DIR" mc alias set reactive-lab "http://127.0.0.1:${LAB_API_PORT}" "$LAB_ACCESS_KEY" "$LAB_SECRET_KEY" >/dev/null
MC_CONFIG_DIR="$LAB_MC_DIR" mc mb --ignore-existing "reactive-lab/${LAB_BUCKET}" >/dev/null

cat > "$LAB_CONFIG_FILE" <<EOF_CONFIG
MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true
MINIO_LAB_ENDPOINT=http://127.0.0.1:${LAB_API_PORT}
MINIO_LAB_ACCESS_KEY=$LAB_ACCESS_KEY
MINIO_LAB_SECRET_KEY=$LAB_SECRET_KEY
MINIO_LAB_REGION=us-east-1
MINIO_LAB_CAN_RESTORE=true
MINIO_LAB_TEST_CONFIG_KV=api requests_max=10
MINIO_LAB_RESTORE_CONFIG_KV=api requests_max=0
MINIO_LAB_ALLOW_FULL_CONFIG_WRITE=${MINIO_LAB_ALLOW_FULL_CONFIG_WRITE:-false}
MINIO_LAB_BUCKET=$LAB_BUCKET
MINIO_LAB_TEST_BUCKET_QUOTA_JSON={"quota":1048576,"quotatype":"hard"}
MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON={"quota":0,"quotatype":"hard"}
MINIO_LAB_REMOTE_TARGET_TYPE=replication
MINIO_LAB_ENABLE_BATCH_JOB_PROBES=false
MINIO_LAB_ENABLE_SPEEDTEST_PROBES=${MINIO_LAB_ENABLE_SPEEDTEST_PROBES:-false}
MINIO_LAB_SPEEDTEST_OBJECT_SIZE=${MINIO_LAB_SPEEDTEST_OBJECT_SIZE:-1048576}
MINIO_LAB_SPEEDTEST_OBJECT_CONCURRENCY=${MINIO_LAB_SPEEDTEST_OBJECT_CONCURRENCY:-1}
MINIO_LAB_SPEEDTEST_OBJECT_DURATION_SECONDS=${MINIO_LAB_SPEEDTEST_OBJECT_DURATION_SECONDS:-2}
MINIO_LAB_ENABLE_DRIVE_SPEEDTEST_PROBE=${MINIO_LAB_ENABLE_DRIVE_SPEEDTEST_PROBE:-false}
MINIO_LAB_SPEEDTEST_DRIVE_BLOCK_SIZE=${MINIO_LAB_SPEEDTEST_DRIVE_BLOCK_SIZE:-4096}
MINIO_LAB_SPEEDTEST_DRIVE_FILE_SIZE=${MINIO_LAB_SPEEDTEST_DRIVE_FILE_SIZE:-8192}
MINIO_LAB_ALLOW_WRITE_FIXTURES=false
MINIO_LAB_REPORT_DIR=$LAB_REPORT_DIR
MINIO_LAB_MC_ALIAS=reactive-lab
EOF_CONFIG
chmod 600 "$LAB_CONFIG_FILE"

cat > "$LAB_ENV_FILE" <<EOF_ENV
LAB_NAME=$LAB_NAME
LAB_API_PORT=$LAB_API_PORT
LAB_CONSOLE_PORT=$LAB_CONSOLE_PORT
LAB_BUCKET=$LAB_BUCKET
LAB_RUN_DIR=$LAB_RUN_DIR
LAB_CONFIG_FILE=$LAB_CONFIG_FILE
LAB_MC_DIR=$LAB_MC_DIR
LAB_REPORT_DIR=$LAB_REPORT_DIR
EOF_ENV
chmod 600 "$LAB_ENV_FILE"

cat <<EOF_SUMMARY
独立 Docker MinIO lab 已启动。
- 容器名：$LAB_NAME
- Endpoint：http://127.0.0.1:$LAB_API_PORT
- Console：http://127.0.0.1:$LAB_CONSOLE_PORT
- Bucket：$LAB_BUCKET
- 临时配置：$LAB_CONFIG_FILE
- 临时运行目录：$LAB_RUN_DIR

运行破坏性 lab 测试示例：
  MINIO_LAB_CONFIG_FILE=$LAB_CONFIG_FILE scripts/minio-lab/run-destructive-tests.sh

清理命令：
  docker rm -f $LAB_NAME
  rm -rf $LAB_RUN_DIR
EOF_SUMMARY
