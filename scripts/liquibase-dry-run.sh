#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
if [[ "$MAVEN_CMD" == "./mvnw" && ! -x "./mvnw" ]]; then
  echo "Warning: ./mvnw not found, using mvn instead."
  MAVEN_CMD="mvn"
fi

MODE="sql"
SKIP_BUILD=0
CHANGELOG="${SIHSALUS_LIQUIBASE_CHANGELOG:-db/changelog/db.changelog-master.xml}"
OUTPUT_FILE="${SIHSALUS_LIQUIBASE_OUTPUT:-$ROOT_DIR/target/liquibase-dry-run/update.sql}"
URL="${SIHSALUS_LIQUIBASE_URL:-${SIHSALUS_DATASOURCE_URL:-}}"
USERNAME="${SIHSALUS_LIQUIBASE_USERNAME:-${SIHSALUS_DATASOURCE_USERNAME:-${SIHSALUS_POSTGRES_USER:-sihsalus}}}"
PASSWORD="${SIHSALUS_LIQUIBASE_PASSWORD:-${SIHSALUS_DATASOURCE_PASSWORD:-${SIHSALUS_POSTGRES_PASSWORD:-}}}"
CONTEXTS="${SIHSALUS_LIQUIBASE_CONTEXTS:-}"
LABELS="${SIHSALUS_LIQUIBASE_LABELS:-}"
DRIVER="${SIHSALUS_LIQUIBASE_DRIVER:-}"
EXTRA_ARGS=()

usage() {
  cat <<'EOF'
Usage: ./scripts/liquibase-dry-run.sh [options]

Modes:
  --sql              Generate pending SQL without applying it (default)
  --status           Show pending changesets
  --validate         Validate the changelog against the target database
  --update           Apply pending changesets to the target database

Options:
  --offline          Use Liquibase offline PostgreSQL mode instead of a live database
  --url <jdbc-url>   Override SIHSALUS_LIQUIBASE_URL / SIHSALUS_DATASOURCE_URL
  --username <user>  Override SIHSALUS_LIQUIBASE_USERNAME / SIHSALUS_DATASOURCE_USERNAME
  --password <pass>  Override SIHSALUS_LIQUIBASE_PASSWORD / SIHSALUS_DATASOURCE_PASSWORD
  --changelog <file> Override the changelog path
  --contexts <list>  Pass Liquibase contexts
  --labels <list>    Pass Liquibase labels
  --output <file>    SQL output file for --sql
  --skip-build       Reuse the existing runtime package
  -h, --help         Show this help message

Defaults:
  changelog: db/changelog/db.changelog-master.xml
  url:       jdbc:postgresql://localhost:${SIHSALUS_POSTGRES_PORT:-5432}/${SIHSALUS_POSTGRES_DB:-sihsalus}
  username:  ${SIHSALUS_POSTGRES_USER:-sihsalus}
  tables:    liquibasechangelog / liquibasechangeloglock

Examples:
  SIHSALUS_POSTGRES_PASSWORD=local ./scripts/liquibase-dry-run.sh --status
  SIHSALUS_POSTGRES_PASSWORD=local ./scripts/liquibase-dry-run.sh --sql --output target/liquibase-dry-run/local.sql
  ./scripts/liquibase-dry-run.sh --offline --sql --output target/liquibase-dry-run/offline-install.sql
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sql)
      MODE="sql"
      shift
      ;;
    --status)
      MODE="status"
      shift
      ;;
    --validate)
      MODE="validate"
      shift
      ;;
    --update)
      MODE="update"
      shift
      ;;
    --offline)
      URL="offline:postgresql"
      USERNAME=""
      PASSWORD=""
      shift
      ;;
    --url)
      URL="${2:-}"
      shift 2
      ;;
    --username)
      USERNAME="${2:-}"
      shift 2
      ;;
    --password)
      PASSWORD="${2:-}"
      shift 2
      ;;
    --changelog)
      CHANGELOG="${2:-}"
      shift 2
      ;;
    --contexts)
      CONTEXTS="${2:-}"
      shift 2
      ;;
    --labels)
      LABELS="${2:-}"
      shift 2
      ;;
    --output)
      OUTPUT_FILE="${2:-}"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      EXTRA_ARGS+=("$@")
      break
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$URL" ]]; then
  URL="jdbc:postgresql://localhost:${SIHSALUS_POSTGRES_PORT:-5432}/${SIHSALUS_POSTGRES_DB:-sihsalus}"
fi

if [[ -z "$DRIVER" ]]; then
  if [[ "$URL" == jdbc:postgresql:* ]]; then
    DRIVER="org.postgresql.Driver"
  elif [[ "$URL" == jdbc:mysql:* ]]; then
    DRIVER="com.mysql.cj.jdbc.Driver"
  fi
fi

if [[ "$URL" != offline:* && -z "$PASSWORD" ]]; then
  echo "Missing database password. Set SIHSALUS_LIQUIBASE_PASSWORD, SIHSALUS_DATASOURCE_PASSWORD, or SIHSALUS_POSTGRES_PASSWORD."
  exit 1
fi

BOOT_JAR="$ROOT_DIR/runtime/target/sihsalus-core-boot-0.1.0-SNAPSHOT.jar"
OPENMRS_DATA_DIR="${SIHSALUS_LIQUIBASE_OPENMRS_DATA_DIR:-$ROOT_DIR/target/liquibase-dry-run/openmrs-data}"
EXTRACT_DIR="$ROOT_DIR/target/liquibase-dry-run/boot"
RUN_DIR="$(mktemp -d "${TMPDIR:-/tmp}/sihsalus-liquibase-dry-run.XXXXXX")"
trap 'rm -rf "$RUN_DIR"' EXIT

mkdir -p "$OPENMRS_DATA_DIR/configuration"
LOG4J_CONFIG="$RUN_DIR/log4j2-liquibase.xml"
cat > "$LOG4J_CONFIG" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Appenders>
    <Console name="stderr" target="SYSTEM_ERR">
      <PatternLayout pattern="%d %-5p [%t] %c - %m%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="ERROR">
      <AppenderRef ref="stderr"/>
    </Root>
  </Loggers>
</Configuration>
EOF

if [[ "$SKIP_BUILD" == 0 ]]; then
  echo "=== Packaging runtime for Liquibase classpath ==="
  "$MAVEN_CMD" -q -pl runtime -am -DskipTests -DskipITs package
elif [[ ! -f "$BOOT_JAR" ]]; then
  echo "Missing $BOOT_JAR. Run without --skip-build first."
  exit 1
fi

echo "=== Extracting boot runtime classpath ==="
java \
  -DOPENMRS_APPLICATION_DATA_DIRECTORY="$OPENMRS_DATA_DIR" \
  -Djarmode=tools \
  -jar "$BOOT_JAR" \
  extract \
  --destination "$EXTRACT_DIR" \
  --force >/dev/null

APP_JAR="$(find "$EXTRACT_DIR" -maxdepth 1 -name '*.jar' | sort | sed -n '1p')"
if [[ -z "$APP_JAR" ]]; then
  echo "Unable to locate extracted application jar in $EXTRACT_DIR."
  exit 1
fi

CLASSPATH="$APP_JAR:$EXTRACT_DIR/lib/*"
LIQUIBASE_COMMAND="updateSQL"
if [[ "$MODE" == "status" ]]; then
  LIQUIBASE_COMMAND="status"
elif [[ "$MODE" == "validate" ]]; then
  LIQUIBASE_COMMAND="validate"
elif [[ "$MODE" == "update" ]]; then
  LIQUIBASE_COMMAND="update"
fi

LIQUIBASE_ARGS=(
  "--changeLogFile=$CHANGELOG"
  "--url=$URL"
  "--databaseChangeLogTableName=liquibasechangelog"
  "--databaseChangeLogLockTableName=liquibasechangeloglock"
)

if [[ "$URL" != offline:* ]]; then
  LIQUIBASE_ARGS+=("--username=$USERNAME" "--password=$PASSWORD")
  if [[ -n "$DRIVER" ]]; then
    LIQUIBASE_ARGS+=("--driver=$DRIVER")
  fi
fi
if [[ -n "$CONTEXTS" ]]; then
  LIQUIBASE_ARGS+=("--contexts=$CONTEXTS")
fi
if [[ -n "$LABELS" ]]; then
  LIQUIBASE_ARGS+=("--labels=$LABELS")
fi
if (( ${#EXTRA_ARGS[@]} > 0 )); then
  LIQUIBASE_ARGS+=("${EXTRA_ARGS[@]}")
fi

if [[ "$MODE" == "sql" ]]; then
  mkdir -p "$(dirname "$OUTPUT_FILE")"
  LOG_OUTPUT="$OUTPUT_FILE.log"
  echo "=== Generating Liquibase updateSQL ==="
  (
    cd "$RUN_DIR"
    java \
      -DOPENMRS_APPLICATION_DATA_DIRECTORY="$OPENMRS_DATA_DIR" \
      -Dlog4j2.configurationFile="$LOG4J_CONFIG" \
      -cp "$CLASSPATH" \
      liquibase.integration.commandline.Main \
      "${LIQUIBASE_ARGS[@]}" \
      "$LIQUIBASE_COMMAND"
  ) > "$OUTPUT_FILE" 2> "$LOG_OUTPUT"

  echo "SQL written to: $OUTPUT_FILE"
  echo "Liquibase log written to: $LOG_OUTPUT"
else
  echo "=== Running Liquibase $LIQUIBASE_COMMAND ==="
  (
    cd "$RUN_DIR"
    java \
      -DOPENMRS_APPLICATION_DATA_DIRECTORY="$OPENMRS_DATA_DIR" \
      -Dlog4j2.configurationFile="$LOG4J_CONFIG" \
      -cp "$CLASSPATH" \
      liquibase.integration.commandline.Main \
      "${LIQUIBASE_ARGS[@]}" \
      "$LIQUIBASE_COMMAND"
  )
fi
