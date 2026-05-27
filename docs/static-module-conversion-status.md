# Static Module Conversion and Stabilization Status

Date: 2026-05-27

The static module migration from runtime `.omod` loading to SIH Salus internal Maven modules is complete. This file now tracks stabilization status for the migrated runtime.

Use this file for module status only. Runtime release checks live in
`runtime-hardening.md`; historical conversion details below are traceability notes,
not an operational checklist.

## Migration Outcome

The Maven reactor is the source of truth for build participation. The compatibility baseline remains `config/baseline/sihsalus-distro.properties`.

Status: migrated, now stabilizing.

- The reactor contains modules that are built into the static backend runtime.
- Module source is owned in-tree instead of loaded through runtime `.omod` packages.
- Static Spring/application composition is the runtime model.
- Centralized Liquibase is the database migration model.
- Legacy OMOD activators are compatibility source only, not runtime entrypoints.
- `sihsalus-module-identitylookup` was removed because it had no current distro source baseline and only existed as a placeholder.
- Future identity lookup work should be added as a real feature module when the product scope is defined.

## Stabilization Status Model

Use these levels when reporting module stabilization:

- `compiled`: module builds as part of the Maven reactor.
- `wired`: Spring/static service registration is present.
- `endpoint verified`: REST, FHIR, or web entrypoint is reachable where applicable.
- `postgres verified`: schema and minimum workflow have been smoke-tested on PostgreSQL.
- `security verified`: authorization and unsafe-default behavior have focused checks.
- `release ready`: module has smoke coverage, known residual risks, and rollback/migration notes.

## Stabilization Queue

High priority:

- `sihsalus-module-stockmanagement`: batch/report restartability, CSV output, permissions, Liquibase order, billing integration.
- `sihsalus-module-o3forms`: service registration, `/rest/v1/o3/forms/{formNameOrUuid}`, schema and form compilation smoke tests.
- `sihsalus-module-billing`: service registration, Hibernate mappings, Liquibase order, receipt workflow, REST surface.
- `sihsalus-module-fua`: Sihsalus-specific workflow ownership, API compatibility, persistence smoke tests.
- `sihsalus-fhir2`: R4 read/search smoke tests, authorization, DAO null-safety.
- `sihsalus-webservices-rest`: REST v1 resource smoke tests and object-level authorization.

Medium priority:

- `sihsalus-module-appointments`
- `sihsalus-module-queue`
- `sihsalus-module-bedmanagement`
- `sihsalus-module-patientflags`
- `sihsalus-module-openconceptlab`
- `sihsalus-module-event`
- `sihsalus-module-teleconsultation`
- `sihsalus-module-reporting`
- `sihsalus-module-reportingrest`
- `sihsalus-module-attachments`
- `sihsalus-module-cohort`

Compiled and migrated, but still requiring targeted stabilization evidence:

- `sihsalus-module-authentication`
- `sihsalus-module-oauth2login`
- `sihsalus-module-idgen`
- `sihsalus-module-addresshierarchy`
- `sihsalus-module-emrapi`
- `sihsalus-module-calculation`
- `sihsalus-module-htmlwidgets`
- `sihsalus-module-serialization-xstream`
- `sihsalus-module-metadatamapping`
- `sihsalus-module-imaging`
- `sihsalus-module-ordertemplates`
- `sihsalus-module-patientdocuments`
- `sihsalus-module-legacyui`
- `sihsalus-module-datafilter`

## Historical Conversion Notes

### Reporting

Status: static internal block.

Scope:

- `sihsalus-module-reporting`
- `sihsalus-module-reportingrest`
- reporting Liquibase included through `sihsalus-core-liquibase`
- reporting Hibernate mappings contributed statically
- reporting services registered in `ServiceContext`
- reporting REST resources registered through the static REST framework

Source baseline:

- Distro artifact: `reporting-omod` `2.1.0`
- Distro artifact: `reportingrest-omod` `2.0.0`
- Local source: `.dev/reference-sources/openmrs-distro-modules/reporting`
- Local source: `.dev/reference-sources/openmrs-distro-modules/reportingrest`

Runtime decision:

- No `.omod` install/start/stop lifecycle.
- No dynamic module discovery for Hibernate mappings or Spring wiring.
- Legacy OMOD activators are not runtime entrypoints.
- Reporting and Reporting REST are treated as one backend capability block.

Verification:

- `.dev/reference-sources/openmrs-distro-modules/openmrs-webapp/mvnw --batch-mode --no-transfer-progress -Denforcer.skip=true -pl apps/backend -am test`
- Result on 2026-05-14: 35-module reactor passed; `SihsalusCoreApplicationTest` ran 20 tests with 0 failures and 0 errors.

Known follow-up:

- Run persistence smoke tests against PostgreSQL for `ReportRequest` and `ReportDesign`.
- Add report execution API coverage once the reporting workflow is exposed as product surface.

## Active Stabilization Block

### O3 Forms

Status: migrated, needs stabilization smoke coverage.

Scope:

- `sihsalus-module-o3forms`
- O3 Forms service
- O3 Forms REST controller
- O3 Forms Liquibase included through `sihsalus-core-liquibase`

Source baseline:

- Distro artifact: `o3forms-omod` `2.3.0`
- Local source: `.dev/reference-sources/openmrs-distro-modules/o3forms`

Runtime decision:

- No `.omod` install/start/stop lifecycle.
- API/service code is local source.
- REST endpoint is statically scanned by Spring.

Stabilization target:

- `sihsalus-module-o3forms` compiles as an internal Maven jar.
- `sihsalus-core-boot` starts with `O3FormsService` registered in `ServiceContext`.
- `/rest/v1/o3/forms/{formNameOrUuid}` is reachable through the static REST stack.
- PostgreSQL smoke test validates the minimum form lookup and compilation path.
