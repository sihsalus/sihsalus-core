# Database Migrations

Sihsalus Core uses a centralized Liquibase changelog for the static runtime:

- Runtime module: `runtime` (`sihsalus-core-boot`)
- Master changelog: `db/changelog/db.changelog-master.xml`
- Liquibase tables: `liquibasechangelog` and `liquibasechangeloglock`

Use `scripts/liquibase-dry-run.sh` before merging migration changes. The script packages the boot runtime, extracts the same classpath used by the application, and runs Liquibase CLI against the configured database.

CI runs `scripts/postgres-migration-gate.sh` as the merge gate. It validates, generates `updateSQL`, applies `update`, and checks post-update status against a clean PostgreSQL database. When the upgrade fixture secrets are configured, it also runs the restored fixture path.

## GitHub Actions Secrets

Configure the upgrade fixture connection in GitHub, not in this repository:

1. Open the repository in GitHub.
2. Go to **Settings** > **Secrets and variables** > **Actions**.
3. Under **Repository secrets**, add:

```text
SIHSALUS_UPGRADE_FIXTURE_URL
SIHSALUS_UPGRADE_FIXTURE_USERNAME
SIHSALUS_UPGRADE_FIXTURE_PASSWORD
```

`SIHSALUS_UPGRADE_FIXTURE_URL` must be a JDBC URL, for example:

```text
jdbc:postgresql://host:5432/sihsalus_upgrade_fixture
```

Do not commit fixture credentials or hospital database URLs to the repo. The
workflow reads them through `${{ secrets.* }}` in
`.github/workflows/backend-runtime.yml`.

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

Apply pending changesets to the target database:

```bash
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --update
```

Run the full local PostgreSQL gate:

```bash
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/postgres-migration-gate.sh
```

For release-candidate readiness, the upgrade fixture is mandatory:

```bash
export SIHSALUS_UPGRADE_FIXTURE_URL='jdbc:postgresql://localhost:5432/sihsalus_upgrade_fixture'
export SIHSALUS_UPGRADE_FIXTURE_USERNAME='sihsalus'
export SIHSALUS_UPGRADE_FIXTURE_PASSWORD='<fixture-password>'
./scripts/postgres-migration-gate.sh --require-upgrade-fixture
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

The Liquibase helper infers the JDBC driver from the target URL, so PostgreSQL
targets use `org.postgresql.Driver` automatically unless you override it with
`SIHSALUS_LIQUIBASE_DRIVER`.

Migration configuration is centralized in the runtime and shared scripts. Any
module-level `liquibase.properties` files that still exist are legacy artifacts,
not part of the supported migration path.

## Offline Mode

Offline mode is useful for syntax review of generated PostgreSQL SQL, but it does not inspect an existing `liquibasechangelog` table:

```bash
./scripts/liquibase-dry-run.sh --offline --sql --output target/liquibase-dry-run/offline-install.sql
```

Treat offline SQL as an install-shape artifact, not as proof that a hospital upgrade is safe.

## Review Rules

- Run the PostgreSQL migration gate against a live database before merging migration work.
- Review `target/liquibase-dry-run/update.sql` for broad deletes, table rewrites, expensive indexes, missing preconditions, and lock-heavy DDL.
- If a changeset uses Java `CustomTaskChange`, the restored upgrade fixture gate is required. `updateSQL` cannot fully prove runtime behavior for Java changesets.
- Keep the centralized changelog aligned with Spring Boot table names. Do not let local dry runs create default `databasechangelog` tables.
- Include rollback notes or an explicit “no rollback” explanation in the change review when a migration is irreversible.
