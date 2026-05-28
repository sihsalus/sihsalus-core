# OpenMRS Parity Audit

Audit date: 2026-05-28

## Verdict

SIH Salus Core is not 1:1 with a classic OpenMRS webapp/WAR runtime.

Functional verdict: SIH Salus Core is not functionally 1:1 yet. It has
functional coverage for important backend paths such as login/session, selected
OpenMRS REST resources, selected FHIR R4 reads/searches, static module wiring,
and several module endpoint registrations. It does not yet have enough
end-to-end workflow evidence to claim that clinical, operational, reporting,
billing, stock, attachment, content import, legacy UI, and authorization
behavior matches the original OpenMRS distro one for one.

It does have broad static coverage of the SIH Salus OpenMRS compatibility
baseline: every module declared in the baseline is represented in
`StaticModuleCatalog`, the backend depends on those static modules, and the
central Liquibase changelog includes the migrated module changelogs that need
database schema.

The difference is runtime behavior. SIH Salus Core is a Spring Boot modular
monolith with static module wiring, compatibility adapters, and selected
legacy routes. It intentionally does not provide the OpenMRS `.omod`
install/start/stop lifecycle or the full classic OpenMRS administration and
legacy UI surface.

## Baseline

The compatibility target is the current SIH Salus distro, not upstream OpenMRS
Core `master`.

- SIH Salus distro repository: `sihsalus/sihsalus`
- SIH Salus distro commit: `c6d4e6a5`
- Distro parent: `3.7.0-SNAPSHOT`
- OpenMRS runtime baseline: `2.8.6`
- Expected machine-readable baseline:
  `config/baseline/sihsalus-distro.properties`

Worktree note from this audit: `config/baseline/sihsalus-distro.properties`
is currently deleted in the local worktree. The comparison below used the
baseline copy from `HEAD` plus the archive reference in
`docs/archive/sihsalus-distro-baseline.md`. Restore the baseline file before
claiming the repository documentation is internally consistent.

## Module Coverage

Comparison source:

- baseline modules from `HEAD:config/baseline/sihsalus-distro.properties`
- static runtime modules from
  `core/api/src/main/java/org/sihsalus/core/api/StaticModuleCatalog.java`

Result:

- baseline modules: 31
- static catalog modules: 33
- baseline modules missing from the static catalog: none
- extra static modules: `datafilter`, `sihsalusinterop`

Version differences that must be treated as compatibility review items:

| Module | Baseline version | Static catalog version |
| --- | --- | --- |
| `addresshierarchy` | `2.21.0` | `3.0.0-SNAPSHOT` |
| `authentication` | `2.3.0` | `2.4.0-SNAPSHOT` |
| `billing` | `2.2.0` | `2.3.0-SNAPSHOT` |
| `calculation` | `2.0.0` | `2.1.0-SNAPSHOT` |
| `emrapi` | `3.4.0` | `3.5.0-SNAPSHOT` |
| `idgen` | `5.0.4` | `6.0.0-SNAPSHOT` |
| `metadatamapping` | `2.0.0` | `2.1.0-SNAPSHOT` |
| `oauth2login` | `1.5.0` | `1.6.0-SNAPSHOT` |

## Functional Parity Matrix

Use this matrix when discussing functional parity. `Covered` means there is
working runtime behavior plus focused test evidence. `Partial` means the
runtime has code, routes, or service wiring, but not full workflow parity
evidence. `Not equivalent` means the Spring Boot runtime intentionally behaves
differently from classic OpenMRS.

| Functional area | Current status | Evidence | Functional parity |
| --- | --- | --- | --- |
| Login and session bootstrap | Basic/session login works for tested paths. Browser session persistence, selected location, invalid credentials, Basic auth, and user properties have focused boot tests. | `SihsalusCoreApplicationTest` session, user, and auth tests; `docs/backend-endpoints.md` authentication section. | Covered for SPA bootstrap paths; not a complete auth parity claim. |
| Static module inventory | Runtime reports compiled/configured/Spring/migration status. | `/api/admin/static-modules`, `/rest/v1/module`, `StaticModuleRuntimeInspector`. | Covered for operator inventory; not equivalent to OpenMRS module admin. |
| OpenMRS module install/start/stop/admin | Dynamic `.omod` lifecycle is absent by design. | Architecture/runtime docs and static catalog model. | Not equivalent. |
| Classic OpenMRS admin UI | `/admin/index.htm` redirects to static module inventory. It is not the old admin console. | `LegacyAdminCompatibilityController`; runtime troubleshooting docs. | Not equivalent. |
| Core REST v1 resources | Generic REST controller is wired and selected resources are tested: location, patient, visit, encounter, obs, provider, user, systemsetting. | `importedOpenmrsRestResourcesAreAvailableThroughLegacyWsRestPrefix`; `docs/backend-endpoints.md`. | Partial. Selected resources are covered; every resource/verb/search shape is not proven. |
| Patient chart bootstrap | Spring-owned adapters return obs, orders, program enrollments, concept references, and term mappings for frontend-critical reads. | `PatientChartCompatibilityController`; `patientChartCompatibilityEndpointsBypassLegacyJavaxResources`. | Partial. Frontend bootstrap reads are covered; full OpenMRS REST renderer parity is not proven. |
| FHIR R4 | Metadata, selected reads, and lightweight searches are wired. Unsupported query parameters fail explicitly. | `FhirR4ReadControllerTest`; backend FHIR tests; FHIR section in `docs/backend-endpoints.md`. | Partial. Not full FHIR2 servlet/search/write/operation parity. |
| EMR API | Services are registered in `Context`; no declared REST root. | `emrApiIsWiredAsStaticInternalModule`; service-only note in `docs/backend-endpoints.md`. | Partial. Service compatibility exists; HTTP/workflow parity is not claimed. |
| Initializer/static content | Static content loader and OCL importer exist. Heavy content/OCL coverage is opt-in and depends on local content. | `SihsalusStaticContentImportTest`; disabled heavy boot-suite test; runtime hardening docs. | Partial. Must be validated with deployment content to claim functional parity. |
| OCL concepts/import | Static OCL import can fail closed and leaves idempotency markers. | `SihsalusOpenConceptLabStaticContentImporterTest`; OCL release blocker docs. | Partial. Full production content import needs release smoke. |
| O3 forms | Service and `/rest/v1/o3/forms/{formNameOrUuid}` route are wired and protected. | `o3FormsIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Real form lookup/compile/render workflows still need smoke evidence. |
| Appointments | Controllers and services are statically wired. | `appointmentsIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Create/search/status-change workflow parity needs smoke tests. |
| Queue | Services, resources, legacy queue endpoints, and some auth/failure paths are covered. | `queueIsWiredAsStaticInternalModule`, queue legacy endpoint tests. | Partial. Full operational queue workflow parity is not proven. |
| Bed management | Services/resources are wired. | `bedManagementIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Admission/assignment workflow parity needs smoke tests. |
| Billing | REST resources, services, mappings, and validators are wired. | `billingIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Receipt/payment/refund/exemption workflows need PostgreSQL and permission smokes. |
| Stock management | Services, mappings, and scheduler-related static wiring exist. | `stockManagementIsWiredAsStaticInternalModule`; stabilization plan. | Partial. Batch/report/restartability/permission workflows remain high-priority gaps. |
| Reporting and reporting REST | Reporting services and REST resources are wired. | `reportingRestIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Report execution, download, scheduling, and PostgreSQL behavior need workflow smokes. |
| Attachments | Attachment service and REST resource are wired. | `attachmentsIsWiredAsStaticInternalModule`; runtime hardening docs. | Partial. Upload/download authorization, storage, malware scanning, backup/restore are not release-complete. |
| Patient documents | PDF/document endpoints are wired. | `patientDocumentsIsWiredAsStaticInternalModule`; endpoint docs. | Partial. End-to-end generation/download workflows need real content and storage smoke. |
| Patient flags | Services and REST resources are wired. | `patientFlagsIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Legacy UI controllers under `src/main/legacy-ui` are not functional parity evidence. |
| Address hierarchy | Service and content loader support are present. | `addressHierarchyIsWiredAsStaticInternalModule`; static content checks. | Partial. Real content load is covered only by opt-in content test. |
| ID generation | Services and authorization proxy checks exist. The dev server has source `101` in PostgreSQL and `idgen` reports `started=true`, but `/ws/rest/v1/idgen/identifiersource/101/identifier` returns `404 Unknown resource`. | `idgenIsWiredAsStaticInternalModule`; auth interceptor tests; dev smoke below. | Partial. Service/DB wiring exists, but REST identifier-generation parity is missing. |
| Metadata mapping | Service and frontend lookup adapter exist. | `metadataMappingIsWiredAsStaticInternalModule`; patient chart adapter. | Partial. Selected lookup path is covered; full module parity is not proven. |
| Imaging | Services and authorization proxy checks exist. | `imagingIsWiredAsStaticInternalModule`; imaging proxy auth tests. | Partial. External imaging integration workflow parity is not proven. |
| FUA | Static service/schema wiring exists. | `fuaIsWiredAsStaticInternalModule`; stabilization queue. | Partial. Sihsalus-specific workflow parity needs persistence smoke tests. |
| Teleconsultation | Endpoint and module wiring exist. | `teleconsultationIsWiredAsStaticInternalModule`; endpoint docs. | Partial. Provider/link generation workflow parity needs integration smoke. |
| Legacy UI and legacy forms | Selected `/module/...` controllers exist for htmlwidgets and sihsalusinterop; broad legacy UI is not active. | `docs/backend-endpoints.md` module endpoints and legacy-controller note. | Partial to not equivalent, depending on the page. Do not claim classic UI parity. |
| Object-level authorization | Representative auth checks exist for selected REST/FHIR/module services. | Runtime hardening auth section; several service proxy tests. | Partial. Workflow-specific object-level authorization remains release debt. |
| PostgreSQL behavior | Liquibase is centralized and dry-run tooling exists. H2 is the normal fast smoke profile. | `docs/runtime-hardening.md`; `docs/spring-boot-runtime.md`. | Not proven for 1:1 until PostgreSQL install/upgrade smokes pass. |
| Public `/openmrs` deployment | Gateway/runbook exists and expected curl checks are documented. | `docs/ops/runtime-troubleshooting.md`. | Not proven unless run against the deployed public URL and image. |

## Dev Server Functional Smoke

Target checked on 2026-05-28:

```text
https://gidis-hsc-dev.inf.pucp.edu.pe/openmrs
```

Runtime state:

- `sihsalus-backend` container: healthy.
- `sihsalus-gateway` container: healthy.
- backend image revision: `c999fd5fd9a63933830f09480b5333352c2af87f`.
- backend context path: `/openmrs`.

Authenticated smoke results:

| Request | Result | Interpretation |
| --- | ---: | --- |
| `GET /actuator/health/readiness` | `200` | Gateway and backend readiness are up. |
| `GET /ws/rest/v1/session` | `200`, `authenticated=true` | Admin Basic auth and OpenMRS session bootstrap work. |
| `GET /admin/index.htm` | `302` | Legacy admin compatibility redirect works. |
| `GET /api/admin/static-modules` | `200` | Static module inventory is reachable. |
| `GET /ws/rest/v1/location?v=default&limit=1` | `200` | REST location read path works. |
| `GET /ws/rest/v1/patient?q=test&limit=1` | `200` | REST patient search path responds. |
| `GET /ws/rest/v1/visit?limit=1` | `200` | REST visit path responds. |
| `GET /ws/rest/v1/encounter?limit=1` | `400` | Resource does not support this list operation without the expected search shape. |
| `GET /ws/rest/v1/obs?limit=1` | `400` | Patient-scoped obs query is required for the compatibility adapter. |
| `GET /ws/fhir2/R4/Location?_count=1` | `200` | FHIR Location search works. |
| `GET /ws/fhir2/R4/Patient?_count=1` | `200` | FHIR Patient search works. |
| `GET /rest/v1/o3/forms/not-a-real-form` | `404` | O3 Forms route is present; nonexistent form returns not found. |
| `GET /rest/v1/queue-entry-number` | `200` | Queue helper endpoint responds. |
| `GET /rest/v1/billing/bill?limit=1` | `400` | Billing resource does not support this direct list operation. |
| `GET /rest/v1/reportingrest/reportDefinition?limit=1` | `200` | Reporting REST report definition path responds. |
| `GET /rest/v1/openconceptlab/import?limit=1` | `200` | OCL REST import resource responds. |
| `GET /rest/v1/attachment?limit=1` | `400` | Attachment resource does not support this direct list operation. |
| `GET /rest/v1/patientflags/flag?limit=1` | `200` | Patient flags REST path responds. |
| `POST /ws/rest/v1/idgen/identifiersource/101/identifier` | `404` | Idgen REST identifier-generation endpoint is missing from this runtime. |

Idgen detail from the same dev server:

- `/ws/rest/v1/module/idgen?v=full` reports `started=true`, `compiled=true`,
  `configured=true`, `springRegistered=true`, `databaseManaged=true`, and
  `databaseMigrated=true`.
- PostgreSQL contains `idgen_identifier_source.id = 101`,
  `uuid = 8549f706-7e85-4c1d-9424-217d50a2988b`, name
  `Generator for SIHSALUS`.

Functional conclusion from this smoke: the deployment is up and several
frontend-critical REST/FHIR paths work, but the Idgen REST identifier-generation
path expected by the client is not present. That is a functional parity gap, not
a timeout or gateway issue.

## What Is Covered

- The Maven reactor includes the OpenMRS-compatible core, static modules, and
  `apps/backend` composition root.
- `apps/backend` depends on all static modules in the runtime catalog.
- `StaticModuleRuntimeInspector` exposes compiled/configured/Spring/migration
  status through `/api/admin/static-modules`.
- Core OpenMRS REST resources are registered through the static
  `webservices-rest` runtime and the generic REST controller.
- FHIR R4 provider imports are wired through the Spring Boot FHIR controllers.
- Central Liquibase includes the OpenMRS core changelog plus migrated module
  changelogs.
- Boot tests cover many module wiring paths, selected REST/FHIR paths,
  authentication boundaries, and compatibility adapters.
- `./scripts/module-map-check.sh` passes and confirms the documented reactor
  map covers 37 Maven modules.

## What Is Not 1:1

- No dynamic `.omod` install, unload, refresh, activator lifecycle, or module
  admin behavior.
- No full classic OpenMRS WAR UI. `/admin/index.htm` is a compatibility
  redirect to `/api/admin/static-modules`, not the old admin console.
- Legacy module controllers that are present only as source or under
  `src/main/legacy-ui` are not necessarily active HTTP endpoints.
- FHIR is a Spring Boot compatibility surface around imported R4 providers,
  not full servlet parity for every FHIR2 operation.
- REST includes compatibility adapters for frontend-critical shapes such as
  session, user properties, patient chart bootstrap, and metadata lookups.
  These are deliberate bridges, not proof that every legacy REST renderer
  behaves identically.
- EMR API is service-only in this runtime; there is no declared
  `/rest/v1/emrapi/...` root.
- H2 boot smoke tests prove wiring and selected contracts, but not PostgreSQL
  parity for every migration and workflow.
- Static content and OCL import have dedicated opt-in coverage because the
  full path is heavy and depends on local content checkouts.
- Object-level authorization coverage is representative, not complete across
  all clinical, billing, stock, reporting, and attachment workflows.

## Gates To Claim 1:1

Do not claim 1:1 parity until these gates pass and are documented:

1. Restore and keep the machine-readable distro baseline under
   `config/baseline/sihsalus-distro.properties`.
2. Decide whether parity means the SIH Salus distro baseline or a newer
   upstream OpenMRS release. Record the exact OpenMRS version and distro commit.
3. Diff the runtime module inventory against the baseline and explicitly
   accept or downgrade every version difference.
4. Run PostgreSQL Liquibase status and SQL dry-run for a clean install and an
   upgrade fixture.
5. Run workflow smokes for stock, billing, O3 forms, FUA, FHIR2, REST,
   reporting, OCL, attachments, appointments, queue, and bed management.
6. Compare active HTTP endpoints against the original OpenMRS distro runtime,
   including REST resources, FHIR routes, module controllers, and legacy UI
   routes.
7. Verify object-level authorization on workflow-specific REST/FHIR paths.
8. Verify static content and OCL imports from the actual deployment content
   packages, including idempotency markers and failure behavior.
9. Validate the public gateway shape under `/openmrs`, including readiness,
   admin compatibility redirect, session, user properties, REST, and FHIR.

Until those gates are complete, the accurate status is: static module migration
is broadly covered against the SIH Salus distro baseline, but OpenMRS 1:1
runtime parity is not yet proven.
