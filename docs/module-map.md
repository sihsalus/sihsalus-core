# Module Map

This document records the current Maven reactor layout after the source tree was
organized into `core/`, `modules/`, and `apps/`.

The move is intentionally physical only. Maven artifact IDs, Java package names,
Liquibase paths inside jars, resource names, REST paths, FHIR paths, and OpenMRS
compatibility contracts remain unchanged.

## Layout Rule

The repository root remains the Maven reactor parent. Child modules now live in
functional folders:

```text
core/
  api/
  liquibase/
  openmrs-bom/

modules/
  <static Sihsalus/OpenMRS-compatible modules>

apps/
  backend/

deploy/
  compose.yml
  docker/

docs/
scripts/
.dev/reference-sources/
```

Future moves must preserve:

- Maven artifact IDs and dependency coordinates
- Java package names under `org.openmrs.*` and `org.sihsalus.*`
- Liquibase changelog paths and migration order
- Hibernate mappings and Spring bean registration order
- REST/FHIR endpoint contracts
- module identifiers used by imported OpenMRS-compatible code

## Core Foundation

These modules define the platform baseline, core domain compatibility, database
migration foundation, and executable runtime composition.

| Reactor path | Artifact ID | Responsibility |
| --- | --- | --- |
| `core/openmrs-bom` | `openmrs-bom` | dependency baseline for OpenMRS-compatible imports |
| `core/api` | `sihsalus-core-api` | OpenMRS-compatible domain, services, DAOs, utilities, and shared contracts |
| `core/liquibase` | `sihsalus-core-liquibase` | centralized database migration ordering |
| `apps/backend` | `sihsalus-core-boot` | Spring Boot runtime composition and startup configuration |

## API Adapters

These modules expose runtime contracts over the core model. They stay separate
because REST and FHIR have different compatibility surfaces.

| Reactor path | Artifact ID | Responsibility |
| --- | --- | --- |
| `modules/webservices-rest` | `sihsalus-webservices-rest` | OpenMRS REST v1 compatible API surface |
| `modules/fhir2` | `sihsalus-fhir2` | FHIR R4 API surface and resource providers |

## Operational Modules

These modules carry Sihsalus product workflows and should be treated as owned
application behavior during stabilization.

| Reactor path | Artifact ID | Responsibility |
| --- | --- | --- |
| `modules/stockmanagement` | `sihsalus-module-stockmanagement` | stock operations, inventory, batch jobs, stock reports |
| `modules/billing` | `sihsalus-module-billing` | billing workflows and receipts |
| `modules/appointments` | `sihsalus-module-appointments` | appointment scheduling model and services |
| `modules/o3forms` | `sihsalus-module-o3forms` | form schemas and form service behavior |
| `modules/fua` | `sihsalus-module-fua` | FUA-specific workflows |
| `modules/queue` | `sihsalus-module-queue` | queue and room/provider workflow |
| `modules/bedmanagement` | `sihsalus-module-bedmanagement` | bed assignment workflow |
| `modules/patientdocuments` | `sihsalus-module-patientdocuments` | patient document generation and rendering |
| `modules/attachments` | `sihsalus-module-attachments` | attachment and complex obs storage helpers |
| `modules/openconceptlab` | `sihsalus-module-openconceptlab` | OCL import and subscription behavior |
| `modules/initializer` | `sihsalus-initializer` | Sihsalus static content loading |
| `modules/sihsalusinterop` | `sihsalus-module-sihsalusinterop` | local interoperability integration |
| `modules/teleconsultation` | `sihsalus-module-teleconsultation` | teleconsultation workflow |
| `modules/imaging` | `sihsalus-module-imaging` | imaging workflow integration |

## Clinical, Reporting, And Data Modules

These modules provide cross-cutting clinical, cohort, reporting, mapping, and
calculation capabilities. Most are inherited OpenMRS-compatible APIs and should
be changed conservatively.

| Reactor path | Artifact ID | Responsibility |
| --- | --- | --- |
| `modules/emrapi` | `sihsalus-module-emrapi` | EMR API services and clinical workflow helpers |
| `modules/reporting` | `sihsalus-module-reporting` | reporting engine, datasets, evaluators, and renderers |
| `modules/reportingrest` | `sihsalus-module-reportingrest` | reporting REST resources |
| `modules/cohort` | `sihsalus-module-cohort` | cohort domain and search behavior |
| `modules/calculation` | `sihsalus-module-calculation` | calculation framework |
| `modules/metadatamapping` | `sihsalus-module-metadatamapping` | metadata mapping and term lookup |
| `modules/event` | `sihsalus-module-event` | event abstraction and message helpers |
| `modules/idgen` | `sihsalus-module-idgen` | identifier generation |
| `modules/addresshierarchy` | `sihsalus-module-addresshierarchy` | address hierarchy data and services |
| `modules/datafilter` | `sihsalus-module-datafilter` | data filter integration |

## Legacy And Compatibility Modules

These modules are compatibility surfaces or legacy UI/support pieces. They are
valid runtime code, but modernization should avoid deep rewrites unless the
runtime path is actively owned and tested.

| Reactor path | Artifact ID | Responsibility |
| --- | --- | --- |
| `modules/legacyui` | `sihsalus-module-legacyui` | legacy UI compatibility |
| `modules/htmlwidgets` | `sihsalus-module-htmlwidgets` | legacy widget rendering and parsing |
| `modules/patientflags` | `sihsalus-module-patientflags` | patient flag compatibility workflows |
| `modules/authentication` | `sihsalus-module-authentication` | static authentication integration |
| `modules/oauth2login` | `sihsalus-module-oauth2login` | static OAuth2 login integration |
| `modules/serialization-xstream` | `sihsalus-module-serialization-xstream` | XStream serialization compatibility |
| `modules/ordertemplates` | `sihsalus-module-ordertemplates` | order template compatibility |

## Apps

`apps/backend` is the executable Spring Boot backend module. Runtime wiring
still lives in the `sihsalus-core-boot` artifact so dependency coordinates remain
stable.

Future `apps/` contents should be concrete entrypoints or harnesses, such as:

- smoke-test harnesses for module workflows
- e2e support apps or fixtures
- deployment-specific backend wrappers

## Reference Sources

`.dev/reference-sources/` is for imported upstream source snapshots, comparison
copies, or migration references that are not compiled by the Maven reactor.
Checked-in reference summaries and manifests live under `docs/archive/reference-sources/`.

Rules:

- never depend on `.dev/reference-sources/` from production code
- keep references out of the parent Maven `<modules>` list
- document source version, upstream repository, and import reason
- remove references once they stop being useful for traceability

## Migration Sequence

1. Keep this map current while stabilization continues.
2. Move only directory paths unless a separate compatibility migration is
   approved.
3. Preserve packages, artifact IDs, and runtime contracts.
4. Validate with `git diff --check`, `./scripts/module-map-check.sh`, focused
   Maven compile, and full CI.

## Verification

Run this before layout-planning commits:

```bash
./scripts/module-map-check.sh
```

The check compares the parent Maven reactor with this document and fails when a
reactor path is missing from the map.
