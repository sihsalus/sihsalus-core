# Documentation

Active documentation lives at the top of this directory. Historical planning notes and one-time reviews live under `archive/`.

## Active

- `architecture.md`: current architecture direction and compatibility boundaries.
- `spring-boot-runtime.md`: executable runtime map, configuration surface, and stabilization checklist.
- `openmrs-parity-audit.md`: current verdict on OpenMRS 1:1 parity, module coverage, and remaining release gates.
- `backend-endpoints.md`: HTTP endpoint map for REST, FHIR, module controllers, and service-only modules.
- `module-map.md`: current module ownership boundaries and future repository layout target.
- `stabilization-plan.md`: current stabilization plan now that static module migration is complete.
- `runtime-hardening.md`: operational checklist for secrets, OCL, attachments, XStream, auth, and PostgreSQL smoke.
- `static-module-conversion-status.md`: migration outcome and stabilization verification queue.
- `java-modernization.md`: Java baseline and modernization rules.
- `technical-debt-modernization.md`: prioritized modernization targets and antipatterns to remove.
- `security-baseline.md`: security rules required for production readiness.
- `ci-baseline.md`: intended CI verification baseline.
- `database-migrations.md`: Liquibase dry-run and migration review procedure.
- `ops/README.md`: runtime image, required environment, and deployment notes.
- `ops/runtime-troubleshooting.md`: gateway shape, timeout defaults, health checks, and runtime symptom triage.

## Checks

- `../scripts/module-map-check.sh`: verifies that `module-map.md` covers every Maven reactor module.
- `../scripts/validate-runtime.sh`: runs the fast local runtime validation set used before Docker/CI-facing changes.
- `../scripts/git-hooks/pre-push`: optional versioned pre-push hook that runs `git diff --check` defensively.

## Archive

- `archive/`: dated audits, completed debt phases, distro review, and migration reference material kept for traceability.
