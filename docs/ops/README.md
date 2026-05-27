# Operations

## Runtime Image

The backend image is published by the `Backend Runtime` workflow after the compose smoke test passes on `main`.

```text
ghcr.io/sihsalus/sihsalus-core:latest
ghcr.io/sihsalus/sihsalus-core:sha-<short-commit>
```

Use the immutable `sha-*` tag for repeatable deployments. Use `latest` only for a moving development environment.

## Required Environment

These values must be provided by the deployment environment and must not be committed to git:

```text
SIHSALUS_POSTGRES_PASSWORD
SIHSALUS_ADMIN_PASSWORD
```

Optional deployment overrides:

```text
SIHSALUS_BACKEND_IMAGE=ghcr.io/sihsalus/sihsalus-core:latest
SIHSALUS_BACKEND_BIND_ADDRESS=0.0.0.0
SIHSALUS_BACKEND_PORT=8080
SIHSALUS_POSTGRES_BIND_ADDRESS=127.0.0.1
SIHSALUS_POSTGRES_PORT=5432
SIHSALUS_OCL_STATIC_IMPORT_ENABLED=true
SIHSALUS_OCL_STATIC_IMPORT_FAIL_ON_ERRORS=true
SIHSALUS_JAVA_OPTS=-XX:MaxRAMPercentage=75
SERVER_SERVLET_CONTEXT_PATH=
```

When this image is deployed behind the `sihsalus/sihsalus` gateway, set
`SERVER_SERVLET_CONTEXT_PATH=/openmrs`. The actuator health endpoint then moves
from `/actuator/health` to `/openmrs/actuator/health`, while REST and FHIR
routes keep their internal paths under the `/openmrs` gateway prefix.

## Development Deployment

```bash
export SIHSALUS_BACKEND_IMAGE=ghcr.io/sihsalus/sihsalus-core:latest
export SIHSALUS_POSTGRES_PASSWORD='<db-secret>'
export SIHSALUS_ADMIN_PASSWORD='<admin-secret>'
docker compose pull backend
docker compose up -d --no-build backend
```

If the GHCR package is private, authenticate first with a token that has `read:packages`.

```bash
docker login ghcr.io
```

The first start with `SIHSALUS_OCL_STATIC_IMPORT_ENABLED=true` can be slow because it imports static concept packages and builds search indexes. OCL import errors fail startup by default. Disable OCL only for fast infrastructure smoke tests.

Use `../runtime-hardening.md` before promoting a runtime build.

## Still Missing

- backup and restore runbook
- monitoring and alerting
- TLS and reverse proxy notes
- log retention and redaction policy
- incident response checklist
