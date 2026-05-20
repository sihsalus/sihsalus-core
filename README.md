# Sihsalus Core

Sihsalus Core is the backend foundation for the Sihsalus distribution. It starts from OpenMRS compatibility and evolves incrementally toward a maintained, secure, production-ready core.

This repository is intentionally small at the beginning. Code is brought in only when its ownership, compatibility impact, test strategy, and security posture are clear.

## Current Phase

Phase 1: static modular monolith skeleton.

Goals:

- keep a Java 21 Maven reactor as the build boundary
- compose SIH Salus modules as internal Maven jars instead of runtime `.omod` packages
- provide a Spring Boot executable runtime
- run centralized Liquibase migrations
- expose technical health and first FHIR metadata endpoints

## Compatibility Rule

Do not rename OpenMRS technical contracts casually. Package names, module identifiers, extension points, API paths, event names, database schema assumptions, and runtime configuration keys must remain compatible unless a migration plan exists.

Sihsalus branding and product experience can evolve independently from OpenMRS compatibility.

## Distro Baseline

The initial compatibility baseline is the current Sihsalus distro, not OpenMRS Core `master`.

Current distro reference:

- repository: `sihsalus/sihsalus`
- distro parent: `3.7.0-SNAPSHOT`
- OpenMRS runtime: `2.8.6`
- module baseline: see `baseline/sihsalus-distro.properties`

## Java Baseline

Sihsalus Core targets Java 21 for new backend work and modernization work.

This matches the current OpenMRS core development branch, which uses `maven.compiler.release=21`. The current Sihsalus distro is still based on OpenMRS `2.8.6`, so imported code must be validated against the distro baseline before being modernized.

## Repository Layout

```text
baseline/   Version pins from the current Sihsalus distro
docs/       Architecture, migration, and security decisions
ops/        Deployment and operations notes
scripts/    Local automation scripts
sihsalus-core-api/        Shared SIH Salus core contracts
sihsalus-core-liquibase/  Centralized database changelogs
sihsalus-core-boot/       Spring Boot executable runtime
sihsalus-fhir2/           First FHIR API surface
sihsalus-webservices-rest/ REST compatibility surface
sihsalus-module-*/        Static internal module placeholders for distro capabilities
```

## Local Verification

Start the local PostgreSQL dependency:

```bash
docker compose up -d postgres
```

Set an explicit admin password before starting the backend. A runtime with the default OpenMRS admin password is rejected at startup.

```bash
export SIHSALUS_ADMIN_PASSWORD='<set-a-long-local-secret>'
docker compose up -d backend
```

```bash
mvn --batch-mode --no-transfer-progress verify
```

The default boot configuration targets PostgreSQL:

```text
SIHSALUS_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sihsalus
SIHSALUS_DATASOURCE_USERNAME=sihsalus
SIHSALUS_DATASOURCE_PASSWORD=sihsalus
```

Tests use H2 in PostgreSQL compatibility mode.

## First Milestone

The first milestone is a runnable minimal core profile with documented compatibility boundaries, CI, static module composition, centralized migrations, and a first FHIR endpoint.
