#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "$1" >&2
  exit 1
}

normalize_endpoint() {
  local value="${1%/}"
  printf '%s' "$value"
}

[[ "${MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS:-}" == "true" ]] || fail "必须显式设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true 才能执行 destructive Admin 测试。"
[[ -n "${MINIO_LAB_ENDPOINT:-}" ]] || fail "缺少 MINIO_LAB_ENDPOINT。"
[[ -n "${MINIO_LAB_ACCESS_KEY:-}" ]] || fail "缺少 MINIO_LAB_ACCESS_KEY。"
[[ -n "${MINIO_LAB_SECRET_KEY:-}" ]] || fail "缺少 MINIO_LAB_SECRET_KEY。"
[[ "${MINIO_LAB_CAN_RESTORE:-}" == "true" ]] || fail "缺少 MINIO_LAB_CAN_RESTORE=true，无法证明实验环境具备回滚能力。"

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

echo "MinIO destructive lab 环境校验通过：$endpoint"
