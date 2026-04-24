#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

"$SCRIPT_DIR/verify-env.sh"

: "${MINIO_LAB_ENDPOINT:?缺少 MINIO_LAB_ENDPOINT}"
: "${MINIO_LAB_ACCESS_KEY:?缺少 MINIO_LAB_ACCESS_KEY}"
: "${MINIO_LAB_SECRET_KEY:?缺少 MINIO_LAB_SECRET_KEY}"

export MINIO_ENDPOINT="$MINIO_LAB_ENDPOINT"
export MINIO_ACCESS_KEY="$MINIO_LAB_ACCESS_KEY"
export MINIO_SECRET_KEY="$MINIO_LAB_SECRET_KEY"
export MINIO_REGION="${MINIO_LAB_REGION:-us-east-1}"

cd "$REPO_ROOT"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}" \
PATH="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}/bin:$PATH" \
mvn -q -Dtest=DestructiveAdminIntegrationTest test
