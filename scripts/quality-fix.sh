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
  RESOLVED_MODULES=()
  for module in "${MODULES[@]}"; do
    RESOLVED_MODULES+=("$(resolve_module_selector "$module")")
  done
  MAVEN_ARGS+=("-pl" "$(IFS=,; echo "${RESOLVED_MODULES[*]}")")
fi

echo "=== Formatting: Spotless apply ==="
"$MAVEN_CMD" "${MAVEN_ARGS[@]}" spotless:apply

echo "=== Whitespace check after formatting ==="
if ! git diff --check; then
  echo "Whitespace issues remain. Commit only when you are ready."
fi
