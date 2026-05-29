#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

REQUIRE_UPGRADE_FIXTURE=0
CLEAN_URL="${SIHSALUS_CLEAN_INSTALL_URL:-${SIHSALUS_LIQUIBASE_URL:-${SIHSALUS_DATASOURCE_URL:-}}}"
CLEAN_USERNAME="${SIHSALUS_CLEAN_INSTALL_USERNAME:-${SIHSALUS_LIQUIBASE_USERNAME:-${SIHSALUS_DATASOURCE_USERNAME:-${SIHSALUS_POSTGRES_USER:-sihsalus}}}}"
CLEAN_PASSWORD="${SIHSALUS_CLEAN_INSTALL_PASSWORD:-${SIHSALUS_LIQUIBASE_PASSWORD:-${SIHSALUS_DATASOURCE_PASSWORD:-${SIHSALUS_POSTGRES_PASSWORD:-}}}}"
UPGRADE_URL="${SIHSALUS_UPGRADE_FIXTURE_URL:-}"
UPGRADE_USERNAME="${SIHSALUS_UPGRADE_FIXTURE_USERNAME:-${SIHSALUS_LIQUIBASE_USERNAME:-${SIHSALUS_DATASOURCE_USERNAME:-${SIHSALUS_POSTGRES_USER:-sihsalus}}}}"
UPGRADE_PASSWORD="${SIHSALUS_UPGRADE_FIXTURE_PASSWORD:-${SIHSALUS_LIQUIBASE_PASSWORD:-${SIHSALUS_DATASOURCE_PASSWORD:-${SIHSALUS_POSTGRES_PASSWORD:-}}}}"
LIQUIBASE_SCRIPT="$ROOT_DIR/scripts/liquibase-dry-run.sh"

usage() {
  cat <<'EOF'
Usage: ./scripts/postgres-migration-gate.sh [options]

Options:
  --require-upgrade-fixture  Fail unless SIHSALUS_UPGRADE_FIXTURE_URL is configured
  --clean-url <jdbc-url>     JDBC URL for the clean PostgreSQL install gate
  --clean-username <user>    Username for the clean PostgreSQL install gate
  --clean-password <pass>    Password for the clean PostgreSQL install gate
  --upgrade-url <jdbc-url>   JDBC URL for a restored upgrade fixture database
  --upgrade-username <user>  Username for the upgrade fixture gate
  --upgrade-password <pass>  Password for the upgrade fixture gate
  -h, --help                 Show this help message

The gate runs Liquibase validate, updateSQL, update, and post-update status against
the clean database. When an upgrade fixture URL is present, it runs the same sequence
against that restored fixture. With --require-upgrade-fixture, absence of the fixture
is a hard failure.

GitHub Actions must provide the upgrade fixture through repository secrets:
  SIHSALUS_UPGRADE_FIXTURE_URL
  SIHSALUS_UPGRADE_FIXTURE_USERNAME
  SIHSALUS_UPGRADE_FIXTURE_PASSWORD
EOF
}

print_missing_upgrade_fixture_message() {
  cat <<'EOF'
Missing SIHSALUS_UPGRADE_FIXTURE_URL.

Restore the upgrade fixture database and point this gate at it. In GitHub,
configure these repository secrets under Settings > Secrets and variables >
Actions > Repository secrets:

  SIHSALUS_UPGRADE_FIXTURE_URL
  SIHSALUS_UPGRADE_FIXTURE_USERNAME
  SIHSALUS_UPGRADE_FIXTURE_PASSWORD
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --require-upgrade-fixture)
      REQUIRE_UPGRADE_FIXTURE=1
      shift
      ;;
    --clean-url)
      CLEAN_URL="${2:-}"
      shift 2
      ;;
    --clean-username)
      CLEAN_USERNAME="${2:-}"
      shift 2
      ;;
    --clean-password)
      CLEAN_PASSWORD="${2:-}"
      shift 2
      ;;
    --upgrade-url)
      UPGRADE_URL="${2:-}"
      shift 2
      ;;
    --upgrade-username)
      UPGRADE_USERNAME="${2:-}"
      shift 2
      ;;
    --upgrade-password)
      UPGRADE_PASSWORD="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$CLEAN_URL" ]]; then
  CLEAN_URL="jdbc:postgresql://localhost:${SIHSALUS_POSTGRES_PORT:-5432}/${SIHSALUS_POSTGRES_DB:-sihsalus}"
fi

if [[ -z "$CLEAN_PASSWORD" ]]; then
  echo "Missing clean install database password."
  exit 1
fi

run_target() {
  local name="$1"
  local url="$2"
  local username="$3"
  local password="$4"
  local output_dir="$ROOT_DIR/target/postgres-migration-gate/$name"

  mkdir -p "$output_dir"
  echo "=== PostgreSQL migration gate: $name validate ==="
  "$LIQUIBASE_SCRIPT" --validate --url "$url" --username "$username" --password "$password"
  echo "=== PostgreSQL migration gate: $name dry-run SQL ==="
  "$LIQUIBASE_SCRIPT" \
    --sql \
    --url "$url" \
    --username "$username" \
    --password "$password" \
    --output "$output_dir/update.sql" \
    --skip-build
  echo "=== PostgreSQL migration gate: $name update ==="
  "$LIQUIBASE_SCRIPT" --update --url "$url" --username "$username" --password "$password" --skip-build
  echo "=== PostgreSQL migration gate: $name post-update status ==="
  "$LIQUIBASE_SCRIPT" --status --url "$url" --username "$username" --password "$password" --skip-build
}

run_target "clean-install" "$CLEAN_URL" "$CLEAN_USERNAME" "$CLEAN_PASSWORD"

if [[ -n "$UPGRADE_URL" ]]; then
  if [[ -z "$UPGRADE_PASSWORD" ]]; then
    echo "Missing upgrade fixture database password."
    exit 1
  fi
  run_target "upgrade-fixture" "$UPGRADE_URL" "$UPGRADE_USERNAME" "$UPGRADE_PASSWORD"
elif [[ "$REQUIRE_UPGRADE_FIXTURE" == 1 ]]; then
  print_missing_upgrade_fixture_message
  exit 1
else
  echo "Skipping upgrade fixture gate because SIHSALUS_UPGRADE_FIXTURE_URL is not set."
fi
