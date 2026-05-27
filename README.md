# Sihsalus Core

Sihsalus Core is the backend foundation for the Sihsalus distribution. It starts from OpenMRS compatibility and evolves incrementally toward a maintained, secure, production-ready core.

The static module migration has been completed. The repository now focuses on stabilizing the migrated runtime: correctness, security, PostgreSQL/Liquibase behavior, CI gates, and production readiness.

## Current Phase

Stabilization after static module migration.

Goals:

- keep the Java 21 Maven reactor green and reproducible
- close CodeQL, formatting, and compiler-warning debt without broad rewrites
- verify migrated modules through PostgreSQL smoke tests and focused workflow tests
- harden security defaults, secrets handling, upload/OCL boundaries, and authorization paths
- validate centralized Liquibase behavior with dry-runs, upgrade fixtures, and rollback notes
- document compatibility exceptions before changing inherited OpenMRS contracts

## Compatibility Rule

Do not rename OpenMRS technical contracts casually. Package names, module identifiers, extension points, API paths, event names, database schema assumptions, and runtime configuration keys must remain compatible unless a migration plan exists.

Sihsalus branding and product experience can evolve independently from OpenMRS compatibility.

## Distro Baseline

The compatibility baseline remains the current Sihsalus distro, not OpenMRS Core `master`.

Current distro reference:

- repository: `sihsalus/sihsalus`
- distro parent: `3.7.0-SNAPSHOT`
- OpenMRS runtime: `2.8.6`
- module baseline: see `config/baseline/sihsalus-distro.properties`

## Java Baseline

Sihsalus Core targets Java 21 for new backend work and modernization work.

This matches the current OpenMRS core development branch, which uses `maven.compiler.release=21`. Stabilization changes must preserve the Sihsalus distro compatibility baseline unless an explicit compatibility review approves the change.

## Repository Layout

```text
config/     Version pins and local configuration baselines
deploy/     Docker image and local Compose runtime assets
docs/       Active architecture, security, CI, and modernization notes
docs/ops/   Deployment and operations notes
tests/e2e/  Cross-module end-to-end test workspace
.dev/       Ignored local reference clones and developer-only scratch data
core/api/                 Shared SIH Salus core contracts
core/liquibase/           Centralized database changelogs
core/openmrs-bom/         OpenMRS-compatible dependency baseline
apps/backend/             Spring Boot executable runtime
modules/fhir2/            First FHIR API surface
modules/webservices-rest/ REST compatibility surface
modules/*/                Static internal modules for distro capabilities
```

## Local Verification

Start the local PostgreSQL dependency:

```bash
export SIHSALUS_POSTGRES_PASSWORD='<set-a-long-local-db-secret>'
docker compose -f deploy/compose.yml up -d postgres
```

Set explicit database and admin passwords before starting the backend. A runtime with the default OpenMRS admin password is rejected at startup.

```bash
export SIHSALUS_POSTGRES_PASSWORD='<set-a-long-local-db-secret>'
export SIHSALUS_ADMIN_PASSWORD='<set-a-long-local-secret>'
docker compose -f deploy/compose.yml up -d backend
```

```bash
mvn --batch-mode --no-transfer-progress verify
```

Explicit quality checks (without wrappers):

```bash
mvn spotless:check
mvn -Dspotbugs.failOnError=false com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:check
mvn -DskipITs -DskipTests compile
mvn -DskipITs test
```

Module-scoped checks:

```bash
mvn -pl core/api -am spotless:check -Dspotbugs.failOnError=false com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:check -DskipITs -DskipTests compile
mvn -pl core/api -am -DskipITs test
```

Liquibase dry-run and migration review:

```bash
SIHSALUS_POSTGRES_PASSWORD='<set-a-long-local-db-secret>' ./scripts/liquibase-dry-run.sh --status
SIHSALUS_POSTGRES_PASSWORD='<set-a-long-local-db-secret>' ./scripts/liquibase-dry-run.sh --sql
```

The dry-run procedure is documented in `docs/database-migrations.md`.

This branch does not include `./mvnw`, so use `mvn` directly.

Modernization and technical-debt cleanup priorities are documented in
`docs/technical-debt-modernization.md`.

The stabilization plan is documented in `docs/stabilization-plan.md`.

The Spring Boot runtime map is documented in `docs/spring-boot-runtime.md`.

The current reactor layout is documented in `docs/module-map.md`.

The default boot configuration targets PostgreSQL:

```text
SIHSALUS_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sihsalus
SIHSALUS_DATASOURCE_USERNAME=sihsalus
SIHSALUS_DATASOURCE_PASSWORD=<set-a-long-local-db-secret>
```

Tests use H2 for fast runtime wiring coverage; PostgreSQL migration and SQL behavior
must still be verified with the Liquibase dry-run flow.

## Stabilization Milestone

The next milestone is a release-candidate backend where the migrated static runtime has predictable CI, PostgreSQL migration validation, smoke coverage for critical modules, hardened security defaults, and a documented residual-risk list.
