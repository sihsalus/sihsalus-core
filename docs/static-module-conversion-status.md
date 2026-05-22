# Static Module Conversion Status

Date: 2026-05-20

This file tracks distro module source that has been converted from runtime `.omod` loading into SIH Salus static internal modules.

## Triage Status

The Maven reactor is the source of truth for build participation. The compatibility baseline remains `baseline/sihsalus-distro.properties`.

### Reactor Integrity

Status: repaired in-tree.

- The reactor contains only modules that are built into the static backend runtime.
- `sihsalus-module-identitylookup` was removed because it had no current distro source baseline and only existed as a placeholder.
- Future identity lookup work should be added as a real feature module when the product scope is defined.

### Operational Status Model

Use these levels when reporting module progress:

- `placeholder`: Maven module exists, no imported runtime behavior.
- `source imported`: upstream or distro source exists locally, but static Spring/runtime wiring is incomplete.
- `spring wired`: module has static configuration and can participate in application composition.
- `service registered`: module services are registered in OpenMRS `ServiceContext` or equivalent static registry.
- `endpoint wired`: REST/FHIR/web endpoints are statically exposed.
- `postgres verified`: module schema and minimum workflow have been smoke-tested on PostgreSQL.

### Current Porting Queue

High priority:

- `sihsalus-module-o3forms`: finish acceptance checks for service registration and `/rest/v1/o3/forms/{formNameOrUuid}`.
- `sihsalus-module-billing`: confirm static service registration, Hibernate mappings, Liquibase order, and REST surface.
- `sihsalus-module-stockmanagement`: confirm service registration, metadata loading, Liquibase order, and billing integration.
- `sihsalus-module-fua`: confirm Sihsalus-specific workflow ownership and API compatibility.

Medium priority:

- `sihsalus-module-appointments`
- `sihsalus-module-queue`
- `sihsalus-module-bedmanagement`
- `sihsalus-module-patientflags`
- `sihsalus-module-openconceptlab`
- `sihsalus-module-event`
- `sihsalus-module-teleconsultation`

Already substantially imported, but still requiring workflow/PostgreSQL verification:

- `sihsalus-fhir2`
- `sihsalus-webservices-rest`
- `sihsalus-module-authentication`
- `sihsalus-module-oauth2login`
- `sihsalus-module-idgen`
- `sihsalus-module-addresshierarchy`
- `sihsalus-module-emrapi`
- `sihsalus-module-reporting`
- `sihsalus-module-reportingrest`
- `sihsalus-module-calculation`
- `sihsalus-module-htmlwidgets`
- `sihsalus-module-serialization-xstream`
- `sihsalus-module-metadatamapping`
- `sihsalus-module-attachments`
- `sihsalus-module-cohort`
- `sihsalus-module-imaging`
- `sihsalus-module-ordertemplates`
- `sihsalus-module-patientdocuments`
- `sihsalus-module-legacyui`
- `sihsalus-module-datafilter`

## Completed Blocks

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
- Local source: `reference-sources/openmrs-distro-modules/reporting`
- Local source: `reference-sources/openmrs-distro-modules/reportingrest`

Runtime decision:

- No `.omod` install/start/stop lifecycle.
- No dynamic module discovery for Hibernate mappings or Spring wiring.
- Legacy OMOD activators are not runtime entrypoints.
- Reporting and Reporting REST are treated as one backend capability block.

Verification:

- `reference-sources/openmrs-distro-modules/openmrs-webapp/mvnw --batch-mode --no-transfer-progress -Denforcer.skip=true -pl sihsalus-core-boot -am test`
- Result on 2026-05-14: 35-module reactor passed; `SihsalusCoreApplicationTest` ran 20 tests with 0 failures and 0 errors.

Known follow-up:

- Run persistence smoke tests against PostgreSQL for `ReportRequest` and `ReportDesign`.
- Add report execution API coverage once the reporting workflow is exposed as product surface.

## Current Block

### O3 Forms

Status: static internal source import in progress.

Scope:

- `sihsalus-module-o3forms`
- O3 Forms service
- O3 Forms REST controller
- O3 Forms Liquibase included through `sihsalus-core-liquibase`

Source baseline:

- Distro artifact: `o3forms-omod` `2.3.0`
- Local source: `reference-sources/openmrs-distro-modules/o3forms`

Runtime decision:

- No `.omod` install/start/stop lifecycle.
- API/service code is local source.
- REST endpoint is statically scanned by Spring.

Acceptance target:

- `sihsalus-module-o3forms` compiles as an internal Maven jar.
- `sihsalus-core-boot` starts with `O3FormsService` registered in `ServiceContext`.
- `/rest/v1/o3/forms/{formNameOrUuid}` is reachable through the static REST stack.
