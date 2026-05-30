# Deploy

The deployment surface is intentionally small:

| File | Purpose |
| --- | --- |
| `deploy/docker/Dockerfile` | Single backend image build, BuildKit-ready, Maven cache mount enabled. |
| `deploy/docker/entrypoint.sh` | Runtime Java entrypoint. |
| `deploy/compose.yml` | Local PostgreSQL plus backend stack. |

The BuildKit cache behavior is folded into the canonical Dockerfile.

## Build

Local image build:

```bash
docker compose -f deploy/compose.yml build backend
```

Direct Docker build:

```bash
docker build -f deploy/docker/Dockerfile -t sihsalus-core:local .
```

The Dockerfile expects static content at `.dev/reference-sources/sihsalus-content` when building the runtime image. CI checks out `sihsalus/sihsalus-content` into that path before building.

## Compose Runtime

Required variables:

```text
SIHSALUS_POSTGRES_PASSWORD
SIHSALUS_ADMIN_PASSWORD
```

Start local backend:

```bash
export SIHSALUS_POSTGRES_PASSWORD='<local-db-secret>'
export SIHSALUS_ADMIN_PASSWORD='<local-admin-secret>'
docker compose -f deploy/compose.yml up -d backend
```

By default, PostgreSQL and backend bind to localhost. Override only when the runtime must be reachable outside the host:

```text
SIHSALUS_BACKEND_BIND_ADDRESS=0.0.0.0
SIHSALUS_BACKEND_PORT=8080
SIHSALUS_POSTGRES_BIND_ADDRESS=127.0.0.1
SIHSALUS_POSTGRES_PORT=5432
```

Common runtime overrides:

```text
SIHSALUS_BACKEND_IMAGE=ghcr.io/sihsalus/sihsalus-core:latest
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

## `/openmrs` Gateway

Preferred gateway mode:

```text
public path: /openmrs
backend SERVER_SERVLET_CONTEXT_PATH=/openmrs
gateway forwards path unchanged
```

Alternative mode only when the gateway strips `/openmrs` before forwarding:

```text
public path: /openmrs
backend SERVER_SERVLET_CONTEXT_PATH=
gateway strips /openmrs
```

For TLS termination at the gateway, keep:

```text
SERVER_FORWARD_HEADERS_STRATEGY=framework
```

The gateway should pass `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-Port`, and `X-Forwarded-For`. If it rewrites path prefixes, also pass `X-Forwarded-Prefix` and verify redirects manually.

Healthy `/openmrs` responses:

| Request | Expected |
| --- | --- |
| `GET /openmrs/actuator/health/readiness` | `200` when ready. |
| `GET /openmrs/admin/index.htm` without credentials | `401`. |
| `GET /openmrs/admin/index.htm` with valid Basic credentials | `302` to `/openmrs/api/admin/static-modules`. |
| `GET /openmrs/ws/rest/v1/session` | `200` with `authenticated=false` when anonymous. |
| `GET /openmrs/ws/rest/v1/session` with valid Basic credentials | `200` with `authenticated=true` and `JSESSIONID`. |

## Health And Smoke

Local health:

```bash
curl -fsS http://localhost:8080/actuator/health/readiness
```

Local auth smoke:

```bash
curl -i http://localhost:8080/rest/v1/patient?q=test
curl -i http://localhost:8080/api/fhir/metadata
curl -i -u admin:"$SIHSALUS_ADMIN_PASSWORD" http://localhost:8080/ws/rest/v1/session
```

Compose diagnostics:

```bash
docker compose -f deploy/compose.yml ps
docker compose -f deploy/compose.yml logs --tail=200 backend postgres
docker compose -f deploy/compose.yml exec -T backend curl -fsS http://localhost:8080/actuator/health
```

Public deployment smoke under `/openmrs`:

```bash
BASE_URL=https://example.org/openmrs
ADMIN_USER=admin
curl -fsS "$BASE_URL/actuator/health/readiness"
curl -i "$BASE_URL/ws/rest/v1/session"
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/admin/index.htm"
curl -fsS -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/ws/rest/v1/session"
```

## Operational Notes

- Use immutable `ghcr.io/<owner>/<repo>:sha-<short-commit>` tags for repeatable deployments. Use `latest` only for moving development environments.
- The first start with OCL/static content enabled can be slow because content import and search indexes run at startup.
- Disable OCL only for infrastructure smoke: `SIHSALUS_OCL_STATIC_IMPORT_ENABLED=false`.
- Production releases should run once with OCL enabled and valid deployment content.
- Gateway read/send/client body timeouts should be at least `300s`; backend readiness uses a `5m` container start period.
- Required production follow-ups remain backup/restore, monitoring/alerting, log retention/redaction, and incident response.
