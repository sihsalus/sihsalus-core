#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
if [[ "$MAVEN_CMD" == "./mvnw" && ! -x "./mvnw" ]]; then
  echo "Warning: ./mvnw not found, using mvn instead."
  MAVEN_CMD="mvn"
fi

usage() {
  cat <<'EOF'
Usage: ./scripts/check.sh <command> [options]

Commands:
  fast [--modules m1,m2]      Whitespace check plus Maven verify with -Pci,quality.
  format [--modules m1,m2]    Apply Spotless formatting.
  db [args...]                Run the PostgreSQL migration gate.
  security [args...]          Generate SBOM and run dependency scan.
  image --image <name>        Scan a built container image.
  liquibase [args...]         Pass through to liquibase-dry-run.sh.
  help                        Show this help.

Module selectors may be reactor paths such as platform/api or artifact IDs prefixed
with :, for example :sihsalus-core-api.
EOF
}

resolve_module_selector() {
  local selector="$1"
  local artifact_selector="${selector#:}"
  local module_path
  local artifact_id

  if [[ -f "$selector/pom.xml" ]]; then
    echo "$selector"
    return 0
  fi

  while IFS= read -r module_path; do
    [[ -f "$module_path/pom.xml" ]] || continue
    artifact_id="$(
      awk '
        /<\/parent>/ { after_parent = 1; next }
        after_parent && match($0, /<artifactId>[^<]+<\/artifactId>/) {
          value = $0
          sub(/^.*<artifactId>/, "", value)
          sub(/<\/artifactId>.*$/, "", value)
          print value
          exit
        }
      ' "$module_path/pom.xml"
    )"
    if [[ "$artifact_id" == "$artifact_selector" ]]; then
      echo "$module_path"
      return 0
    fi
  done < <(sed -n 's:.*<module>\([^<][^<]*\)</module>.*:\1:p' pom.xml)

  echo "$selector"
}

parse_modules() {
  local value="$1"
  local raw_modules=()
  local resolved_modules=()
  local module

  IFS=',' read -r -a raw_modules <<< "$value"
  for module in "${raw_modules[@]}"; do
    [[ -n "$module" ]] || continue
    resolved_modules+=("$(resolve_module_selector "$module")")
  done

  if (( ${#resolved_modules[@]} == 0 )); then
    echo "Missing module list after --modules."
    exit 1
  fi

  local IFS=,
  echo "${resolved_modules[*]}"
}

run_whitespace_check() {
  if [[ -n "${SIHSALUS_DIFF_BASE:-}" ]]; then
    echo "=== Whitespace: ${SIHSALUS_DIFF_BASE}..HEAD ==="
    git diff --check "$SIHSALUS_DIFF_BASE" HEAD
  else
    echo "=== Whitespace: working tree ==="
    git diff --check
    git diff --cached --check
  fi
}

run_fast() {
  local module_list=""
  local maven_args=(--batch-mode --no-transfer-progress -Pci,quality)

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --modules)
        module_list="$(parse_modules "${2:-}")"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Unknown fast option: $1"
        usage
        exit 1
        ;;
    esac
  done

  if [[ -n "$module_list" ]]; then
    maven_args+=(-pl "$module_list" -am)
  fi

  run_whitespace_check
  echo "=== Maven verify: -Pci,quality ==="
  "$MAVEN_CMD" "${maven_args[@]}" verify
}

run_format() {
  local module_list=""
  local maven_args=(--batch-mode --no-transfer-progress -DskipITs)

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --modules)
        module_list="$(parse_modules "${2:-}")"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Unknown format option: $1"
        usage
        exit 1
        ;;
    esac
  done

  if [[ -n "$module_list" ]]; then
    maven_args+=(-pl "$module_list")
  fi

  echo "=== Spotless apply ==="
  "$MAVEN_CMD" "${maven_args[@]}" spotless:apply
  run_whitespace_check
}

command="${1:-help}"
if [[ $# -gt 0 ]]; then
  shift
fi

case "$command" in
  fast)
    run_fast "$@"
    ;;
  format)
    run_format "$@"
    ;;
  db)
    ./scripts/postgres-migration-gate.sh "$@"
    ;;
  security)
    ./scripts/supply-chain-gate.sh "$@"
    ;;
  image)
    ./scripts/supply-chain-gate.sh --skip-sbom --skip-dependency --require-container "$@"
    ;;
  liquibase)
    ./scripts/liquibase-dry-run.sh "$@"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "Unknown command: $command"
    usage
    exit 1
    ;;
esac
