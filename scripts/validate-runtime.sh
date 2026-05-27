#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
QUALITY_MODULES="${QUALITY_MODULES:-core/api,apps/backend}"
BOOT_JAR="$ROOT_DIR/apps/backend/target/sihsalus-core-boot-0.1.0-SNAPSHOT.jar"

export SIHSALUS_POSTGRES_PASSWORD="${SIHSALUS_POSTGRES_PASSWORD:-dummy}"
export SIHSALUS_ADMIN_PASSWORD="${SIHSALUS_ADMIN_PASSWORD:-dummy}"

echo "=== Module map ==="
./scripts/module-map-check.sh

echo "=== Quality doctor: $QUALITY_MODULES ==="
MAVEN_CMD="$MAVEN_CMD" ./scripts/quality-doctor.sh --modules "$QUALITY_MODULES" --skip-spotless

echo "=== Liquibase offline validation ==="
LIQUIBASE_ARGS=(--offline --validate)
if [[ -f "$BOOT_JAR" ]]; then
  LIQUIBASE_ARGS+=(--skip-build)
fi
MAVEN_CMD="$MAVEN_CMD" ./scripts/liquibase-dry-run.sh "${LIQUIBASE_ARGS[@]}"

echo "=== Docker Compose config ==="
docker compose -f deploy/compose.yml config >/dev/null

echo "=== Runtime validation completed ==="
