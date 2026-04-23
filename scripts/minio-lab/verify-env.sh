#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "$1" >&2
  exit 1
}

[[ "${MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS:-}" == "true" ]] || fail "必须显式设置 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS=true 才能执行 destructive Admin 测试。"
[[ -n "${MINIO_LAB_ENDPOINT:-}" ]] || fail "缺少 MINIO_LAB_ENDPOINT。"
[[ -n "${MINIO_LAB_ACCESS_KEY:-}" ]] || fail "缺少 MINIO_LAB_ACCESS_KEY。"
[[ -n "${MINIO_LAB_SECRET_KEY:-}" ]] || fail "缺少 MINIO_LAB_SECRET_KEY。"
[[ "${MINIO_LAB_CAN_RESTORE:-}" == "true" ]] || fail "缺少 MINIO_LAB_CAN_RESTORE=true，无法证明实验环境具备回滚能力。"
[[ "$MINIO_LAB_ENDPOINT" != "http://127.0.0.1:9000" ]] || fail "MINIO_LAB_ENDPOINT 不能指向共享环境 http://127.0.0.1:9000。"
[[ "$MINIO_LAB_ENDPOINT" =~ ^https?:// ]] || fail "MINIO_LAB_ENDPOINT 必须是 http:// 或 https:// 开头。"

echo "MinIO destructive lab 环境校验通过：$MINIO_LAB_ENDPOINT"
