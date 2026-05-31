#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
CYCLONEDX_VERSION="${CYCLONEDX_MAVEN_PLUGIN_VERSION:-2.9.1}"
DEPENDENCY_CHECK_VERSION="${DEPENDENCY_CHECK_MAVEN_PLUGIN_VERSION:-12.1.0}"
TRIVY_IMAGE="${TRIVY_IMAGE:-aquasec/trivy:latest}"
TARGET_IMAGE=""
RUN_SBOM=1
RUN_DEPENDENCY_CHECK=1
RUN_CONTAINER=0
REQUIRE_CONTAINER=0

usage() {
  cat <<'EOF'
Usage: ./scripts/supply-chain-gate.sh [options]

Options:
  --image <name>          Container image to scan with Trivy
  --require-container    Fail when no --image is supplied
  --skip-sbom            Skip CycloneDX SBOM generation
  --skip-dependency      Skip OWASP Dependency-Check
  --skip-container       Skip Trivy container scan
  -h, --help             Show this help message

Defaults generate target/bom.xml and run dependency-check. Container scanning runs
when --image is supplied, using an installed trivy binary or the TRIVY_IMAGE Docker image.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image)
      TARGET_IMAGE="${2:-}"
      RUN_CONTAINER=1
      shift 2
      ;;
    --require-container)
      REQUIRE_CONTAINER=1
      shift
      ;;
    --skip-sbom)
      RUN_SBOM=0
      shift
      ;;
    --skip-dependency)
      RUN_DEPENDENCY_CHECK=0
      shift
      ;;
    --skip-container)
      RUN_CONTAINER=0
      shift
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

if [[ "$RUN_SBOM" == 1 ]]; then
  echo "=== Generating CycloneDX SBOM ==="
  "$MAVEN_CMD" -DskipTests -DskipITs \
    "org.cyclonedx:cyclonedx-maven-plugin:${CYCLONEDX_VERSION}:makeAggregateBom"
fi

if [[ "$RUN_DEPENDENCY_CHECK" == 1 ]]; then
  echo "=== Running OWASP Dependency-Check ==="
  dep_check_args=(
    -DskipTests -DskipITs
    -Dossindex.enabled=false
    "-DdataDirectory=${HOME}/.dependency-check/data"
  )
  if [[ -n "${NVD_API_KEY:-}" ]]; then
    dep_check_args+=("-DnvdApiKey=${NVD_API_KEY}")
  fi
  "$MAVEN_CMD" "${dep_check_args[@]}" \
    "org.owasp:dependency-check-maven:${DEPENDENCY_CHECK_VERSION}:check"
fi

if [[ -z "$TARGET_IMAGE" ]]; then
  if [[ "$REQUIRE_CONTAINER" == 1 && "$RUN_CONTAINER" == 1 ]]; then
    echo "Container scan was required but no --image was supplied."
    exit 1
  fi
  exit 0
fi

if [[ "$RUN_CONTAINER" == 1 ]]; then
  echo "=== Running Trivy container scan for $TARGET_IMAGE ==="
  if command -v trivy >/dev/null 2>&1; then
    trivy image --exit-code 1 --severity HIGH,CRITICAL "$TARGET_IMAGE"
  else
    docker run --rm \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -v "$ROOT_DIR/.trivycache:/root/.cache/" \
      "$TRIVY_IMAGE" image --exit-code 1 --severity HIGH,CRITICAL "$TARGET_IMAGE"
  fi
fi
