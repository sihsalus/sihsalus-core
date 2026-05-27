# Database Migrations

Sihsalus Core uses a centralized Liquibase changelog for the static runtime:

- Runtime module: `apps/backend` (`sihsalus-core-boot`)
- Master changelog: `db/changelog/db.changelog-master.xml`
- Liquibase tables: `liquibasechangelog` and `liquibasechangeloglock`

Use `scripts/liquibase-dry-run.sh` before merging migration changes. The script packages the boot runtime, extracts the same classpath used by the application, and runs Liquibase CLI against the configured database.

## Standard Checks

Start the local PostgreSQL dependency first:

```bash
export SIHSALUS_POSTGRES_PASSWORD='<local-db-password>'
docker compose -f deploy/compose.yml up -d postgres
```

Check what is pending:

```bash
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --status
```

Generate SQL without applying it:

```bash
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --sql
```

The SQL is written to:

```text
target/liquibase-dry-run/update.sql
```

Liquibase logs for the SQL run are written next to the SQL file with a `.log` suffix.

Validate changelog parsing and database connectivity:

```bash
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --validate
```

## Target Database

The script defaults to the compose PostgreSQL service exposed locally:

```text
jdbc:postgresql://localhost:${SIHSALUS_POSTGRES_PORT:-5432}/${SIHSALUS_POSTGRES_DB:-sihsalus}
```

Override connection settings with the Liquibase-specific variables when reviewing a fixture or restored database:

```bash
export SIHSALUS_LIQUIBASE_URL='jdbc:postgresql://localhost:5432/sihsalus_upgrade_fixture'
export SIHSALUS_LIQUIBASE_USERNAME='sihsalus'
export SIHSALUS_LIQUIBASE_PASSWORD='<fixture-password>'
./scripts/liquibase-dry-run.sh --status
./scripts/liquibase-dry-run.sh --sql --output target/liquibase-dry-run/upgrade-fixture.sql
```

## Offline Mode

Offline mode is useful for syntax review of generated PostgreSQL SQL, but it does not inspect an existing `liquibasechangelog` table:

```bash
./scripts/liquibase-dry-run.sh --offline --sql --output target/liquibase-dry-run/offline-install.sql
```

Treat offline SQL as an install-shape artifact, not as proof that a hospital upgrade is safe.

## Review Rules

- Run `--status`, `--validate`, and `--sql` against a live PostgreSQL database before merging migration work.
- Review `target/liquibase-dry-run/update.sql` for broad deletes, table rewrites, expensive indexes, missing preconditions, and lock-heavy DDL.
- If a changeset uses Java `CustomTaskChange`, also test against a restored database fixture. `updateSQL` cannot fully prove runtime behavior for Java changesets.
- Keep the centralized changelog aligned with Spring Boot table names. Do not let local dry runs create default `databasechangelog` tables.
- Include rollback notes or an explicit “no rollback” explanation in the change review when a migration is irreversible.
