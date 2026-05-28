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
SIHSALUS_AUTH_MODE=frontend
SIHSALUS_OCL_STATIC_IMPORT_ENABLED=true
SIHSALUS_OCL_STATIC_IMPORT_FAIL_ON_ERRORS=true
SIHSALUS_JAVA_OPTS=-XX:MaxRAMPercentage=75
SERVER_SERVLET_CONTEXT_PATH=
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_TOMCAT_CONNECTION_TIMEOUT=20s
SERVER_SERVLET_SESSION_TIMEOUT=30m
SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s
SIHSALUS_DATASOURCE_CONNECTION_TIMEOUT=30000
SIHSALUS_DATASOURCE_VALIDATION_TIMEOUT=5000
```

When this image is deployed behind the `sihsalus/sihsalus` gateway, set
`SERVER_SERVLET_CONTEXT_PATH=/openmrs`. The actuator health endpoint then moves
from `/actuator/health` to `/openmrs/actuator/health`, while REST and FHIR
routes keep their internal paths under the `/openmrs` gateway prefix.

## Authentication Mode

`SIHSALUS_AUTH_MODE` is the supported deployment switch:

- `frontend` is the default. The SPA authenticates against the OpenMRS REST session
  endpoint with username/password credentials, and the backend uses the OpenMRS
  username/password authentication scheme.
- `keycloak` selects the OAuth2 authentication scheme by setting the OpenMRS
  runtime property `authentication.scheme=oauth2`.

`OAUTH2_ENABLED=true` is still accepted as a legacy compatibility switch when
`SIHSALUS_AUTH_MODE` is not set. Prefer `SIHSALUS_AUTH_MODE` in new deployments.

Do not enable `keycloak` unless the gateway or frontend integration is also
providing the expected OAuth2 user-info credentials. The backend mode selection is
explicit; it does not silently fall back to frontend login if Keycloak is
misconfigured.

## Development Deployment

```bash
export SIHSALUS_BACKEND_IMAGE=ghcr.io/sihsalus/sihsalus-core:latest
export SIHSALUS_POSTGRES_PASSWORD='<db-secret>'
export SIHSALUS_ADMIN_PASSWORD='<admin-secret>'
export SIHSALUS_AUTH_MODE=frontend
docker compose -f deploy/compose.yml pull backend
docker compose -f deploy/compose.yml up -d --no-build backend
```

If the GHCR package is private, authenticate first with a token that has `read:packages`.

```bash
docker login ghcr.io
```

The first start with `SIHSALUS_OCL_STATIC_IMPORT_ENABLED=true` can be slow because it imports static concept packages and builds search indexes. OCL import errors fail startup by default. Disable OCL only for fast infrastructure smoke tests.

Use `runtime-troubleshooting.md` for `/openmrs` gateway shape, proxy timeouts,
health checks, and common 401/404/502/504 diagnosis.

After deploying a new backend image behind the gateway, run the public smoke
checks in `runtime-troubleshooting.md`. At minimum, validate health, REST
session creation, user properties, login locations, and a patient chart bootstrap
path before handing the environment back to frontend users.

Use `../runtime-hardening.md` before promoting a runtime build.

## Still Missing

- backup and restore runbook
- monitoring and alerting
- log retention and redaction policy
- incident response checklist
