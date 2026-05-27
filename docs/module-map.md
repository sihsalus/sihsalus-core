# Module Map

This document records the current runtime ownership boundaries before any
directory-layout migration. It is the phase 1 artifact for a future move toward
a clearer repository layout such as `core/`, `modules/`, `apps/`, `docs/`,
`scripts/`, and `reference-sources/`.

No source directories are moved in this phase. Maven module paths, artifact IDs,
Java package names, Liquibase paths, resource names, REST paths, FHIR paths, and
OpenMRS compatibility contracts remain unchanged.

## Current Rule

The current repository root is still a Maven reactor. Every listed module keeps
its current directory until a separate compatibility-tested layout migration is
approved.

Future moves must preserve:

- Maven artifact IDs and dependency coordinates
- Java package names under `org.openmrs.*` and `org.sihsalus.*`
- Liquibase changelog paths and migration order
- Hibernate mappings and Spring bean registration order
- REST/FHIR endpoint contracts
- module identifiers used by imported OpenMRS-compatible code

## Proposed Future Layout

```text
core/
  api/
  liquibase/
  boot/
  openmrs-bom/

modules/
  <static Sihsalus/OpenMRS-compatible modules>

apps/
  <runtime entrypoints, smoke apps, e2e harnesses>

docs/
scripts/
reference-sources/
```

This is an organizational target, not a current implementation. The active
runtime composition root remains `sihsalus-core-boot`.

## Core Foundation

These modules define the platform baseline, core domain compatibility, database
migration foundation, and executable runtime composition.

| Current module | Future area | Responsibility |
| --- | --- | --- |
| `sihsalus-openmrs-bom` | `core/openmrs-bom` | dependency baseline for OpenMRS-compatible imports |
| `sihsalus-core-api` | `core/api` | OpenMRS-compatible domain, services, DAOs, utilities, and shared contracts |
| `sihsalus-core-liquibase` | `core/liquibase` | centralized database migration ordering |
| `sihsalus-core-boot` | `core/boot` or `apps/backend` | Spring Boot runtime composition and startup configuration |

## API Adapters

These modules expose runtime contracts over the core model. They should stay
separate because REST and FHIR have different compatibility surfaces.

| Current module | Future area | Responsibility |
| --- | --- | --- |
| `sihsalus-webservices-rest` | `modules/webservices-rest` | OpenMRS REST v1 compatible API surface |
| `sihsalus-fhir2` | `modules/fhir2` | FHIR R4 API surface and resource providers |

## Operational Modules

These modules carry Sihsalus product workflows and should be treated as owned
application behavior during stabilization.

| Current module | Future area | Responsibility |
| --- | --- | --- |
| `sihsalus-module-stockmanagement` | `modules/stockmanagement` | stock operations, inventory, batch jobs, stock reports |
| `sihsalus-module-billing` | `modules/billing` | billing workflows and receipts |
| `sihsalus-module-appointments` | `modules/appointments` | appointment scheduling model and services |
| `sihsalus-module-o3forms` | `modules/o3forms` | form schemas and form service behavior |
| `sihsalus-module-fua` | `modules/fua` | FUA-specific workflows |
| `sihsalus-module-queue` | `modules/queue` | queue and room/provider workflow |
| `sihsalus-module-bedmanagement` | `modules/bedmanagement` | bed assignment workflow |
| `sihsalus-module-patientdocuments` | `modules/patientdocuments` | patient document generation and rendering |
| `sihsalus-module-attachments` | `modules/attachments` | attachment and complex obs storage helpers |
| `sihsalus-module-openconceptlab` | `modules/openconceptlab` | OCL import and subscription behavior |
| `sihsalus-initializer` | `modules/initializer` | Sihsalus static content loading |
| `sihsalus-module-sihsalusinterop` | `modules/sihsalusinterop` | local interoperability integration |
| `sihsalus-module-teleconsultation` | `modules/teleconsultation` | teleconsultation workflow |
| `sihsalus-module-imaging` | `modules/imaging` | imaging workflow integration |

## Clinical, Reporting, And Data Modules

These modules provide cross-cutting clinical, cohort, reporting, mapping, and
calculation capabilities. Most are inherited OpenMRS-compatible APIs and should
be changed conservatively.

| Current module | Future area | Responsibility |
| --- | --- | --- |
| `sihsalus-module-emrapi` | `modules/emrapi` | EMR API services and clinical workflow helpers |
| `sihsalus-module-reporting` | `modules/reporting` | reporting engine, datasets, evaluators, and renderers |
| `sihsalus-module-reportingrest` | `modules/reportingrest` | reporting REST resources |
| `sihsalus-module-cohort` | `modules/cohort` | cohort domain and search behavior |
| `sihsalus-module-calculation` | `modules/calculation` | calculation framework |
| `sihsalus-module-metadatamapping` | `modules/metadatamapping` | metadata mapping and term lookup |
| `sihsalus-module-event` | `modules/event` | event abstraction and message helpers |
| `sihsalus-module-idgen` | `modules/idgen` | identifier generation |
| `sihsalus-module-addresshierarchy` | `modules/addresshierarchy` | address hierarchy data and services |
| `sihsalus-module-datafilter` | `modules/datafilter` | data filter integration |

## Legacy And Compatibility Modules

These modules are compatibility surfaces or legacy UI/support pieces. They are
valid runtime code, but modernization should avoid deep rewrites unless the
runtime path is actively owned and tested.

| Current module | Future area | Responsibility |
| --- | --- | --- |
| `sihsalus-module-legacyui` | `modules/legacyui` | legacy UI compatibility |
| `sihsalus-module-htmlwidgets` | `modules/htmlwidgets` | legacy widget rendering and parsing |
| `sihsalus-module-patientflags` | `modules/patientflags` | patient flag compatibility workflows |
| `sihsalus-module-authentication` | `modules/authentication` | static authentication integration |
| `sihsalus-module-oauth2login` | `modules/oauth2login` | static OAuth2 login integration |
| `sihsalus-module-serialization-xstream` | `modules/serialization-xstream` | XStream serialization compatibility |
| `sihsalus-module-ordertemplates` | `modules/ordertemplates` | order template compatibility |

## Apps

`apps/` should only be introduced when there is a concrete entrypoint or harness
to place there. Candidate future contents:

- backend runtime wrapper if `sihsalus-core-boot` is separated from core wiring
- smoke-test harnesses for module workflows
- e2e support apps or fixtures

Do not create an empty `apps/` directory just for structure.

## Reference Sources

`reference-sources/` is for imported upstream source snapshots, comparison
copies, or migration references that are not compiled by the Maven reactor.

Rules:

- never depend on `reference-sources/` from production code
- keep references out of the parent Maven `<modules>` list
- document source version, upstream repository, and import reason
- remove references once they stop being useful for traceability

## Migration Sequence

1. Keep this map current while stabilization continues.
2. Pick one low-risk pilot move after the reactor and CI are stable.
3. Move only directory paths in the pilot; preserve packages, artifact IDs, and
   runtime contracts.
4. Validate with `git diff --check`, focused Maven compile, and full CI.
5. Repeat by group only after the pilot proves that tooling, IDE imports, and CI
   handle the new layout.
