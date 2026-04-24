#!/usr/bin/env bash
set -euo pipefail

# 读取 destructive lab 配置文件。
# 配置文件采用简单 KEY=VALUE 格式；空行和 # 注释会被忽略。
# 这里不使用 source，避免配置文件执行任意 shell 代码。

load_minio_lab_config() {
  local config_file="${MINIO_LAB_CONFIG_FILE:-}"
  if [[ -z "$config_file" ]]; then
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    config_file="$script_dir/lab.properties"
  fi

  [[ -f "$config_file" ]] || return 0

  local line key value
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "${line//[[:space:]]/}" ]] && continue
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" == *"="* ]] || continue

    key="${line%%=*}"
    value="${line#*=}"
    key="${key//[[:space:]]/}"
    [[ "$key" =~ ^[A-Z0-9_]+$ ]] || continue

    # 去掉常见引号，保留值中间的空格，例如：api requests_max=10。
    if [[ "$value" == \"*\" && "$value" == *\" ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
      value="${value:1:${#value}-2}"
    fi
    export "$key=$value"
  done < "$config_file"
}
