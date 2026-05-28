# Runtime Troubleshooting

This runbook covers the backend when it is published behind a gateway under
`/openmrs`, for example:

```text
https://gidis-hsc-dev.inf.pucp.edu.pe/openmrs
```

The Spring Boot runtime is not a classic OpenMRS WAR. Some legacy URLs are
compatibility redirects and some old UI pages intentionally do not exist.

## Required Gateway Shape

Use one of these routing modes. Do not mix them.

Preferred mode for the SIH Salus gateway:

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

The preferred mode is easier to reason about because backend health, REST, FHIR,
and redirects all include `/openmrs`.

Set forwarded headers when TLS terminates at the gateway:

```text
SERVER_FORWARD_HEADERS_STRATEGY=framework
```

The gateway should pass at least:

```text
X-Forwarded-Proto
X-Forwarded-Host
X-Forwarded-Port
X-Forwarded-For
```

If the gateway rewrites path prefixes, also pass `X-Forwarded-Prefix` and verify
redirects manually.

## Runtime Timeouts

Backend defaults:

| Setting | Default | Purpose |
| --- | ---: | --- |
| `SERVER_TOMCAT_CONNECTION_TIMEOUT` | `20s` | Time allowed for the client or proxy to establish/send the request. This is not a long-running request limit. |
| `SERVER_SERVLET_SESSION_TIMEOUT` | `30m` | Browser session lifetime for OpenMRS-backed sessions. |
| `SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE` | `30s` | Graceful shutdown budget for each Spring shutdown phase. |
| `SIHSALUS_DATASOURCE_CONNECTION_TIMEOUT` | `30000` | Hikari connection acquisition timeout in milliseconds. |
| `SIHSALUS_DATASOURCE_VALIDATION_TIMEOUT` | `5000` | Hikari connection validation timeout in milliseconds. |

Container health check:

```text
GET /openmrs/actuator/health/readiness
interval=10s
timeout=5s
start_period=5m
retries=12
```

The 5 minute start period is deliberate. First startup with static content import,
OCL import, and search index creation can be much slower than a warm restart.

Gateway timeout recommendations:

| Gateway timeout | Recommended floor | Why |
| --- | ---: | --- |
| connect timeout | `10s` | Backend container should accept quickly after readiness is UP. |
| read timeout | `300s` | Reporting, imports, attachments, and first authenticated calls can be slow. |
| send timeout | `300s` | Large uploads and slow clients should not be cut off too early. |
| client upload/body timeout | `300s` | Patient documents and attachments can be larger than normal JSON calls. |

For Nginx-style proxies, use equivalent values:

```nginx
proxy_connect_timeout 10s;
proxy_read_timeout 300s;
proxy_send_timeout 300s;
send_timeout 300s;
```

If a specific workflow needs more than 5 minutes, prefer making that workflow
asynchronous before raising the global proxy timeout.

## Expected Compatibility Behavior

These are healthy responses:

| Request | Expected response |
| --- | --- |
| `GET /openmrs/actuator/health/readiness` | `200` when the app is ready. |
| `GET /openmrs/admin/index.htm` without credentials | `401` with `WWW-Authenticate`. |
| `GET /openmrs/admin/index.htm` with valid Basic credentials | `302` to `/openmrs/api/admin/static-modules`. |
| `GET /openmrs/ws/rest/v1/session` | `200` with `authenticated=false` when anonymous. |
| `GET /openmrs/ws/rest/v1/session` with valid Basic credentials | `200` with `authenticated=true` and `JSESSIONID`. |
| `GET /openmrs/ws/rest/v1/user/{uuid}` with a valid session or Basic credentials | `200` with `userProperties`, `roles`, and `person`. |

`/openmrs/admin/index.htm` is a legacy compatibility route. The supported
operator endpoint is `/openmrs/api/admin/static-modules`.

The user endpoint is also a compatibility route in the Spring Boot runtime. It
prevents the SPA user-properties lookup from falling through to the imported
legacy REST renderer, which can fail with old Apache Commons or servlet API
method signatures.

## Symptom Matrix

| Symptom | Likely cause | Check | Fix |
| --- | --- | --- | --- |
| Whitelabel `404` at `/openmrs/admin/index.htm` | Old image, wrong backend, or context path mismatch. | `curl -i /openmrs/admin/index.htm` and check image tag. | Deploy an image with the compatibility controller and set `SERVER_SERVLET_CONTEXT_PATH=/openmrs` if the gateway forwards `/openmrs`. |
| `401` at `/openmrs/admin/index.htm` | Normal unauthenticated response. | Retry with Basic auth. | Use a valid admin account or browser session. |
| `302` to `/api/admin/static-modules` missing `/openmrs` | Backend did not see the `/openmrs` context path or forwarded prefix. | Check `SERVER_SERVLET_CONTEXT_PATH` and gateway path rewriting. | Use the preferred gateway shape or pass forwarded prefix headers consistently. |
| `502 Bad Gateway` | Backend process is down or port mapping is wrong. | `docker compose -f deploy/compose.yml ps backend` and backend logs. | Start backend, fix port binding, or wait for startup. |
| `504 Gateway Timeout` | Proxy read timeout is too short for the workflow. | Compare gateway timeout with backend logs and request duration. | Use `proxy_read_timeout 300s` as the floor; make longer workflows async. |
| Health check remains `starting` | First startup is still importing content or building indexes. | `docker inspect` health log and backend logs. | Wait up to the 5 minute start period; if still failing, inspect OCL/Initializer errors. |
| Health check flips to `unhealthy` after startup | Database, migrations, or app runtime dependency became unavailable. | `/openmrs/actuator/health/readiness` and logs with `X-Request-Id`. | Fix DB connectivity or failed runtime dependency. |
| Redirects use `http://` behind HTTPS | Missing forwarded proto headers. | `curl -I` through the public gateway. | Forward `X-Forwarded-Proto=https` and keep `SERVER_FORWARD_HEADERS_STRATEGY=framework`. |
| Browser login session disappears | Cookie path/proxy context mismatch or multiple backend replicas without shared session. | Inspect `Set-Cookie` path and gateway routing. | Keep context path consistent; use sticky sessions or shared session strategy before scaling replicas. |
| SPA shows `Error fetching user properties` and `/openmrs/ws/rest/v1/user/{uuid}` returns `400` | Old backend image or request falling through to the legacy REST user renderer. | Check the deployed image tag and call the user endpoint with valid credentials. | Deploy an image with `UserCompatibilityController`; the endpoint must return `200` and include `userProperties`. |
| Startup fails with OCL/content errors | OCL import is enabled and fail-on-errors is true. | Search logs for `SihsalusOpenConceptLabStaticContentImporter`. | Fix content root or disable OCL only for infrastructure smoke tests. |

## Triage Commands

Set these once:

```bash
BASE_URL=https://gidis-hsc-dev.inf.pucp.edu.pe/openmrs
ADMIN_USER=admin
```

Readiness:

```bash
curl -i "$BASE_URL/actuator/health/readiness"
```

Legacy admin compatibility:

```bash
curl -i "$BASE_URL/admin/index.htm"
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/admin/index.htm"
```

REST session:

```bash
curl -i "$BASE_URL/ws/rest/v1/session"
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/ws/rest/v1/session"
```

REST user properties:

```bash
SESSION_BODY=$(curl -fsS -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/ws/rest/v1/session")
USER_UUID=$(printf '%s' "$SESSION_BODY" | sed -n 's/.*"user":{"uuid":"\([^"]*\)".*/\1/p')
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/ws/rest/v1/user/$USER_UUID"
```

Static module status:

```bash
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" \
  "$BASE_URL/api/admin/static-modules"
```

Compose-side checks:

```bash
docker compose -f deploy/compose.yml ps
docker compose -f deploy/compose.yml logs --tail=200 backend
docker inspect --format '{{json .State.Health}}' sihsalus-core-backend-1
```

If the container name differs, get it from `docker compose ps`.

## Release Smoke

Before promoting an image behind `/openmrs`, verify:

```bash
mvn -pl apps/backend -am \
  -Dtest=SihsalusCoreApplicationTest#adminAndLegacyModuleEndpointsRequireAuthentication+userCompatibilityEndpointReturnsUserPropertiesWithoutLegacyRoleConversion \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Then run the public checks:

```bash
curl -fsS "$BASE_URL/actuator/health/readiness"
curl -fsS "$BASE_URL/ws/rest/v1/session"
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/admin/index.htm"
SESSION_BODY=$(curl -fsS -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/ws/rest/v1/session")
USER_UUID=$(printf '%s' "$SESSION_BODY" | sed -n 's/.*"user":{"uuid":"\([^"]*\)".*/\1/p')
curl -i -u "$ADMIN_USER:$SIHSALUS_ADMIN_PASSWORD" "$BASE_URL/ws/rest/v1/user/$USER_UUID"
```

The authenticated admin compatibility check must return `302` with:

```text
Location: /openmrs/api/admin/static-modules
```
