#!/usr/bin/env bash
set -euo pipefail

# 破坏性实验环境报告生成器。
# 报告只记录环境指纹和夹具开关，不写入 access key、secret key 或请求签名。

minio_lab_sanitize_endpoint() {
  local endpoint="${1:-}"
  endpoint="${endpoint%%\?*}"
  endpoint="${endpoint%%#*}"
  endpoint="${endpoint%/}"
  if [[ "$endpoint" =~ ^(https?://)([^/@]+@)?([^/]+) ]]; then
    printf '%s%s' "${BASH_REMATCH[1]}" "${BASH_REMATCH[3]}"
  else
    printf '<未设置或格式异常>'
  fi
}

minio_lab_bool() {
  local value="${1:-}"
  if [[ -n "$value" ]]; then
    printf '已设置'
  else
    printf '未设置'
  fi
}

minio_lab_fixture_enabled() {
  local label="$1"
  local enabled="$2"
  if [[ "$enabled" == "true" ]]; then
    printf -- '- %s：启用\n' "$label"
  else
    printf -- '- %s：跳过\n' "$label"
  fi
}

write_minio_lab_report() {
  local result="${1:-未知}"
  local exit_code="${2:-}"
  local detail="${3:-}"
  local repo_root="${REPO_ROOT:-$(pwd)}"
  local report_dir="${MINIO_LAB_REPORT_DIR:-$repo_root/target/minio-lab-reports}"
  local timestamp
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  mkdir -p "$report_dir"
  local report_file="$report_dir/destructive-lab-$timestamp.md"

  local config_enabled quota_enabled remote_enabled tier_enabled batch_enabled tier_write_enabled remote_write_enabled
  config_enabled="false"
  quota_enabled="false"
  remote_enabled="false"
  tier_enabled="false"
  batch_enabled="false"
  tier_write_enabled="false"
  remote_write_enabled="false"

  if [[ -n "${MINIO_LAB_TEST_CONFIG_KV:-}" && -n "${MINIO_LAB_RESTORE_CONFIG_KV:-}" ]]; then
    config_enabled="true"
  fi
  if [[ -n "${MINIO_LAB_BUCKET:-}" && -n "${MINIO_LAB_TEST_BUCKET_QUOTA_JSON:-}" && -n "${MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON:-}" ]]; then
    quota_enabled="true"
  fi
  if [[ -n "${MINIO_LAB_BUCKET:-}" ]]; then
    remote_enabled="true"
  fi
  if [[ -n "${MINIO_LAB_TIER_NAME:-}" ]]; then
    tier_enabled="true"
  fi
  if [[ "${MINIO_LAB_ENABLE_BATCH_JOB_PROBES:-}" == "true" ]]; then
    batch_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" == "true" \
    && -n "${MINIO_LAB_TIER_WRITE_NAME:-}" \
    && -n "${MINIO_LAB_ADD_TIER_BODY:-}" \
    && "${MINIO_LAB_REMOVE_TIER_AFTER_TEST:-}" == "true" ]]; then
    tier_write_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" == "true" \
    && -n "${MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET:-}" \
    && -n "${MINIO_LAB_SET_REMOTE_TARGET_BODY:-}" \
    && -n "${MINIO_LAB_REMOVE_REMOTE_TARGET_ARN:-}" \
    && "${MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST:-}" == "true" ]]; then
    remote_write_enabled="true"
  fi

  {
    printf '# MinIO 破坏性实验环境执行报告\n\n'
    printf -- '- 生成时间（UTC）：`%s`\n' "$timestamp"
    printf -- '- 执行结果：`%s`\n' "$result"
    printf -- '- 退出码：`%s`\n' "${exit_code:-未记录}"
    printf -- '- 说明：%s\n' "${detail:-无}"
    printf -- '- Lab 端点：`%s`\n' "$(minio_lab_sanitize_endpoint "${MINIO_LAB_ENDPOINT:-}")"
    printf -- '- Java Home：`%s`\n' "${JAVA_HOME:-未显式设置}"
    printf -- '- Maven 测试：`DestructiveAdminIntegrationTest`\n\n'

    printf '## 夹具开关\n\n'
    minio_lab_fixture_enabled 'config KV 写入 + 恢复' "$config_enabled"
    minio_lab_fixture_enabled 'bucket quota 写入 + 恢复' "$quota_enabled"
    minio_lab_fixture_enabled 'tier typed/raw 探测' "$tier_enabled"
    minio_lab_fixture_enabled 'remote target typed/raw 探测' "$remote_enabled"
    minio_lab_fixture_enabled 'batch job typed/raw 探测' "$batch_enabled"
    minio_lab_fixture_enabled 'tier add/edit/remove 写入 + 恢复' "$tier_write_enabled"
    minio_lab_fixture_enabled 'remote target set/remove 写入 + 恢复' "$remote_write_enabled"
    printf '\n'

    printf '## 夹具指纹（不含凭证）\n\n'
    printf -- '- 配置文件：`%s`\n' "${MINIO_LAB_CONFIG_FILE:-未使用}"
    printf -- '- bucket：`%s`\n' "${MINIO_LAB_BUCKET:-未设置}"
    printf -- '- tier 名称：`%s`\n' "${MINIO_LAB_TIER_NAME:-未设置}"
    printf -- '- tier 写入名称：`%s`\n' "${MINIO_LAB_TIER_WRITE_NAME:-未设置}"
    printf -- '- tier add 请求体：%s\n' "$(minio_lab_bool "${MINIO_LAB_ADD_TIER_BODY:-}")"
    printf -- '- tier edit 请求体：%s\n' "$(minio_lab_bool "${MINIO_LAB_EDIT_TIER_BODY:-}")"
    printf -- '- remote target 类型：`%s`\n' "${MINIO_LAB_REMOTE_TARGET_TYPE:-replication}"
    printf -- '- remote target 写入 bucket：`%s`\n' "${MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET:-未设置}"
    printf -- '- remote target set 请求体：%s\n' "$(minio_lab_bool "${MINIO_LAB_SET_REMOTE_TARGET_BODY:-}")"
    printf -- '- remote target 预期 ARN：%s\n' "$(minio_lab_bool "${MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN:-}")"
    printf -- '- remote target 删除 ARN：%s\n' "$(minio_lab_bool "${MINIO_LAB_REMOVE_REMOTE_TARGET_ARN:-}")"
    printf -- '- 写入夹具总开关：`%s`\n' "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-false}"
    printf -- '- batch job 预期 ID：%s\n\n' "$(minio_lab_bool "${MINIO_LAB_BATCH_EXPECTED_JOB_ID:-}")"

    printf '## 失败恢复提示\n\n'
    printf '1. 如果 config KV 用例失败，使用 `MINIO_LAB_RESTORE_CONFIG_KV` 对应值恢复服务配置。\n'
    printf '2. 如果 bucket quota 用例失败，使用 `MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON` 对应值恢复 bucket quota。\n'
    printf '3. 如果 tier 写入夹具失败，优先执行 `MINIO_LAB_REMOVE_TIER_AFTER_TEST=true` 对应的 tier 删除恢复。\n'
    printf '4. 如果 remote target 写入夹具失败，优先使用 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` 删除刚写入的 target。\n'
    printf '5. 如果 remote target、tier 或 batch job 探测失败，先查看 MinIO 管理日志，再用独立 lab 的控制台或 `mc admin` 回滚。\n'
    printf '6. 不要把本报告复制到仓库；报告可能包含 lab 端点和资源名称，但不会包含凭证。\n'
  } > "$report_file"

  printf 'MinIO 破坏性实验环境报告已生成：%s\n' "$report_file"
}
