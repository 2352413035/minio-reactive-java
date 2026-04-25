#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=load-config.sh
source "$SCRIPT_DIR/load-config.sh"

# 本脚本只做非破坏性准备度审计：它不会连接 MinIO，也不会执行写入测试。
# access key、secret key、请求体和签名只显示“是否已设置”，避免把敏感值写入终端或报告。
minio_lab_config_file_path() {
  if [[ -n "${MINIO_LAB_CONFIG_FILE:-}" ]]; then
    printf '%s' "$MINIO_LAB_CONFIG_FILE"
  else
    printf '%s/lab.properties' "$SCRIPT_DIR"
  fi
}

minio_lab_normalize_endpoint() {
  local value="${1%/}"
  printf '%s' "$value"
}

minio_lab_presence() {
  if [[ -n "${1:-}" ]]; then
    printf '已设置'
  else
    printf '未设置'
  fi
}

MINIO_LAB_WRITE_FIXTURE_VARS=(
  MINIO_LAB_ADD_TIER_BODY
  MINIO_LAB_ADD_TIER_BODY_FILE
  MINIO_LAB_EDIT_TIER_BODY
  MINIO_LAB_EDIT_TIER_BODY_FILE
  MINIO_LAB_SET_REMOTE_TARGET_BODY
  MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE
  MINIO_LAB_REMOVE_REMOTE_TARGET_ARN
  MINIO_LAB_BATCH_START_BODY
  MINIO_LAB_BATCH_START_BODY_FILE
  MINIO_LAB_BATCH_CANCEL_BODY
  MINIO_LAB_BATCH_CANCEL_BODY_FILE
  MINIO_LAB_SITE_REPLICATION_ADD_BODY
  MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE
  MINIO_LAB_SITE_REPLICATION_EDIT_BODY
  MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE
  MINIO_LAB_SITE_REPLICATION_REMOVE_BODY
  MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE
)

minio_lab_write_fixture_present() {
  local var_name
  for var_name in "${MINIO_LAB_WRITE_FIXTURE_VARS[@]}"; do
    if [[ -n "${!var_name:-}" ]]; then
      return 0
    fi
  done
  return 1
}

config_file="$(minio_lab_config_file_path)"
load_minio_lab_config
endpoint="$(minio_lab_normalize_endpoint "${MINIO_LAB_ENDPOINT:-}")"

printf 'MinIO 破坏性实验环境准备度审计\n'
printf '================================\n'
if [[ -f "$config_file" ]]; then
  printf -- '- 配置文件：已找到 `%s`\n' "$config_file"
else
  printf -- '- 配置文件：未找到 `%s`（仍可完全使用环境变量）\n' "$config_file"
fi
printf -- '- 显式开关 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS：`%s`\n' "${MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS:-未设置}"
printf -- '- Lab 端点：`%s`\n' "${endpoint:-未设置}"
printf -- '- Lab access key：%s\n' "$(minio_lab_presence "${MINIO_LAB_ACCESS_KEY:-}")"
printf -- '- Lab secret key：%s\n' "$(minio_lab_presence "${MINIO_LAB_SECRET_KEY:-}")"
printf -- '- 可回滚声明 MINIO_LAB_CAN_RESTORE：`%s`\n' "${MINIO_LAB_CAN_RESTORE:-未设置}"
printf -- '- 写入夹具总开关 MINIO_LAB_ALLOW_WRITE_FIXTURES：`%s`\n' "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-false}"
if minio_lab_write_fixture_present; then
  printf -- '- 写入夹具请求体/删除 ARN：已检测到（不输出内容）\n'
else
  printf -- '- 写入夹具请求体/删除 ARN：未检测到\n'
fi

printf '\n门禁校验结果\n'
printf -- '------------\n'
verify_output="$(mktemp)"
trap 'rm -f "$verify_output"' EXIT
if "$SCRIPT_DIR/verify-env.sh" >"$verify_output" 2>&1; then
  printf '通过：当前配置满足启动破坏性 lab 测试的门禁。\n'
  printf '提示：这只代表门禁通过，不代表 typed/raw 破坏性矩阵已经执行；真实执行仍需运行 `scripts/minio-lab/run-destructive-tests.sh` 并检查报告。\n'
else
  printf '拒绝：当前配置不能启动破坏性 lab 测试。原因如下：\n'
  sed 's/^/  /' "$verify_output"
  printf '提示：如果你正在准备 tier、remote target、batch job 或 site replication 夹具，请运行 `scripts/minio-lab/audit-fixtures.sh` 查看逐项缺口。\n'
  exit 1
fi
