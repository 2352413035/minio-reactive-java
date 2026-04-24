#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# shellcheck source=load-config.sh
source "$SCRIPT_DIR/load-config.sh"
# shellcheck source=write-report.sh
source "$SCRIPT_DIR/write-report.sh"
load_minio_lab_config

LAB_REPORT_DETAIL="测试尚未开始。"
finish_minio_lab_report() {
  local exit_code=$?
  local result="失败"
  if [[ "$exit_code" -eq 0 ]]; then
    result="通过"
  fi
  set +e
  write_minio_lab_report "$result" "$exit_code" "$LAB_REPORT_DETAIL"
  local report_code=$?
  if [[ "$report_code" -ne 0 ]]; then
    echo "MinIO 破坏性实验环境报告生成失败，但保留原始退出码：$exit_code" >&2
  fi
  exit "$exit_code"
}
trap finish_minio_lab_report EXIT

LAB_REPORT_DETAIL="verify-env 门禁拒绝或配置不完整。"
"$SCRIPT_DIR/verify-env.sh"

: "${MINIO_LAB_ENDPOINT:?缺少 MINIO_LAB_ENDPOINT}"
: "${MINIO_LAB_ACCESS_KEY:?缺少 MINIO_LAB_ACCESS_KEY}"
: "${MINIO_LAB_SECRET_KEY:?缺少 MINIO_LAB_SECRET_KEY}"

export MINIO_ENDPOINT="$MINIO_LAB_ENDPOINT"
export MINIO_ACCESS_KEY="$MINIO_LAB_ACCESS_KEY"
export MINIO_SECRET_KEY="$MINIO_LAB_SECRET_KEY"
export MINIO_REGION="${MINIO_LAB_REGION:-us-east-1}"

cd "$REPO_ROOT"
LAB_REPORT_DETAIL="Maven 破坏性实验环境测试执行失败，请按报告里的恢复提示检查 lab。"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
mvn -q -Dtest=DestructiveAdminIntegrationTest test
LAB_REPORT_DETAIL="所有破坏性实验环境测试通过。"
