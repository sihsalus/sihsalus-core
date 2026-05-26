#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
if [[ "$MAVEN_CMD" == "./mvnw" && ! -x "./mvnw" ]]; then
  echo "Warning: ./mvnw not found, using mvn instead."
  MAVEN_CMD="mvn"
fi
SKIP_TESTS=1
MODULES=()

usage() {
  cat <<'EOF'
Usage: ./scripts/quality-doctor.sh [options]

Options:
  --modules module1,module2  Run only specific modules (comma-separated)
  --tests                    Run tests after compile (default: skip tests)
  --profiles <list>          Maven profiles to activate (comma-separated)
  -h, --help                Show this help message

Examples:
  ./scripts/quality-doctor.sh
  ./scripts/quality-doctor.sh --modules sihsalus-core-api,sihsalus-module-reporting --tests
  MAVEN_CMD=./mvnw ./scripts/quality-doctor.sh --modules sihsalus-core-api
EOF
}

MAVEN_ARGS=("-DskipITs")
while [[ $# -gt 0 ]]; do
  case "$1" in
    --modules)
      IFS=',' read -r -a MODULES <<< "${2:-}"
      shift 2
      ;;
    --tests)
      SKIP_TESTS=0
      shift
      ;;
    --profiles)
      MAVEN_ARGS+=("-P${2:-}")
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

if (( ${#MODULES[@]} > 0 )); then
  MAVEN_ARGS+=("-pl" "$(IFS=,; echo "${MODULES[*]}")" "-am")
fi

echo "=== Step 1/4: Spotless check ==="
"$MAVEN_CMD" "${MAVEN_ARGS[@]}" spotless:check

echo "=== Step 2/4: SpotBugs check ==="
"$MAVEN_CMD" "${MAVEN_ARGS[@]}" spotbugs:check

echo "=== Step 3/4: Compile ==="
"$MAVEN_CMD" "${MAVEN_ARGS[@]}" -DskipTests compile

if [[ "$SKIP_TESTS" == 0 ]]; then
  echo "=== Step 4/4: Tests ==="
  "$MAVEN_CMD" "${MAVEN_ARGS[@]}" test
else
  echo "=== Step 4/4: Skipped tests ==="
fi

echo "=== Quality doctor completed ==="
