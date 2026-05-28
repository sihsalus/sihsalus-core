# Backend Endpoints

This document maps the HTTP surface exposed by the Spring Boot backend. Paths are written relative
to the application root. In deployments mounted at `/openmrs`, prepend `/openmrs` to each path.

Coverage note: this file is audited against the active Spring Boot component scans, Spring MVC
`@RequestMapping` declarations, static-module REST `@Resource`/`@SubResource` annotations, and FHIR
R4 provider classes. Core OpenMRS resources are still dynamic because they are imported from
`webservices.rest-omod`; the generic OpenMRS REST controller exposes the runtime resource catalog.

## Authentication

Most REST, FHIR, admin, and legacy module endpoints require an authenticated OpenMRS user.

- Basic auth is accepted by the REST and FHIR filters.
- `Disable-WWW-Authenticate: true` suppresses the browser-native Basic auth challenge on `401`.
- `GET /rest/v1/session` and `GET /ws/rest/v1/session` return the current OpenMRS session state.
  When authenticated, the payload includes `sessionId`, `sessionLocation`, `userProperties`,
  `privileges`, and `roles`, and the response creates a `JSESSIONID` cookie.

## Runtime

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/actuator/health` | Spring Boot health check. |
| `GET` | `/api/system/info` | Runtime metadata, including static module/runtime status. |
| `GET` | `/api/admin/static-modules` | Static module inventory for operators, including compiled/configured/Spring/migration/scheduler signals. |
| `GET` | `/spa/frontend.json` | SPA frontend manifest, if packaged in the runtime image. |
| `GET` | `/spa/importmap.json` | SPA import map, if packaged in the runtime image. |

## OpenMRS REST

Both prefixes route to the same REST runtime:

- `/rest/v1`
- `/ws/rest/v1`

The `/ws/rest/v1` prefix exists for compatibility with OpenMRS frontend clients and older module
URLs. The root endpoint returns the REST version and URI prefix:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/rest/v1` | REST root. |
| `GET` | `/ws/rest/v1` | Compatibility REST root. |
| `GET` | `/rest/v1/session` | Session state. |
| `GET` | `/ws/rest/v1/session` | Compatibility session state. |

Generic resource operations are handled by the OpenMRS REST controllers when the resource supports
the operation:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/rest/v1/{resource}` | List or search a resource. Support depends on the resource. |
| `GET` | `/rest/v1/{resource}/{uuid}` | Retrieve by UUID. |
| `POST` | `/rest/v1/{resource}` | Create. |
| `POST` | `/rest/v1/{resource}/{uuid}` | Update. |
| `DELETE` | `/rest/v1/{resource}/{uuid}` | Retire, void, or delete. |
| `GET/POST/DELETE/PUT` | `/rest/v1/{resource}/{parentUuid}/{subResource}` | Subresource operations. |

The same patterns are available through `/ws/rest/v1/{resource}`.

Core OpenMRS resources are provided by the upstream `webservices.rest-omod` resource classes and are
registered in the static backend runtime. The compatibility smoke test currently validates:

- `/ws/rest/v1/location`
- `/ws/rest/v1/patient`
- `/ws/rest/v1/visit`
- `/ws/rest/v1/encounter`
- `/ws/rest/v1/obs`
- `/ws/rest/v1/provider`
- `/ws/rest/v1/user`
- `/ws/rest/v1/systemsetting`

Important behavior: availability does not mean every resource supports anonymous list-all access.
For example, patient retrieval by UUID is validated through `/ws/rest/v1/patient/{uuid}`, and patient
search is validated as a search endpoint returning a `results` array.

## FHIR R4

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/fhir/metadata` | R4 capability statement. |
| `GET` | `/api/fhir/CapabilityStatement` | R4 capability statement alias. |
| `GET` | `/api/fhir/r4/metadata` | R4 capability statement alias. |
| `GET` | `/ws/fhir2/R4/metadata` | OpenMRS FHIR2 compatibility metadata route. |
| `GET` | `/api/fhir/{resourceType}/{id}` | R4 read. |
| `GET` | `/api/fhir/r4/{resourceType}/{id}` | R4 read alias. |
| `GET` | `/ws/fhir2/R4/{resourceType}/{id}` | FHIR2 compatibility read route. |
| `GET` | `/api/fhir/{resourceType}` | R4 search bundle route. |
| `GET` | `/api/fhir/r4/{resourceType}` | R4 search bundle alias. |
| `GET` | `/ws/fhir2/R4/{resourceType}` | FHIR2 compatibility search route. |

The current lightweight FHIR search route supports the neutral parameters `_count`, `_format`, and
`_pretty`. Other query parameters return an `OperationOutcome` with `issue.code = invalid` instead
of being silently ignored.

Registered R4 resource types:

- `AllergyIntolerance`
- `Condition`
- `DiagnosticReport`
- `Encounter`
- `EpisodeOfCare` (read only)
- `Group`
- `Immunization`
- `Location`
- `Medication`
- `MedicationDispense`
- `MedicationRequest`
- `Observation`
- `Patient`
- `Person`
- `Practitioner`
- `RelatedPerson`
- `ServiceRequest`
- `Task`
- `ValueSet`

Validated examples:

- `GET /ws/fhir2/R4/Location?_count=1`
- `GET /api/fhir/r4/Patient/{uuid}`

## Module REST Resources

These modules expose explicit REST resources or controllers in addition to the core OpenMRS REST
catalog.

| Module | Public path roots |
| --- | --- |
| Appointments | `/rest/v1/appointment`, `/rest/v1/appointments`, `/rest/v1/recurring-appointments`, `/rest/v1/appointmentService`, `/rest/v1/appointment-services`, `/rest/v1/appointment-service-attribute-types`, `/rest/v1/speciality`, `/rest/v1/adhocTeleconsultation` |
| Attachments | `/rest/v1/attachment`, `/rest/v1/attachment/{uuid}/bytes` |
| Bed management | `/rest/v1/admissionLocation`, `/rest/v1/bed`, `/rest/v1/beds`, `/rest/v1/bedPatientAssignment`, `/rest/v1/bedTag`, `/rest/v1/bedTagMap`, `/rest/v1/bedtype` |
| Billing | `/rest/v1/billing/bill`, `/rest/v1/billing/bill/{uuid}/payment`, `/rest/v1/billing/billDiscount`, `/rest/v1/billing/billExemption`, `/rest/v1/billing/billExemption/{uuid}/rule`, `/rest/v1/billing/billLineItem`, `/rest/v1/billing/billRefund`, `/rest/v1/billing/billableService`, `/rest/v1/billing/cashierItemPrice`, `/rest/v1/billing/cashPoint`, `/rest/v1/billing/paymentAttribute`, `/rest/v1/billing/paymentMode`, `/rest/v1/billing/paymentModeAttributeType`, `/rest/v1/billing/attributetype`, `/rest/v1/billing/receipt`, `/rest/v1/billing/api/billable-service`, `/rest/v2/billing/timesheet` |
| Cohort | `/rest/v1/cohortm/cohort`, `/rest/v1/cohortm/cohort/{uuid}/attribute`, `/rest/v1/cohortm/cohortmember`, `/rest/v1/cohortm/cohortmember/{uuid}/attribute`, `/rest/v1/cohortm/cohorttype`, `/rest/v1/cohortm/cohortattributetype`, `/rest/v1/cohortm/cohort-member-attribute-type` |
| HTML Widgets | `/module/htmlwidgets/conceptSearch.form`, `/module/htmlwidgets/personSearch.form`, `/module/htmlwidgets/userSearch.form`, `/module/htmlwidgets/patientSearch.form`, `/module/htmlwidgets/demonstration.form` |
| O3 Forms | `/rest/v1/o3/forms/{formNameOrUuid}` |
| Open Concept Lab | `/rest/v1/openconceptlab/import`, `/rest/v1/openconceptlab/import/{uuid}/item`, `/rest/v1/openconceptlab/importaction`, `/rest/v1/openconceptlab/subscription` |
| Order Templates | `/rest/v1/ordertemplates/orderTemplate` |
| Patient Documents | `/rest/v1/patientdocuments/patientIdSticker`, `/rest/v1/patientdocuments/encounters`, `/rest/v1/patientdocuments/encounters/status/{requestUuid}`, `/rest/v1/patientdocuments/encounters/download/{requestUuid}` |
| Patient Flags | `/rest/v1/patientflags/flag`, `/rest/v1/patientflags/tag`, `/rest/v1/patientflags/priority`, `/rest/v1/patientflags/displaypoint`, `/rest/v1/patientflags/patientflag` |
| Queue | `/rest/v1/queue`, `/rest/v1/queue/{uuid}/entry`, `/rest/v1/queue-entry`, `/rest/v1/queue-entry/transition`, `/rest/v1/queue-entry-metric`, `/rest/v1/queue-entry-metrics`, `/rest/v1/queue-entry-number`, `/rest/v1/queue-metrics`, `/rest/v1/queue-room`, `/rest/v1/queue-room-provider`, `/rest/v1/queueroom`, `/rest/v1/roomprovidermap`, `/rest/v1/visit-queue-entry`, `/rest/v1/queueutil/active-tickets`, `/rest/v1/queueutil/assignticket` |
| Reporting REST | `/rest/v1/reportingrest/reportDefinition`, `/rest/v1/reportingrest/reportRequest`, `/rest/v1/reportingrest/reportDesign`, `/rest/v1/reportingrest/reportDefinitionsWithScheduledRequests`, `/rest/v1/reportingrest/cohortDefinition`, `/rest/v1/reportingrest/cohort`, `/rest/v1/reportingrest/dataSet`, `/rest/v1/reportingrest/dataSetDefinition`, `/rest/v1/reportingrest/reportdata`, `/rest/v1/reportingrest/definitionlibrary`, `/rest/v1/reportingrest/adhocquery`, `/rest/v1/reportingrest/adhocdataset`, `/rest/v1/reportingrest/saveReport`, `/rest/v1/reportingrest/downloadReport`, `/rest/v1/reportingrest/downloadMultipleReports`, `/rest/v1/reportingrest/reportDataSet/{reportDefinitionUuid}/{dataSetKey}`, `/rest/v1/reportingrest/runReport/{reportDefinitionUuid}`, `/rest/v1/reportingrest/runReport/{reportDefinitionUuid}/{reportDesignUuid}` |
| SIH Salus Interop | `/rest/v1/interop/send`, `/rest/v1/interop/processQueue`, `/rest/v1/interop/queue`, `/rest/v1/interop/queue/{id}`, `/rest/v1/interop/queue/{id}/retry`, `/rest/v1/interop/patient/{identifier}`, `/rest/v1/interop/patient/import/{identifier}`, `/rest/v1/interop/status`, `/rest/v1/interop/terminology/check`, `/module/sihsalusinterop/api/queue/patient/{patientId}`, `/module/sihsalusinterop/api/queue/process`, `/module/sihsalusinterop/api/queue/items`, `/module/sihsalusinterop/api/queue/items/status/{status}`, `/module/sihsalusinterop/api/queue/retry/{queueId}`, `/module/sihsalusinterop/api/queue/stats`, `/module/sihsalusinterop/monitoreoInteroperabilidad.form`, `/module/sihsalusinterop/monitoreoInteroperabilidad` |
| Teleconsultation | `/rest/v1/teleconsultation/generateLink` |

All `/rest/...` module endpoints above are also reachable through the matching `/ws/rest/...`
compatibility prefix because the backend forwards `/ws/rest/...` to `/rest/...`.

Legacy controllers that exist in source but are not part of the active Spring component scan are not
listed as active HTTP endpoints.

## Service-Only Modules

Some static modules are intentionally available as Java/Spring services and database model support,
not as HTTP endpoints. EMR API is in this group.

EMR API currently registers services including:

- `EmrConceptService`
- `EmrPatientService`
- `AdtService`
- `ProcedureService`
- `AccountService`
- `DispositionService`
- `DiagnosisService`

The backend test `emrApiIsWiredAsStaticInternalModule` validates these service registrations and the
EMR API Liquibase tables. No `/rest/v1/emrapi/...` endpoint root is currently declared in
`modules/emrapi`.

## Verification

The current endpoint wiring is covered by these focused checks:

```bash
mvn -pl modules/fhir2 -Dtest=FhirR4ReadControllerTest test
mvn -pl apps/backend -am -Dtest=SihsalusCoreApplicationTest#patientRegistryPatientRequiresAuthenticationThroughRestAndFhir+importedOpenmrsRestResourcesAreAvailableThroughLegacyWsRestPrefix+fhirR4SearchEndpointInvokesImportedProvider -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl apps/backend -am -Dtest=SihsalusCoreApplicationTest#emrApiIsWiredAsStaticInternalModule -Dsurefire.failIfNoSpecifiedTests=false test
```
