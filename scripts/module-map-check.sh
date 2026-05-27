#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

POM_FILE="pom.xml"
MODULE_MAP="docs/module-map.md"

if [[ ! -f "$POM_FILE" ]]; then
  echo "Missing $POM_FILE"
  exit 1
fi

if [[ ! -f "$MODULE_MAP" ]]; then
  echo "Missing $MODULE_MAP"
  exit 1
fi

missing=()
reactor_count=0
while IFS= read -r module; do
  reactor_count=$((reactor_count + 1))
  if ! grep -Fq "\`$module\`" "$MODULE_MAP"; then
    missing+=("$module")
  fi
done < <(sed -n 's:.*<module>\([^<][^<]*\)</module>.*:\1:p' "$POM_FILE" | sort)

if (( ${#missing[@]} > 0 )); then
  echo "Modules listed in $POM_FILE but missing from $MODULE_MAP:"
  printf '  - %s\n' "${missing[@]}"
  exit 1
fi

echo "Module map covers $reactor_count Maven modules."
