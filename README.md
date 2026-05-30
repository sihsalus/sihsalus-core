# Sihsalus Core

Sihsalus Core is the Spring Boot backend foundation for the Sihsalus OpenMRS-compatible distribution. The static module migration is complete; current work is stabilization: reproducible Maven builds, PostgreSQL/Liquibase confidence, runtime smoke checks, security gates, and compact operational documentation.

## Compatibility Rule

Preserve OpenMRS/Sihsalus technical contracts unless a reviewed migration says otherwise. That includes Java packages, module identifiers, REST/FHIR paths, Liquibase history, database schema assumptions, extension points, and runtime configuration keys.

Sihsalus product behavior can evolve, but compatibility breaks must be intentional and documented.

## Layout

```text
platform/openmrs-bom/    OpenMRS-compatible dependency baseline
platform/api/            OpenMRS-compatible domain, services, DAOs, utilities
platform/liquibase/      Centralized changelog ordering
modules/                 Static internal modules imported from the distro baseline
runtime/                 Spring Boot executable backend
deploy/                  Dockerfile, entrypoint, local Compose stack
scripts/                 One check entrypoint plus operational gates
docs/                    Current backend, deploy, and release docs
```

## Main Commands

```bash
./scripts/check.sh fast
./scripts/check.sh format --modules platform/api,modules/reporting
./scripts/check.sh db
./scripts/check.sh security
./scripts/check.sh image --image sihsalus-core:local
./scripts/check.sh liquibase --status
```

`fast` runs a whitespace check and `mvn --batch-mode --no-transfer-progress -Pci,quality verify`. It is the local equivalent of the Maven portion of CI.

The repository does not include `./mvnw`; use `mvn` directly or set `MAVEN_CMD` if a wrapper is restored later.

## Local Runtime

Start PostgreSQL and the backend with explicit secrets:

```bash
export SIHSALUS_POSTGRES_PASSWORD='<local-db-secret>'
export SIHSALUS_ADMIN_PASSWORD='<local-admin-secret>'
docker compose -f deploy/compose.yml up -d backend
```

Useful local checks:

```bash
docker compose -f deploy/compose.yml ps
curl -fsS http://localhost:8080/actuator/health/readiness
curl -i http://localhost:8080/rest/v1/session
curl -i -u admin:"$SIHSALUS_ADMIN_PASSWORD" http://localhost:8080/ws/rest/v1/session
```

For `/openmrs` gateway deployments, set `SERVER_SERVLET_CONTEXT_PATH=/openmrs` when the gateway forwards the prefix unchanged.

## Documentation

- `docs/backend.md`: architecture, module baseline, REST/FHIR surface, parity status.
- `docs/deploy.md`: Docker, Compose, `/openmrs` gateway shape, health and smoke checks.
- `docs/release.md`: CI gates, scripts, Maven profiles, PostgreSQL/Liquibase and security release criteria.
