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

minio_lab_bool_any() {
  local first="${1:-}"
  local second="${2:-}"
  if [[ -n "$first" || -n "$second" ]]; then
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

minio_lab_safe_cell() {
  local value="${1:-}"
  value="${value//$'\r'/ }"
  value="${value//$'\n'/ }"
  value="${value//|/／}"
  value="${value//\`/’}"
  if [[ "${#value}" -gt 160 ]]; then
    value="${value:0:160}…"
  fi
  printf '%s' "$value"
}

minio_lab_step_file() {
  local repo_root="${REPO_ROOT:-$(pwd)}"
  local report_dir="${MINIO_LAB_REPORT_DIR:-$repo_root/target/minio-lab-reports}"
  local timestamp="${MINIO_LAB_RUN_ID:-}"
  if [[ -n "${MINIO_LAB_STEP_STATUS_FILE:-}" ]]; then
    printf '%s' "$MINIO_LAB_STEP_STATUS_FILE"
  elif [[ -n "$timestamp" ]]; then
    printf '%s/destructive-lab-%s.steps' "$report_dir" "$timestamp"
  else
    printf ''
  fi
}

minio_lab_render_step_details() {
  local step_file="$1"
  if [[ -z "$step_file" || ! -s "$step_file" ]]; then
    printf '未记录 typed/raw 步骤明细。通常表示测试在门禁前退出，或没有执行可选写入矩阵。\n'
    return
  fi

  printf '| 范围 | 步骤 | 结果 | 说明 |\n'
  printf '| --- | --- | --- | --- |\n'
  local scope step status detail
  while IFS='|' read -r scope step status detail || [[ -n "${scope:-}" ]]; do
    [[ -z "${scope:-}" ]] && continue
    printf '| %s | %s | %s | %s |\n' \
      "$(minio_lab_safe_cell "$scope")" \
      "$(minio_lab_safe_cell "$step")" \
      "$(minio_lab_safe_cell "$status")" \
      "$(minio_lab_safe_cell "$detail")"
  done < "$step_file"
}

minio_lab_mc_hint() {
  local alias_name="${MINIO_LAB_MC_ALIAS:-<你的独立-lab-alias>}"
  if command -v mc >/dev/null 2>&1; then
    printf -- '- 当前系统检测到 `mc` 命令，可用它做恢复前后只读核验。\n'
  else
    printf -- '- 当前系统未检测到 `mc` 命令；如需命令行恢复核验，请先安装 MinIO Client。\n'
  fi
  printf -- '- 推荐先执行：`mc alias list`，确认只使用独立 lab alias。\n'
  printf -- '- 服务状态核验：`mc admin info %s`\n' "$alias_name"
  printf -- '- tier 核验：`mc admin tier ls %s`\n' "$alias_name"
  printf -- '- 配置核验：`mc admin config get %s <subsys>`\n' "$alias_name"
  printf -- '- 不要在仓库文件中保存 `mc alias set` 命令、access key、secret key、token 或签名。\n'
}

write_minio_lab_report() {
  local result="${1:-未知}"
  local exit_code="${2:-}"
  local detail="${3:-}"
  local repo_root="${REPO_ROOT:-$(pwd)}"
  local report_dir="${MINIO_LAB_REPORT_DIR:-$repo_root/target/minio-lab-reports}"
  local timestamp
  timestamp="${MINIO_LAB_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
  mkdir -p "$report_dir"
  local report_file="$report_dir/destructive-lab-$timestamp.md"
  local step_file
  step_file="$(minio_lab_step_file)"

  local config_enabled full_config_enabled quota_enabled remote_enabled replication_diff_enabled tier_enabled batch_enabled tier_write_enabled remote_write_enabled batch_write_enabled site_write_enabled
  config_enabled="false"
  full_config_enabled="false"
  quota_enabled="false"
  remote_enabled="false"
  replication_diff_enabled="false"
  tier_enabled="false"
  batch_enabled="false"
  tier_write_enabled="false"
  remote_write_enabled="false"
  batch_write_enabled="false"
  site_write_enabled="false"

  if [[ -n "${MINIO_LAB_TEST_CONFIG_KV:-}" && -n "${MINIO_LAB_RESTORE_CONFIG_KV:-}" ]]; then
    config_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_FULL_CONFIG_WRITE:-}" == "true" ]]; then
    full_config_enabled="true"
  fi
  if [[ -n "${MINIO_LAB_BUCKET:-}" && -n "${MINIO_LAB_TEST_BUCKET_QUOTA_JSON:-}" && -n "${MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON:-}" ]]; then
    quota_enabled="true"
  fi
  if [[ -n "${MINIO_LAB_BUCKET:-}" ]]; then
    remote_enabled="true"
  fi
  if [[ "${MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE:-}" == "true" ]]; then
    replication_diff_enabled="true"
  fi
  if [[ -n "${MINIO_LAB_TIER_NAME:-}" ]]; then
    tier_enabled="true"
  fi
  if [[ "${MINIO_LAB_ENABLE_BATCH_JOB_PROBES:-}" == "true" ]]; then
    batch_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" == "true" \
    && -n "${MINIO_LAB_TIER_WRITE_NAME:-}" \
    && ( -n "${MINIO_LAB_ADD_TIER_BODY:-}" || -n "${MINIO_LAB_ADD_TIER_BODY_FILE:-}" ) \
    && "${MINIO_LAB_REMOVE_TIER_AFTER_TEST:-}" == "true" ]]; then
    tier_write_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" == "true" \
    && -n "${MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET:-}" \
    && ( -n "${MINIO_LAB_SET_REMOTE_TARGET_BODY:-}" || -n "${MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE:-}" ) \
    && "${MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST:-}" == "true" ]]; then
    remote_write_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" == "true" \
    && ( -n "${MINIO_LAB_BATCH_START_BODY:-}" || -n "${MINIO_LAB_BATCH_START_BODY_FILE:-}" ) \
    && "${MINIO_LAB_CANCEL_BATCH_AFTER_TEST:-}" == "true" ]]; then
    batch_write_enabled="true"
  fi
  if [[ "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-}" == "true" \
    && ( -n "${MINIO_LAB_SITE_REPLICATION_ADD_BODY:-}" || -n "${MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE:-}" ) \
    && ( -n "${MINIO_LAB_SITE_REPLICATION_REMOVE_BODY:-}" || -n "${MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE:-}" ) \
    && "${MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST:-}" == "true" ]]; then
    site_write_enabled="true"
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
    printf -- '- 步骤状态文件：`%s`\n\n' "${step_file:-未启用}"

    printf '## 夹具开关\n\n'
    minio_lab_fixture_enabled 'config KV 写入 + 恢复' "$config_enabled"
    minio_lab_fixture_enabled 'full config 原样写回 + 恢复' "$full_config_enabled"
    minio_lab_fixture_enabled 'bucket quota 写入 + 恢复' "$quota_enabled"
    minio_lab_fixture_enabled 'tier typed/raw 探测' "$tier_enabled"
    minio_lab_fixture_enabled 'remote target typed/raw 探测' "$remote_enabled"
    minio_lab_fixture_enabled 'replication diff typed/raw 探测' "$replication_diff_enabled"
    minio_lab_fixture_enabled 'batch job typed/raw 探测' "$batch_enabled"
    minio_lab_fixture_enabled 'tier add/edit/remove 写入 + 恢复' "$tier_write_enabled"
    minio_lab_fixture_enabled 'remote target set/remove 写入 + 恢复' "$remote_write_enabled"
    minio_lab_fixture_enabled 'batch job start/status/cancel 实验矩阵' "$batch_write_enabled"
    minio_lab_fixture_enabled 'site replication add/edit/remove 实验矩阵' "$site_write_enabled"
    printf '\n'

    printf '## 夹具指纹（不含凭证）\n\n'
    printf -- '- 配置文件：`%s`\n' "${MINIO_LAB_CONFIG_FILE:-未使用}"
    printf -- '- bucket：`%s`\n' "${MINIO_LAB_BUCKET:-未设置}"
    printf -- '- tier 名称：`%s`\n' "${MINIO_LAB_TIER_NAME:-未设置}"
    printf -- '- tier 写入名称：`%s`\n' "${MINIO_LAB_TIER_WRITE_NAME:-未设置}"
    printf -- '- tier add 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_ADD_TIER_BODY:-}" "${MINIO_LAB_ADD_TIER_BODY_FILE:-}")"
    printf -- '- tier edit 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_EDIT_TIER_BODY:-}" "${MINIO_LAB_EDIT_TIER_BODY_FILE:-}")"
    printf -- '- remote target 类型：`%s`\n' "${MINIO_LAB_REMOTE_TARGET_TYPE:-replication}"
    printf -- '- remote target 写入 bucket：`%s`\n' "${MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET:-未设置}"
    printf -- '- remote target set 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_SET_REMOTE_TARGET_BODY:-}" "${MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE:-}")"
    printf -- '- remote target 预期 ARN：%s\n' "$(minio_lab_bool "${MINIO_LAB_REMOTE_TARGET_EXPECTED_ARN:-}")"
    printf -- '- remote target 删除 ARN：%s（可选兜底）\n' "$(minio_lab_bool "${MINIO_LAB_REMOVE_REMOTE_TARGET_ARN:-}")"
    printf -- '- replication diff 探测开关：`%s`\n' "${MINIO_LAB_ENABLE_REPLICATION_DIFF_PROBE:-false}"
    printf -- '- replication diff prefix：%s\n' "$(minio_lab_bool "${MINIO_LAB_REPLICATION_DIFF_PREFIX:-}")"
    printf -- '- replication diff ARN：%s\n' "$(minio_lab_bool "${MINIO_LAB_REPLICATION_DIFF_ARN:-}")"
    printf -- '- batch job start 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_BATCH_START_BODY:-}" "${MINIO_LAB_BATCH_START_BODY_FILE:-}")"
    printf -- '- batch job cancel 旧式请求体：%s（当前 SDK 不要求）\n' "$(minio_lab_bool_any "${MINIO_LAB_BATCH_CANCEL_BODY:-}" "${MINIO_LAB_BATCH_CANCEL_BODY_FILE:-}")"
    printf -- '- site replication add 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_SITE_REPLICATION_ADD_BODY:-}" "${MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE:-}")"
    printf -- '- site replication edit 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_SITE_REPLICATION_EDIT_BODY:-}" "${MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE:-}")"
    printf -- '- site replication remove 请求体：%s\n' "$(minio_lab_bool_any "${MINIO_LAB_SITE_REPLICATION_REMOVE_BODY:-}" "${MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE:-}")"
    printf -- '- full config 原样写回开关：`%s`\n' "${MINIO_LAB_ALLOW_FULL_CONFIG_WRITE:-false}"
    printf -- '- 写入夹具总开关：`%s`\n' "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-false}"
    printf -- '- batch job 预期 ID：%s\n\n' "$(minio_lab_bool "${MINIO_LAB_BATCH_EXPECTED_JOB_ID:-}")"

    printf '## typed/raw 执行明细\n\n'
    minio_lab_render_step_details "$step_file"
    printf '\n\n'

    printf '## mc 恢复/核验提示\n\n'
    minio_lab_mc_hint
    printf '\n'

    printf '## 失败恢复提示\n\n'
    printf '1. 如果 config KV 用例失败，使用 `MINIO_LAB_RESTORE_CONFIG_KV` 对应值恢复服务配置。\n'
    printf '2. 如果 full config 原样写回失败，测试会优先使用原始全量配置文本恢复；必要时请用独立 lab 控制台或 mc admin config 回滚。\n'
    printf '3. 如果 bucket quota 用例失败，使用 `MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON` 对应值恢复 bucket quota。\n'
    printf '4. 如果 tier 写入夹具失败，优先执行 `MINIO_LAB_REMOVE_TIER_AFTER_TEST=true` 对应的 tier 删除恢复。\n'
    printf '5. 如果 remote target 写入夹具失败，优先使用 set 响应返回的 ARN 删除刚写入的 target；响应不可解析时再使用 `MINIO_LAB_REMOVE_REMOTE_TARGET_ARN` 兜底。\n'
    printf '6. 如果 batch job 实验矩阵失败，优先使用 start 响应中的 jobId 执行 `cancelBatchJob(jobId)` 或 `ADMIN_CANCEL_BATCH_JOB?id=<jobId>`；旧式 cancel 请求体仅作人工排错参考。\n'
    printf '7. 如果 site replication 实验矩阵失败，优先使用 `MINIO_LAB_SITE_REPLICATION_REMOVE_BODY` 或对应文件移除刚新增的站点复制配置。\n'
    printf '8. 如果 remote target、tier、batch job 或 replication diff 探测失败，先查看 MinIO 管理日志，再确认 bucket 复制规则和远端 target 是否属于本次 lab。\n'
    printf '9. 不要把本报告复制到仓库；报告可能包含 lab 端点和资源名称，但不会包含凭证。\n'
  } > "$report_file"

  printf 'MinIO 破坏性实验环境报告已生成：%s\n' "$report_file"
}
