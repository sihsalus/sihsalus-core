# Documentation

Active documentation is intentionally small. Historical migration notes were removed from the active tree; the current source of truth is:

- `backend.md`: backend architecture, OpenMRS compatibility rules, module baseline, endpoint surface, parity status.
- `deploy.md`: Docker image, Compose runtime, `/openmrs` gateway, health checks, operational smoke.
- `release.md`: CI gates, scripts, Maven profiles, PostgreSQL/Liquibase validation, security and release criteria.

Keep new operational facts in one of these files instead of adding another one-off planning document.
