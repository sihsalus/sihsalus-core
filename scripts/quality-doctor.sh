#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
if [[ "$MAVEN_CMD" == "./mvnw" && ! -x "./mvnw" ]]; then
  echo "Warning: ./mvnw not found, using mvn instead."
  MAVEN_CMD="mvn"
fi
SPOTBUGS_GOAL="com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:check"
SKIP_TESTS=1
RUN_SPOTBUGS=0
STRICT_SPOTBUGS=0
RUN_SPOTLESS=1
MODULES=()

usage() {
  cat <<'EOF'
Usage: ./scripts/quality-doctor.sh [options]

Options:
  --modules module1,module2  Run only specific modules (comma-separated)
  --tests                    Run tests after compile (default: skip tests)
  --skip-spotless            Skip Maven Spotless check
  --spotbugs                 Run SpotBugs using the fully-qualified Maven plugin
  --strict-spotbugs          Fail when SpotBugs finds existing issues
  --profiles <list>          Maven profiles to activate (comma-separated)
  -h, --help                Show this help message

Examples:
  ./scripts/quality-doctor.sh
  ./scripts/quality-doctor.sh --modules sihsalus-core-api,sihsalus-module-reporting --tests
  ./scripts/quality-doctor.sh --modules sihsalus-module-patientflags --spotbugs
  MAVEN_CMD=./mvnw ./scripts/quality-doctor.sh --modules sihsalus-core-api
EOF
}

BASE_MAVEN_ARGS=("-DskipITs")
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
    --skip-spotless)
      RUN_SPOTLESS=0
      shift
      ;;
    --spotbugs)
      RUN_SPOTBUGS=1
      shift
      ;;
    --strict-spotbugs)
      RUN_SPOTBUGS=1
      STRICT_SPOTBUGS=1
      shift
      ;;
    --profiles)
      BASE_MAVEN_ARGS+=("-P${2:-}")
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

SPOTLESS_MAVEN_ARGS=("${BASE_MAVEN_ARGS[@]}")
BUILD_MAVEN_ARGS=("${BASE_MAVEN_ARGS[@]}")
if (( ${#MODULES[@]} > 0 )); then
  module_list="$(IFS=,; echo "${MODULES[*]}")"
  SPOTLESS_MAVEN_ARGS+=("-pl" "$module_list")
  BUILD_MAVEN_ARGS+=("-pl" "$module_list" "-am")
fi

echo "=== Step 1: Git whitespace check ==="
git diff --check

if [[ "$RUN_SPOTLESS" == 1 ]]; then
  echo "=== Step 2: Spotless check ==="
  "$MAVEN_CMD" "${SPOTLESS_MAVEN_ARGS[@]}" spotless:check
else
  echo "=== Step 2: Skipped Spotless check ==="
fi

echo "=== Step 3: Compile ==="
"$MAVEN_CMD" "${BUILD_MAVEN_ARGS[@]}" -DskipTests compile

if [[ "$SKIP_TESTS" == 0 ]]; then
  echo "=== Step 4: Tests ==="
  "$MAVEN_CMD" "${BUILD_MAVEN_ARGS[@]}" test
else
  echo "=== Step 4: Skipped tests ==="
fi

if [[ "$RUN_SPOTBUGS" == 1 ]]; then
  echo "=== Step 5: SpotBugs check ==="
  if [[ "$STRICT_SPOTBUGS" == 1 ]]; then
    "$MAVEN_CMD" "${BUILD_MAVEN_ARGS[@]}" "$SPOTBUGS_GOAL"
  else
    "$MAVEN_CMD" "${BUILD_MAVEN_ARGS[@]}" -Dspotbugs.failOnError=false "$SPOTBUGS_GOAL"
  fi
fi

echo "=== Quality doctor completed ==="
