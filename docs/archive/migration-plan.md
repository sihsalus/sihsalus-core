# Migration Plan

## Phase 0: Foundation

Status: in progress.

Deliverables:

- repository structure
- Java 21 parent POM
- Sihsalus distro baseline
- CI baseline
- OpenMRS core review
- security baseline
- import rules

## Phase 1: Inventory

Map the current backend and module estate.

Outputs:

- current backend version
- current Sihsalus distro versions
- database engine/version
- module list with versions
- custom patches
- deployment topology
- secrets/config inventory
- frontend compatibility matrix

## Phase 2: Minimal Runtime

Create a reproducible local runtime.

Outputs:

- Docker Compose
- `.env.example`
- healthcheck
- first boot documentation
- backup/restore draft

## Phase 3: First Module Import

Choose a low-risk module with clear value.

Acceptance:

- compiles with Java 21 baseline or has documented compatibility exception
- tests pass
- module ownership documented
- frontend flow validated
- security review complete

## Phase 4: Production Readiness

Outputs:

- container image
- CI release workflow
- dependency audit
- SBOM
- vulnerability scan
- backup/restore test
- logging policy
- runbook
- rollback plan
