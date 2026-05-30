# Backend

Sihsalus Core is a static Spring Boot modular monolith around an OpenMRS-compatible domain model. It is not a classic OpenMRS WAR and it does not provide the `.omod` install/start/stop lifecycle.

## Architecture

Runtime ownership:

- `runtime` (`sihsalus-core-boot`) is the only executable composition root.
- Spring Boot owns startup, configuration binding, datasource creation, Liquibase startup, Hibernate construction, health endpoints, filters, and container runtime.
- OpenMRS-compatible entities, services, database assumptions, package names, and API contracts remain the compatibility base.
- Internal modules are normal Maven/Spring code loaded statically, not runtime-installed OMODs.
- Liquibase is centralized through `platform/liquibase` and uses OpenMRS-compatible tables: `liquibasechangelog` and `liquibasechangeloglock`.

Layer map:

| Area | Responsibility |
| --- | --- |
| `platform/openmrs-bom` | Dependency baseline for OpenMRS-compatible imports. |
| `platform/api` | Core domain, DAOs, services, utilities, OpenMRS compatibility contracts. |
| `platform/liquibase` | Central changelog order for core and migrated modules. |
| `modules/webservices-rest` | OpenMRS REST v1 compatibility surface. |
| `modules/fhir2` | FHIR R4 compatibility surface. |
| `modules/*` | Static Sihsalus/OpenMRS-compatible application modules. |
| `runtime` | Boot startup, static module wiring, filters, health, admin bootstrap, OpenMRS runtime bridge. |

## Compatibility Baseline

The compatibility target is the Sihsalus distro baseline, not upstream OpenMRS Core `master`.

| Key | Value |
| --- | --- |
| Distro repository | `sihsalus/sihsalus` |
| Distro branch | `main` |
| Distro commit | `c6d4e6a5` |
| Distro parent | `3.7.0-SNAPSHOT` |
| OpenMRS runtime | `2.8.6` |
| Sihsalus content | `1.8.30` |

Module baseline:

| Module | Baseline version |
| --- | --- |
| `initializer` | `2.11.0` |
| `fhir2` | `4.0.0-SNAPSHOT` |
| `webservices-rest` | `3.4.1` |
| `authentication` | `2.3.0` |
| `oauth2login` | `1.5.0` |
| `idgen` | `5.0.4` |
| `addresshierarchy` | `2.21.0` |
| `patientdocuments` | `1.1.0-SNAPSHOT` |
| `attachments` | `4.0.0` |
| `cohort` | `3.7.3` |
| `patientflags` | `3.0.10` |
| `o3forms` | `2.3.0` |
| `emrapi` | `3.4.0` |
| `queue` | `3.0.0` |
| `appointments` | `2.1.0-20250318.070530-1` |
| `teleconsultation` | `2.1.0-20250318.154145-1` |
| `bedmanagement` | `7.2.0` |
| `reporting` | `2.1.0` |
| `reportingrest` | `2.0.0` |
| `calculation` | `2.0.0` |
| `htmlwidgets` | `2.0.1` |
| `serialization-xstream` | `0.3.0` |
| `metadatamapping` | `2.0.0` |
| `openconceptlab` | `3.0.0` |
| `ordertemplates` | `2.2.0` |
| `event` | `4.0.0` |
| `stockmanagement` | `3.0.0` |
| `billing` | `2.2.0` |
| `fua` | `1.0.75` |
| `imaging` | `1.2.2` |
| `legacyui` | `2.1.0` |

Runtime additions that were not in the original distro baseline:

| Module | Runtime version |
| --- | --- |
| `datafilter` | `0.1.0-SNAPSHOT` |
| `sihsalusinterop` | `1.0.3` |

Version differences in `StaticModuleCatalog` must be treated as compatibility review items: `addresshierarchy`, `authentication`, `billing`, `calculation`, `emrapi`, `idgen`, `metadatamapping`, and `oauth2login` currently use newer local source versions than the distro baseline.

## Runtime Configuration

Required for containerized runtime:

```text
SIHSALUS_POSTGRES_PASSWORD
SIHSALUS_ADMIN_PASSWORD
```

Important deployment settings:

```text
SIHSALUS_DATASOURCE_URL
SIHSALUS_DATASOURCE_USERNAME
SIHSALUS_DATASOURCE_PASSWORD
SIHSALUS_ADMIN_USERNAME
SIHSALUS_ADMIN_PASSWORD
SIHSALUS_AUTH_MODE
OAUTH2_ENABLED
OPENMRS_APPLICATION_DATA_DIRECTORY
SIHSALUS_INITIALIZER_SOURCE_ROOT
SIHSALUS_INITIALIZER_STARTUP_LOAD
SIHSALUS_INITIALIZER_DOMAINS
SIHSALUS_INITIALIZER_EXCLUDE_ADDRESSHIERARCHY
SIHSALUS_OCL_STATIC_IMPORT_ENABLED
SIHSALUS_OCL_STATIC_IMPORT_FAIL_ON_ERRORS
SERVER_PORT
SERVER_SERVLET_CONTEXT_PATH
SERVER_FORWARD_HEADERS_STRATEGY
SERVER_TOMCAT_CONNECTION_TIMEOUT
SERVER_SERVLET_SESSION_TIMEOUT
SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE
SIHSALUS_DATASOURCE_CONNECTION_TIMEOUT
SIHSALUS_DATASOURCE_VALIDATION_TIMEOUT
JAVA_OPTS
```

Supported datasource families are `jdbc:postgresql:` for runtime and `jdbc:h2:` for tests. Other JDBC families should fail startup instead of guessing a driver or dialect.

Authentication modes:

| Mode | Behavior |
| --- | --- |
| `SIHSALUS_AUTH_MODE=frontend` | Default. Uses OpenMRS username/password session login. |
| `SIHSALUS_AUTH_MODE=keycloak` | Selects the OAuth2 authentication scheme. |
| `OAUTH2_ENABLED=true` | Legacy compatibility input only when `SIHSALUS_AUTH_MODE` is unset. |

## HTTP Surface

Paths are relative to the application root. In `/openmrs` deployments, prepend `/openmrs`.

| Surface | Paths |
| --- | --- |
| Health | `/actuator/health`, `/actuator/health/readiness` |
| Runtime metadata | `/api/system/info`, `/api/admin/static-modules` |
| Legacy admin compatibility | `/admin/index.htm` redirects to `/api/admin/static-modules` after auth |
| OpenMRS REST | `/rest/v1`, `/ws/rest/v1` |
| Session | `/rest/v1/session`, `/ws/rest/v1/session` |
| FHIR R4 | `/api/fhir`, `/api/fhir/r4`, `/ws/fhir2/R4` |
| HTML widgets | `/module/htmlwidgets/*` |
| SIH Salus interop legacy routes | `/module/sihsalusinterop/*` |

Most REST, FHIR, admin, and legacy module paths require an authenticated OpenMRS user. Basic auth is accepted by the REST and FHIR filters. `Disable-WWW-Authenticate: true` suppresses browser-native Basic auth prompts on `401`.

Selected active module REST roots:

| Module | Path roots |
| --- | --- |
| Appointments | `/rest/v1/appointment`, `/rest/v1/appointments`, `/rest/v1/recurring-appointments`, `/rest/v1/appointmentService`, `/rest/v1/appointment-services`, `/rest/v1/speciality` |
| Attachments | `/rest/v1/attachment`, `/rest/v1/attachment/{uuid}/bytes` |
| Bed management | `/rest/v1/admissionLocation`, `/rest/v1/bed`, `/rest/v1/beds`, `/rest/v1/bedPatientAssignment`, `/rest/v1/bedTag`, `/rest/v1/bedtype` |
| Billing | `/rest/v1/billing/*`, `/rest/v2/billing/timesheet` |
| Cohort | `/rest/v1/cohortm/*` |
| ID generation | `/rest/v1/idgen/identifiersource/{sourceIdOrUuid}/identifier` |
| O3 Forms | `/rest/v1/o3/forms/{formNameOrUuid}` |
| Open Concept Lab | `/rest/v1/openconceptlab/*` |
| Order templates | `/rest/v1/ordertemplates/orderTemplate` |
| Patient documents | `/rest/v1/patientdocuments/*` |
| Patient flags | `/rest/v1/patientflags/*` |
| Queue | `/rest/v1/queue*`, `/rest/v1/queue-entry*`, `/rest/v1/queue-room*`, `/rest/v1/queueutil/*` |
| Reporting REST | `/rest/v1/reportingrest/*` |
| SIH Salus Interop | `/rest/v1/interop/*`, `/module/sihsalusinterop/*` |
| Teleconsultation | `/rest/v1/teleconsultation/generateLink` |

All `/rest/...` module endpoints are also expected through the matching `/ws/rest/...` compatibility prefix. Legacy controllers present only as source or under legacy UI folders are not automatically active HTTP endpoints.

## Parity Status

Current status: static module migration is broadly covered, but OpenMRS 1:1 runtime parity is not proven.

Covered or partially covered:

- Boot startup with static module wiring.
- Login/session bootstrap for selected paths.
- Selected OpenMRS REST resources and compatibility adapters.
- FHIR R4 metadata, selected reads, and lightweight searches.
- Static module inventory through `/api/admin/static-modules`.
- Central Liquibase ordering and dry-run tooling.
- H2-based boot smoke tests for wiring and selected contracts.

Not equivalent to classic OpenMRS:

- No dynamic `.omod` lifecycle or classic module admin.
- No full classic OpenMRS WAR UI.
- FHIR is a Spring Boot compatibility surface, not full servlet parity for every FHIR2 operation.
- EMR API is service-only in this runtime unless a route is explicitly declared.
- PostgreSQL behavior is not proven by H2 tests.
- Object-level authorization coverage is representative, not complete across all workflows.

Do not claim 1:1 parity until these gates pass and are documented: clean PostgreSQL install, upgrade fixture, endpoint diff against the original distro runtime, workflow smokes for stock/billing/O3/FUA/FHIR/REST/reporting/OCL/attachments/appointments/queue/bed management, static content/OCL import with deployment content, object-level authorization checks, and public `/openmrs` gateway smoke.
