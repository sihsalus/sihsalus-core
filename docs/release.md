# Release

There are no end-to-end tests yet. Release confidence currently comes from Maven verification, PostgreSQL/Liquibase gates, dependency/SBOM/container scanning, and Compose runtime smoke.

## Script Entrypoint

Use `scripts/check.sh` instead of one-off wrappers:

| Command | Purpose |
| --- | --- |
| `./scripts/check.sh fast` | Whitespace plus `mvn --batch-mode --no-transfer-progress -Pci,quality verify`. |
| `./scripts/check.sh format --modules m1,m2` | Spotless apply, optionally module-scoped. |
| `./scripts/check.sh db` | Clean PostgreSQL migration gate, and upgrade fixture when configured. |
| `./scripts/check.sh security` | CycloneDX SBOM plus OWASP Dependency-Check. |
| `./scripts/check.sh image --image name` | Trivy container image scan. |
| `./scripts/check.sh liquibase --status` | Pass-through to Liquibase dry-run tooling. |

The retained operational scripts are:

- `scripts/liquibase-dry-run.sh`
- `scripts/postgres-migration-gate.sh`
- `scripts/supply-chain-gate.sh`
- `scripts/git-hooks/pre-push`

## Maven Profiles

Root `pom.xml` exposes these profiles:

| Profile | Intent |
| --- | --- |
| `ci` | CI-oriented verify defaults: Surefire/Failsafe plus JaCoCo prepare/report. |
| `quality` | Bind `spotless:check` to `validate`. |
| `analysis` | Bind SpotBugs `check` to `verify` for explicit static analysis runs. |
| `security` | Bind CycloneDX aggregate SBOM and OWASP Dependency-Check to `verify`. |

Normal fast check:

```bash
./scripts/check.sh fast
```

Explicit analysis:

```bash
mvn --batch-mode --no-transfer-progress -Panalysis verify
```

Explicit Maven security profile:

```bash
mvn --batch-mode --no-transfer-progress -Psecurity -DskipTests -DskipITs verify
```

`./scripts/check.sh security` remains the preferred operational command because it matches the CI gate and keeps room for container scanning.

## CI Workflow

The single active workflow is `.github/workflows/backend-runtime.yml` named `Backend`.

Jobs:

| Job | Gate |
| --- | --- |
| `quality` | Checkout, Java 21, whitespace base, Liquibase XML validation, `check.sh fast`, SBOM/dependency scan. |
| `postgres-migration-gate` | PostgreSQL 17 clean install migration gate; optional upgrade fixture through secrets. |
| `compose-smoke` | Build the Dockerfile, scan image, start backend stack, verify health and auth smoke. |
| `publish-ghcr` | Publish GHCR image from `main` after all gates pass. |

Fork pull requests skip jobs that require secrets or local service trust.

## PostgreSQL And Liquibase

Clean local database gate:

```bash
export SIHSALUS_POSTGRES_PASSWORD='<local-db-secret>'
docker compose -f deploy/compose.yml up -d postgres
./scripts/check.sh liquibase --validate
./scripts/check.sh liquibase --sql
./scripts/check.sh db
```

Review generated SQL and logs:

```text
target/liquibase-dry-run/update.sql
target/liquibase-dry-run/update.sql.log
target/postgres-migration-gate/*/update.sql
```

Upgrade fixture gate uses:

```text
SIHSALUS_UPGRADE_FIXTURE_URL
SIHSALUS_UPGRADE_FIXTURE_USERNAME
SIHSALUS_UPGRADE_FIXTURE_PASSWORD
```

Use `--require-upgrade-fixture` for release branches when absence of the fixture must fail.

## Security Gates

Required before release candidate:

- No secrets in git.
- Dependency vulnerability scan passes or has reviewed exceptions.
- SBOM is generated.
- Backend image scan passes for HIGH/CRITICAL findings or has reviewed exceptions.
- PostgreSQL runtime rejects blank datasource credentials.
- Bootstrap admin password is not the inherited default.
- OCL static import fails closed when enabled.
- Attachment upload extensions and sizes are explicitly reviewed.
- XStream request-controlled deserialization goes through deny-by-default serializers.
- REST/FHIR workflow paths have authentication and targeted object-level authorization coverage.

## Release Candidate Criteria

A backend release candidate needs:

- `Backend` workflow passing on a clean branch.
- `./scripts/check.sh fast` passing locally for touched code before push.
- PostgreSQL clean install migration gate passing.
- Upgrade fixture gate passing or explicitly deferred with owner and reason.
- Compose smoke passing with the single Dockerfile.
- OCL/static content smoke run with deployment content at least once before promotion.
- Workflow smoke evidence for changed or high-risk modules.
- Residual risks documented in this file or the release issue, not scattered in historical docs.

Do not claim OpenMRS 1:1 runtime parity until the parity gates in `docs/backend.md` pass.
