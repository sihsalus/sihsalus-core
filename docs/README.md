# Documentation

Active documentation lives at the top of this directory. Historical planning notes and one-time reviews live under `archive/`.

## Active

- `architecture.md`: current architecture direction and compatibility boundaries.
- `spring-boot-runtime.md`: executable runtime map, configuration surface, and stabilization checklist.
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

## Checks

- `../scripts/module-map-check.sh`: verifies that `module-map.md` covers every Maven reactor module.

## Archive

- `archive/`: dated audits, completed debt phases, distro review, and migration reference material kept for traceability.
