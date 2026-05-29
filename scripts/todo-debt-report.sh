#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

OUT_DIR="${TODO_DEBT_OUT_DIR:-$ROOT_DIR/target/todo-debt}"
REPORT="$OUT_DIR/todo-report.tsv"
SUMMARY="$OUT_DIR/summary.txt"

mkdir -p "$OUT_DIR"

rg -n --no-heading "TODO|FIXME|HACK" \
  --glob '!target/**' \
  --glob '!reference-sources/**' \
  --glob '!.git/**' \
  --glob '!scripts/todo-debt-report.sh' \
  --glob '!docs/todo-debt-register.md' \
  --glob '!*.class' \
  > "$OUT_DIR/raw.txt" || true

awk -F: '
  BEGIN {
    OFS = "\t"
    print "marker", "area", "path", "line", "text"
  }
  {
    marker = "TODO"
    if ($0 ~ /FIXME/) {
      marker = "FIXME"
    } else if ($0 ~ /HACK/) {
      marker = "HACK"
    }
    area = $1
    sub(/\/.*/, "", area)
    text = $0
    sub(/^[^:]+:[0-9]+:/, "", text)
    print marker, area, $1, $2, text
  }
' "$OUT_DIR/raw.txt" > "$REPORT"

{
  echo "TODO debt summary"
  echo
  awk -F '\t' 'NR > 1 { count[$1]++ } END { for (marker in count) print marker, count[marker] }' "$REPORT" | sort
  echo
  echo "By area"
  awk -F '\t' 'NR > 1 { count[$2]++ } END { for (area in count) print area, count[area] }' "$REPORT" | sort -k2,2nr -k1,1
  echo
  echo "Report: $REPORT"
} > "$SUMMARY"

cat "$SUMMARY"
