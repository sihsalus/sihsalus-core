# Static Module Conversion Status

Date: 2026-05-14

This file tracks distro module source that has been converted from runtime `.omod` loading into SIH Salus static internal modules.

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
