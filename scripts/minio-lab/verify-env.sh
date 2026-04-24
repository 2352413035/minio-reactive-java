#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=load-config.sh
source "$SCRIPT_DIR/load-config.sh"
load_minio_lab_config

fail() {
  echo "$1" >&2
  exit 1
}

normalize_endpoint() {
  local value="${1%/}"
  printf '%s' "$value"
}

has_write_fixture_config() {
  [[ -n "${MINIO_LAB_ADD_TIER_BODY:-}" \
    || -n "${MINIO_LAB_ADD_TIER_BODY_FILE:-}" \
    || -n "${MINIO_LAB_EDIT_TIER_BODY:-}" \
    || -n "${MINIO_LAB_EDIT_TIER_BODY_FILE:-}" \
    || -n "${MINIO_LAB_SET_REMOTE_TARGET_BODY:-}" \
    || -n "${MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE:-}" \
    || -n "${MINIO_LAB_REMOVE_REMOTE_TARGET_ARN:-}" \
    || -n "${MINIO_LAB_BATCH_START_BODY:-}" \
    || -n "${MINIO_LAB_BATCH_START_BODY_FILE:-}" \
    || -n "${MINIO_LAB_BATCH_CANCEL_BODY:-}" \
    || -n "${MINIO_LAB_BATCH_CANCEL_BODY_FILE:-}" \
    || -n "${MINIO_LAB_SITE_REPLICATION_ADD_BODY:-}" \
    || -n "${MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE:-}" \
    || -n "${MINIO_LAB_SITE_REPLICATION_EDIT_BODY:-}" \
    || -n "${MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE:-}" \
    || -n "${MINIO_LAB_SITE_REPLICATION_REMOVE_BODY:-}" \
    || -n "${MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE:-}" ]]
}

[[ "${MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS:-}" == "true" ]] || fail "必须显式设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true 才能执行 破坏性 Admin 测试。"
[[ -n "${MINIO_LAB_ENDPOINT:-}" ]] || fail "缺少 MINIO_LAB_ENDPOINT。"
[[ -n "${MINIO_LAB_ACCESS_KEY:-}" ]] || fail "缺少 MINIO_LAB_ACCESS_KEY。"
[[ -n "${MINIO_LAB_SECRET_KEY:-}" ]] || fail "缺少 MINIO_LAB_SECRET_KEY。"
[[ "${MINIO_LAB_CAN_RESTORE:-}" == "true" ]] || fail "缺少 MINIO_LAB_CAN_RESTORE=true，无法证明实验环境具备回滚能力。"

if has_write_fixture_config && [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" != "true" ]]; then
  fail "检测到 tier/remote target/batch/site replication 写入夹具，但缺少 MINIO_LAB_ALLOW_WRITE_FIXTURES=true。"
fi

endpoint="$(normalize_endpoint "$MINIO_LAB_ENDPOINT")"
[[ "$endpoint" =~ ^https?:// ]] || fail "MINIO_LAB_ENDPOINT 必须是 http:// 或 https:// 开头。"
case "$endpoint" in
  http://127.0.0.1:9000|http://localhost:9000|http://0.0.0.0:9000)
    fail "MINIO_LAB_ENDPOINT 不能指向共享环境或常见本机默认环境：$endpoint。"
    ;;
esac

if [[ -n "${MINIO_ENDPOINT:-}" ]]; then
  normal_endpoint="$(normalize_endpoint "$MINIO_ENDPOINT")"
  [[ "$endpoint" != "$normal_endpoint" ]] || fail "MINIO_LAB_ENDPOINT 不能与 MINIO_ENDPOINT 指向同一环境。"
fi

echo "MinIO 破坏性实验环境校验通过：$endpoint"
