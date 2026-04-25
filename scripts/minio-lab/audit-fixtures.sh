#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=load-config.sh
source "$SCRIPT_DIR/load-config.sh"

# 本脚本只做本地准备度审计：不连接 MinIO、不执行写入、不读取请求体内容。
# 所有凭证、token、签名和请求体只显示“是否已设置”，避免误写入终端记录或报告。
load_minio_lab_config

is_true() {
  [[ "${1:-}" == "true" ]]
}

is_set_var() {
  local name="$1"
  [[ -n "${!name:-}" ]]
}

presence() {
  if [[ -n "${1:-}" ]]; then
    printf '已设置'
  else
    printf '未设置'
  fi
}

body_presence() {
  local inline_var="$1"
  local file_var="$2"
  if is_set_var "$inline_var"; then
    printf '已设置内联请求体'
  elif is_set_var "$file_var"; then
    if [[ -f "${!file_var}" ]]; then
      printf '已设置请求体文件'
    else
      printf '请求体文件路径已设置但文件不存在'
    fi
  else
    printf '未设置'
  fi
}

body_ready() {
  local inline_var="$1"
  local file_var="$2"
  if is_set_var "$inline_var"; then
    return 0
  fi
  if is_set_var "$file_var" && [[ -f "${!file_var}" ]]; then
    return 0
  fi
  return 1
}

append_missing() {
  local __name="$1"
  local value="$2"
  if [[ -n "${!__name:-}" ]]; then
    printf -v "$__name" '%s；%s' "${!__name}" "$value"
  else
    printf -v "$__name" '%s' "$value"
  fi
}

print_gate_summary() {
  local gate_output
  gate_output="$(mktemp)"
  if "$SCRIPT_DIR/verify-env.sh" >"$gate_output" 2>&1; then
    printf -- '- 硬门禁：通过（只代表允许启动独立 lab 测试，不代表矩阵已执行）\n'
  else
    printf -- '- 硬门禁：未通过，原因如下（仍可继续查看夹具缺口）：\n'
    sed 's/^/  /' "$gate_output"
  fi
  rm -f "$gate_output"
}

print_row() {
  local name="$1"
  local status="$2"
  local need="$3"
  local restore="$4"
  local template="$5"
  printf '| %s | %s | %s | %s | %s |\n' "$name" "$status" "$need" "$restore" "$template"
}

write_enabled="false"
is_true "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-false}" && write_enabled="true"

printf 'MinIO 高风险 lab 夹具准备度审计\n'
printf '==================================\n'
printf '本脚本不会连接 MinIO，也不会执行任何写入；它只帮助你确认还缺哪些本机私有夹具。\n\n'
printf '基础配置\n'
printf -- '--------\n'
printf -- '- Lab 端点：%s\n' "$(presence "${MINIO_LAB_ENDPOINT:-}")"
printf -- '- Lab access key：%s\n' "$(presence "${MINIO_LAB_ACCESS_KEY:-}")"
printf -- '- Lab secret key：%s\n' "$(presence "${MINIO_LAB_SECRET_KEY:-}")"
printf -- '- 可回滚声明 MINIO_LAB_CAN_RESTORE：`%s`\n' "${MINIO_LAB_CAN_RESTORE:-未设置}"
printf -- '- 破坏性测试总开关 MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS：`%s`\n' "${MINIO_ALLOW_DESTRUCTIVE_ADMIN_TESTS:-未设置}"
printf -- '- 高风险写入夹具开关 MINIO_LAB_ALLOW_WRITE_FIXTURES：`%s`\n' "${MINIO_LAB_ALLOW_WRITE_FIXTURES:-false}"
print_gate_summary

printf '\n请求体设置情况（不输出内容）\n'
printf -- '--------------------------\n'
printf -- '- tier add：%s\n' "$(body_presence MINIO_LAB_ADD_TIER_BODY MINIO_LAB_ADD_TIER_BODY_FILE)"
printf -- '- tier edit：%s\n' "$(body_presence MINIO_LAB_EDIT_TIER_BODY MINIO_LAB_EDIT_TIER_BODY_FILE)"
printf -- '- remote target set：%s\n' "$(body_presence MINIO_LAB_SET_REMOTE_TARGET_BODY MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE)"
printf -- '- batch job start：%s\n' "$(body_presence MINIO_LAB_BATCH_START_BODY MINIO_LAB_BATCH_START_BODY_FILE)"
printf -- '- batch job cancel 旧式请求体：%s（当前 SDK 按 madmin 语义使用 start 响应 jobId 取消，不再要求该请求体）\n' "$(body_presence MINIO_LAB_BATCH_CANCEL_BODY MINIO_LAB_BATCH_CANCEL_BODY_FILE)"
printf -- '- site replication add：%s\n' "$(body_presence MINIO_LAB_SITE_REPLICATION_ADD_BODY MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE)"
printf -- '- site replication edit：%s\n' "$(body_presence MINIO_LAB_SITE_REPLICATION_EDIT_BODY MINIO_LAB_SITE_REPLICATION_EDIT_BODY_FILE)"
printf -- '- site replication remove：%s\n' "$(body_presence MINIO_LAB_SITE_REPLICATION_REMOVE_BODY MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE)"
printf -- '- remote target 删除 ARN：%s（set 响应可解析 ARN 时可不预填）\n' "$(presence "${MINIO_LAB_REMOVE_REMOTE_TARGET_ARN:-}")"

printf '\n矩阵准备度\n'
printf -- '----------\n'
printf '| 矩阵 | 当前状态 | 最小前置条件 | 恢复/回滚变量 | 模板 |\n'
printf '| --- | --- | --- | --- | --- |\n'

config_missing=""
[[ -n "${MINIO_LAB_TEST_CONFIG_KV:-}" ]] || append_missing config_missing '缺 MINIO_LAB_TEST_CONFIG_KV'
[[ -n "${MINIO_LAB_RESTORE_CONFIG_KV:-}" ]] || append_missing config_missing '缺 MINIO_LAB_RESTORE_CONFIG_KV'
if [[ -z "$config_missing" ]]; then
  print_row 'config KV typed/raw 写入恢复' '夹具已设置' '独立 lab + test/restore KV' 'MINIO_LAB_RESTORE_CONFIG_KV' '无需额外模板'
else
  print_row 'config KV typed/raw 写入恢复' "未就绪：$config_missing" '独立 lab + test/restore KV' 'MINIO_LAB_RESTORE_CONFIG_KV' '无需额外模板'
fi

quota_missing=""
[[ -n "${MINIO_LAB_BUCKET:-}" ]] || append_missing quota_missing '缺 MINIO_LAB_BUCKET'
[[ -n "${MINIO_LAB_TEST_BUCKET_QUOTA_JSON:-}" ]] || append_missing quota_missing '缺 MINIO_LAB_TEST_BUCKET_QUOTA_JSON'
[[ -n "${MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON:-}" ]] || append_missing quota_missing '缺 MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON'
if [[ -z "$quota_missing" ]]; then
  print_row 'bucket quota typed/raw 写入恢复' '夹具已设置' '独立 lab bucket + test/restore quota JSON' 'MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON' '无需额外模板'
else
  print_row 'bucket quota typed/raw 写入恢复' "未就绪：$quota_missing" '独立 lab bucket + test/restore quota JSON' 'MINIO_LAB_RESTORE_BUCKET_QUOTA_JSON' '无需额外模板'
fi

if [[ -n "${MINIO_LAB_TIER_NAME:-}" ]]; then
  print_row 'tier typed/raw 只读探测' '夹具已设置' 'MINIO_LAB_TIER_NAME；可选 MINIO_LAB_EXPECT_TIER_IN_LIST' '无写入恢复' '无需额外模板'
else
  print_row 'tier typed/raw 只读探测' '未就绪：缺 MINIO_LAB_TIER_NAME' 'MINIO_LAB_TIER_NAME；可选 MINIO_LAB_EXPECT_TIER_IN_LIST' '无写入恢复' '无需额外模板'
fi

if [[ -n "${MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET:-${MINIO_LAB_BUCKET:-}}" ]]; then
  print_row 'remote target typed/raw 只读探测' '夹具已设置或可使用 bucket' 'MINIO_LAB_BUCKET 或 MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET；可选预期 ARN' '无写入恢复' '无需额外模板'
else
  print_row 'remote target typed/raw 只读探测' '未就绪：缺 bucket' 'MINIO_LAB_BUCKET 或 MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET；可选预期 ARN' '无写入恢复' '无需额外模板'
fi

if is_true "${MINIO_LAB_ENABLE_BATCH_JOB_PROBES:-false}"; then
  print_row 'batch job typed/raw 只读探测' '夹具已设置' 'MINIO_LAB_ENABLE_BATCH_JOB_PROBES=true；可选预期 jobId' '无写入恢复' '无需额外模板'
else
  print_row 'batch job typed/raw 只读探测' '未启用：MINIO_LAB_ENABLE_BATCH_JOB_PROBES 不是 true' 'MINIO_LAB_ENABLE_BATCH_JOB_PROBES=true；可选预期 jobId' '无写入恢复' '无需额外模板'
fi

tier_write_missing=""
is_true "$write_enabled" || append_missing tier_write_missing '缺 MINIO_LAB_ALLOW_WRITE_FIXTURES=true'
[[ -n "${MINIO_LAB_TIER_WRITE_NAME:-}" ]] || append_missing tier_write_missing '缺 MINIO_LAB_TIER_WRITE_NAME'
body_ready MINIO_LAB_ADD_TIER_BODY MINIO_LAB_ADD_TIER_BODY_FILE || append_missing tier_write_missing '缺 tier add 请求体'
is_true "${MINIO_LAB_REMOVE_TIER_AFTER_TEST:-false}" || append_missing tier_write_missing '缺 MINIO_LAB_REMOVE_TIER_AFTER_TEST=true'
if [[ -z "$tier_write_missing" ]]; then
  print_row 'tier add/edit/remove 写入恢复' '可执行' '写入总开关 + 大写 tier 名称 + add 请求体；edit 请求体可选' 'MINIO_LAB_REMOVE_TIER_AFTER_TEST=true' 'templates/tier-add-minio.json.example；tier-edit-creds.json.example'
else
  print_row 'tier add/edit/remove 写入恢复' "未就绪：$tier_write_missing" '写入总开关 + 大写 tier 名称 + add 请求体；edit 请求体可选' 'MINIO_LAB_REMOVE_TIER_AFTER_TEST=true' 'templates/tier-add-minio.json.example；tier-edit-creds.json.example'
fi

remote_write_missing=""
is_true "$write_enabled" || append_missing remote_write_missing '缺 MINIO_LAB_ALLOW_WRITE_FIXTURES=true'
[[ -n "${MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET:-}" ]] || append_missing remote_write_missing '缺 MINIO_LAB_REMOTE_TARGET_WRITE_BUCKET'
body_ready MINIO_LAB_SET_REMOTE_TARGET_BODY MINIO_LAB_SET_REMOTE_TARGET_BODY_FILE || append_missing remote_write_missing '缺 remote target set 请求体'
is_true "${MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST:-false}" || append_missing remote_write_missing '缺 MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST=true'
if [[ -z "$remote_write_missing" ]]; then
  print_row 'remote target set/remove 写入恢复' '可执行' '写入总开关 + bucket + set 请求体；删除 ARN 可由 set 响应解析' '优先使用 set 响应 ARN；可选 MINIO_LAB_REMOVE_REMOTE_TARGET_ARN 兜底；MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST=true' 'templates/remote-target-set-replication.json.example'
else
  print_row 'remote target set/remove 写入恢复' "未就绪：$remote_write_missing" '写入总开关 + bucket + set 请求体；删除 ARN 可由 set 响应解析' '优先使用 set 响应 ARN；可选 MINIO_LAB_REMOVE_REMOTE_TARGET_ARN 兜底；MINIO_LAB_REMOVE_REMOTE_TARGET_AFTER_TEST=true' 'templates/remote-target-set-replication.json.example'
fi

batch_missing=""
is_true "$write_enabled" || append_missing batch_missing '缺 MINIO_LAB_ALLOW_WRITE_FIXTURES=true'
body_ready MINIO_LAB_BATCH_START_BODY MINIO_LAB_BATCH_START_BODY_FILE || append_missing batch_missing '缺 batch start 请求体'
is_true "${MINIO_LAB_CANCEL_BATCH_AFTER_TEST:-false}" || append_missing batch_missing '缺 MINIO_LAB_CANCEL_BATCH_AFTER_TEST=true'
if [[ -z "$batch_missing" ]]; then
  print_row 'batch job start/status/cancel' '可执行' '写入总开关 + start YAML；取消使用 start 响应 jobId' 'MINIO_LAB_CANCEL_BATCH_AFTER_TEST=true' 'templates/batch-start-job.yaml.example；batch-cancel-job.yaml.example（仅旧式说明）'
else
  print_row 'batch job start/status/cancel' "未就绪：$batch_missing" '写入总开关 + start YAML；取消使用 start 响应 jobId' 'MINIO_LAB_CANCEL_BATCH_AFTER_TEST=true' 'templates/batch-start-job.yaml.example；batch-cancel-job.yaml.example（仅旧式说明）'
fi

site_missing=""
is_true "$write_enabled" || append_missing site_missing '缺 MINIO_LAB_ALLOW_WRITE_FIXTURES=true'
body_ready MINIO_LAB_SITE_REPLICATION_ADD_BODY MINIO_LAB_SITE_REPLICATION_ADD_BODY_FILE || append_missing site_missing '缺 site replication add 请求体'
body_ready MINIO_LAB_SITE_REPLICATION_REMOVE_BODY MINIO_LAB_SITE_REPLICATION_REMOVE_BODY_FILE || append_missing site_missing '缺 site replication remove 请求体'
is_true "${MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST:-false}" || append_missing site_missing '缺 MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true'
if [[ -z "$site_missing" ]]; then
  print_row 'site replication add/edit/remove' '可执行' '写入总开关 + PeerSite[] add JSON + remove JSON；edit JSON 可选且通常需要 deploymentID' 'MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true；推荐 remove 使用 all=true 清理 lab 拓扑' 'templates/site-replication-*.json.example'
else
  print_row 'site replication add/edit/remove' "未就绪：$site_missing" '写入总开关 + PeerSite[] add JSON + remove JSON；edit JSON 可选且通常需要 deploymentID' 'MINIO_LAB_REMOVE_SITE_REPLICATION_AFTER_TEST=true；推荐 remove 使用 all=true 清理 lab 拓扑' 'templates/site-replication-*.json.example'
fi

printf '\n使用建议\n'
printf -- '--------\n'
printf '1. 先运行 start-docker-lab.sh 生成独立 MinIO，再把模板复制到仓库外的私有目录填写。\n'
printf '2. 模板中的 accessKey、secretKey、endpoint、ARN 都必须属于本次独立 lab；batch cancel 会使用 start 响应中的 jobId，不需要预填 jobID。\n'
printf '3. 准备好私有夹具后，再运行 audit-readiness.sh 通过硬门禁，最后运行 run-destructive-tests.sh 收集 typed/raw 证据。\n'
printf '4. 未执行真实矩阵前，不要把 destructive-blocked 从统计中移除。\n'
