# Stabilization Plan

The static module migration is complete. The active work is now stabilization: make the migrated backend predictable, secure, observable, and safe to release without changing inherited OpenMRS contracts unnecessarily.

## Stabilization Objectives

1. Keep the reactor reproducible

   The full Maven reactor, module-scoped checks, Spotless, whitespace checks, and CI baseline must remain green. New fixes should use the existing quality scripts instead of ad hoc local-only commands.

2. Reduce high-signal static-analysis debt

   Prioritize CodeQL findings that map to runtime risk: null dereferences, resource leaks, unsafe parsing, misleading control flow, mutable state exposure, and brittle casts. Avoid suppressing warnings as a default fix.

3. Verify PostgreSQL and Liquibase behavior

   Centralized migrations must be validated with dry-run status, SQL output, rollback notes, and clean-install or upgrade fixtures before release branches are cut.

4. Smoke-test critical module workflows

   Static wiring is not enough. Each critical module needs at least one smoke path that proves service registration, schema availability, permission checks, and REST/FHIR entrypoints where applicable.

5. Harden production defaults

   Secrets, admin credentials, attachment storage, OCL imports, authorization boundaries, audit behavior, and upload handling must fail closed in non-development profiles.

6. Document residual risk

   Known gaps should be tracked as release blockers, accepted risks, or post-release debt. Historical migration notes remain in `docs/archive/`; active release risk belongs in current docs.

## Workstreams

### Quality Gates

- Keep `.github/workflows/ci.yml` aligned with local quality scripts.
- Run `git diff --check` before every commit.
- Use module-scoped validation for focused patches and full reactor validation before release candidates.
- Do not add broad `@SuppressWarnings` to clear CodeQL findings without a documented compatibility reason.

### Spring Boot Runtime

- Keep `apps/backend` (`sihsalus-core-boot`) as the only executable composition root.
- Keep OpenMRS runtime properties derived from Spring configuration before OpenMRS beans initialize.
- Keep Liquibase ahead of Hibernate `SessionFactory` creation.
- Keep static module wiring covered by boot smoke tests whenever modules, mappings, filters, or service registrations move.
- Treat direct jar defaults differently from Compose defaults: Compose already requires secrets, but direct startup still needs explicit validation before production use.
- Use `docs/spring-boot-runtime.md` as the runtime map for startup sequencing, configuration, and immediate gaps.

### Reliability

- Replace manual stream/zip/file cleanup with try-with-resources.
- Convert misleading loops, dead branches, and unused local state into direct control flow.
- Make nullable values explicit at module boundaries.
- Keep batch/report jobs restartable and observable.

### Security

- Reject unsafe defaults in production profiles.
- Verify service authorization and object-level access on REST/FHIR paths.
- Restrict external import destinations and token forwarding behavior.
- Require explicit attachment allow-lists and storage containment.
- Use `docs/runtime-hardening.md` as the release checklist for secrets, OCL,
  attachments, XStream, auth, and PostgreSQL smoke.

### Database

- Validate Liquibase status and generated SQL against PostgreSQL.
- Separate install and upgrade assumptions in review notes.
- Add rollback or explicit no-rollback explanations for irreversible changes.
- Track index and long-running migration risk before release.

### Module Smoke Coverage

High-priority smoke targets:

- `modules/stockmanagement`
- `modules/o3forms`
- `modules/billing`
- `modules/fua`
- `modules/fhir2`
- `modules/webservices-rest`
- `modules/reporting`
- `modules/openconceptlab`

Each smoke target should prove:

- Spring/static service registration
- Liquibase objects exist
- minimum read or write workflow works
- expected authorization is enforced
- failure path is visible in logs or job status

## Release Candidate Criteria

A stabilization branch is release-candidate ready when:

- CI passes on a clean branch.
- `./scripts/quality-doctor.sh` passes for touched modules.
- PostgreSQL Liquibase dry-run has been reviewed.
- critical module smoke tests pass or have documented blockers.
- open CodeQL warnings are either fixed, scoped to accepted compatibility debt, or tracked with owners.
- production defaults fail closed for secrets and risky integrations.
- the runtime hardening residual-risk register has been reviewed and updated.
