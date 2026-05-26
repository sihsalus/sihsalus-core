#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
if [[ "$MAVEN_CMD" == "./mvnw" && ! -x "./mvnw" ]]; then
  echo "Warning: ./mvnw not found, using mvn instead."
  MAVEN_CMD="mvn"
fi
MODULES=()
MAVEN_ARGS=("-DskipITs")

while [[ $# -gt 0 ]]; do
  case "$1" in
    --modules)
      IFS=',' read -r -a MODULES <<< "${2:-}"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: ./scripts/quality-fix.sh [--modules module1,module2]"
      exit 1
      ;;
  esac
done

if (( ${#MODULES[@]} > 0 )); then
  MAVEN_ARGS+=("-pl" "$(IFS=,; echo "${MODULES[*]}")" "-am")
fi

echo "=== Formatting: Spotless apply ==="
"$MAVEN_CMD" "${MAVEN_ARGS[@]}" spotless:apply

echo "=== Whitespace check after formatting ==="
if ! git diff --check; then
  echo "Whitespace issues remain. Commit only when you are ready."
fi
