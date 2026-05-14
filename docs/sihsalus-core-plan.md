# SIH Salus Core Plan

Date: 2026-05-14

## Goal

Build SIH Salus Core as our own health information system core, using OpenMRS and the current SIH Salus distro as references, not as boundaries that must be preserved forever.

The objective is not to produce a cosmetic fork. The objective is to understand the useful clinical, operational, and interoperability behavior from OpenMRS, then design a SIH Salus-owned backend that can evolve independently.

## Working Principle

OpenMRS is reference material.

SIH Salus Core owns:

- domain model decisions
- API shape
- database ownership
- module boundaries
- security model
- deployment model
- migration strategy
- frontend contracts

Compatibility with OpenMRS can be useful during migration, but it is not the final architecture constraint unless we explicitly decide it is.

## Reference Sources

Local reference code:

- `reference-sources/openmrs-distro-modules`: OpenMRS core, OpenMRS modules, Bahmni modules, SIH Salus local modules, and reference content packages from the distro POM.
- `reference-sources/sihsalus-content`: SIH Salus content package, kept separately because it represents configuration/content, not core module source.

Reference docs:

- `docs/openmrs-distro-pom-reference.md`
- `docs/sihsalus-distro-baseline.md`
- `docs/architecture.md`
- `docs/migration-plan.md`

## Product Surface To Cover

SIH Salus Core should eventually cover these capabilities:

- patients and demographics
- identifiers
- users, roles, privileges, sessions, OAuth/login
- locations and address hierarchy
- concepts and metadata mapping
- encounters, visits, observations, diagnoses, allergies, conditions
- orders and order templates
- forms
- documents and attachments
- queues
- appointments
- teleconsultation
- bed management/admissions
- cohorts
- patient flags
- reporting and calculations
- stock management
- billing
- FUA workflows
- imaging workflows
- REST API
- FHIR API
- events/integration hooks
- initializer/content loading
- audit trail
- migration/import tools
- deployment packaging

## Architecture Direction

### Core First

Start with a coherent core domain instead of importing modules randomly.

Initial bounded contexts:

- Identity and access
- Facility/location
- Patient registry
- Clinical dictionary
- Clinical record
- Orders
- Documents
- Scheduling and queue
- Reporting
- Billing and stock
- Interoperability
- Configuration/content loading

### API First

Define APIs around SIH Salus workflows, not around OpenMRS class names.

Required API groups:

- `/api/patients`
- `/api/encounters`
- `/api/observations`
- `/api/concepts`
- `/api/forms`
- `/api/documents`
- `/api/appointments`
- `/api/queue`
- `/api/beds`
- `/api/reports`
- `/api/billing`
- `/api/stock`
- `/api/fhir`
- `/api/admin`

### Migration Friendly

Even if the final architecture is ours, we need migration adapters:

- OpenMRS database reader/importer
- OpenMRS UUID preservation strategy
- concept/content import pipeline
- patient import pipeline
- encounter/obs import pipeline
- document import pipeline
- user/role import pipeline
- validation reports after import

## Phase 0: Baseline And Inventory

Status: in progress.

Deliverables:

- local reference source clones
- distro POM summary
- SIH Salus content separated as reference content
- module/capability inventory
- first domain map
- API compatibility map
- database object inventory

Acceptance:

- every capability from the distro POM is listed
- each capability has a keep, replace, redesign, or defer decision
- each external source has repository, branch, commit, and version notes

## Phase 1: Core Skeleton

Create the SIH Salus Core application skeleton.

Deliverables:

- application runtime
- database migration tool
- healthcheck
- configuration system
- structured logging
- audit foundation
- test baseline
- local Docker Compose

Acceptance:

- app starts locally from a clean checkout
- database boots and migrates
- healthcheck passes
- CI can compile and test

## Phase 2: Identity And Facility Foundation

Build the minimum secure administrative foundation.

Deliverables:

- users
- roles
- permissions
- sessions/tokens
- OAuth strategy
- organizations/facilities/locations
- address hierarchy
- audit events

Acceptance:

- admin user can authenticate
- permissions can protect APIs
- locations can be created and queried
- audit records are written for security-sensitive actions

## Phase 3: Patient Registry

Build the patient identity layer.

Deliverables:

- patient
- person names
- demographics
- addresses
- identifiers
- identifier generation
- patient search
- duplicate detection baseline
- import adapter for OpenMRS patient data

Acceptance:

- patient CRUD works
- identifiers are unique and auditable
- patient search supports common clinical workflows
- imported OpenMRS patients can preserve source UUIDs

## Phase 4: Clinical Dictionary And Content

Own concepts and metadata content.

Deliverables:

- concept model
- concept mappings
- metadata model
- SIH Salus content import
- reference content import
- validation report for content package

Acceptance:

- SIH Salus content can be loaded from `reference-sources/sihsalus-content`
- concepts and mappings can be queried by API
- invalid content fails with actionable validation errors

## Phase 5: Clinical Record

Build the minimal clinical record.

Deliverables:

- visits/encounters
- observations
- diagnoses
- allergies/conditions
- forms baseline
- attachments/documents
- REST API
- FHIR read/write subset

Acceptance:

- a clinician-facing workflow can create an encounter with observations
- documents can be attached to a patient
- FHIR Patient, Encounter, Observation, and Condition are usable at a practical baseline

## Phase 6: Operational Workflows

Build the workflow layer.

Deliverables:

- queue
- appointments
- teleconsultation
- bed management/admissions
- patient flags
- cohorts

Acceptance:

- patient can move through queue states
- appointment can be scheduled and completed/cancelled
- bed assignment can be tracked

## Phase 7: Finance, Stock, FUA, Imaging

Build or adapt local operational modules.

Deliverables:

- stock management model
- billing model
- FUA workflow
- imaging workflow
- integration points with patient and encounter records

Acceptance:

- billing and stock can reference clinical activity
- FUA and imaging workflows preserve required local behavior
- APIs are documented enough for frontend integration

## Phase 8: Reporting And Interoperability

Build reporting and integration support.

Deliverables:

- report definitions
- calculation engine or replacement
- report execution API
- event publishing
- export/import jobs
- FHIR expansion

Acceptance:

- core operational reports can run
- events can be consumed by external integrations
- FHIR API covers required exchange scenarios

## Immediate Next Decisions

1. Choose the implementation stack for SIH Salus Core.
2. Decide whether the first runtime should be compatible with OpenMRS database imports or start with a clean SIH Salus schema plus import tools.
3. Pick the first vertical slice: recommended slice is patient registry plus concepts/content import.
4. Define the minimum frontend contract that must keep working first.
5. Decide what "OpenMRS compatible" means during migration: API compatible, database import compatible, module compatible, or only behavior compatible.

## Recommended Next Slice

Start with:

- content inventory from `reference-sources/sihsalus-content`
- concept/metadata import design
- patient registry model
- patient identifier strategy
- REST API skeleton

This gives SIH Salus Core a real clinical foundation without getting blocked by the full OpenMRS module ecosystem.
