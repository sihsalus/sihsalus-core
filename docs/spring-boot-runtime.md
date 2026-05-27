# Spring Boot Runtime

## Decision

Sihsalus Core is a modern Java and Spring Boot runtime around an OpenMRS-compatible
domain model. This is not a full rewrite.

The target architecture is a static Spring Boot modular monolith:

- Java 21 for new backend work.
- Spring Boot as the executable process, configuration boundary, HTTP runtime,
  health surface, and composition root.
- OpenMRS entities, database schema assumptions, service contracts, package names,
  and extension points preserved where the Sihsalus distro depends on them.
- Internal modules loaded as normal Maven/Spring code, not through the `.omod`
  install/start/stop lifecycle.

## Runtime Ownership

Spring Boot owns:

- process startup through `sihsalus-core-boot`
- environment and property loading
- datasource creation
- Liquibase startup
- Hibernate `SessionFactory` construction
- static bean composition for OpenMRS core and internal modules
- request filters, request tracing, health endpoints, and container runtime
- admin credential normalization at startup

Spring Boot does not own:

- OpenMRS database compatibility
- OpenMRS API method contracts
- inherited REST/FHIR path compatibility
- module business behavior copied from the Sihsalus distro baseline
- broad package renames or schema redesigns
- `.omod` dynamic lifecycle compatibility

## Startup Path

1. `SihsalusCoreApplication` starts the Boot application and scans `org.sihsalus`.
2. Boot loads `application.yml`, active profile overrides, environment variables,
   and container-provided settings.
3. `OpenmrsRuntimePropertiesConfigurer` maps Spring datasource settings into
   OpenMRS runtime properties before OpenMRS beans are built.
4. The application-data directory is assigned through OpenMRS runtime properties.
5. `SpringLiquibase` runs `classpath:/db/changelog/db.changelog-master.xml`.
6. Liquibase uses the OpenMRS-compatible tables `liquibasechangelog` and
   `liquibasechangeloglock`.
7. Hibernate builds the `sessionFactory` after Liquibase has completed.
8. Hibernate mapping contributors from static modules add their mapping resources.
9. OpenMRS DAOs, services, validators, handlers, message sources, and storage beans
   are registered through static Spring configuration.
10. `ServiceContext` is populated explicitly with the OpenMRS services that legacy
    `Context` callers expect.
11. `Context` is wired with the static `ServiceContext`, `ContextDAO`, and
    authentication scheme.
12. `OpenmrsAdminUserBootstrapper` rejects unsafe admin defaults and syncs scheduler
    credentials.
13. HTTP requests pass through request tracing and OpenMRS `Context` session filters.

## Configuration Surface

Required for containerized runtime:

```text
SIHSALUS_POSTGRES_PASSWORD
SIHSALUS_ADMIN_PASSWORD
```

Important runtime settings:

```text
SIHSALUS_DATASOURCE_URL
SIHSALUS_DATASOURCE_USERNAME
SIHSALUS_DATASOURCE_PASSWORD
SIHSALUS_ADMIN_USERNAME
SIHSALUS_ADMIN_PASSWORD
OPENMRS_APPLICATION_DATA_DIRECTORY
SIHSALUS_INITIALIZER_SOURCE_ROOT
SIHSALUS_OCL_STATIC_IMPORT_ENABLED
SIHSALUS_OCL_STATIC_IMPORT_FAIL_ON_ERRORS
SERVER_PORT
JAVA_OPTS
```

Supported datasource URL families in the OpenMRS runtime bridge:

- `jdbc:postgresql:` for normal runtime.
- `jdbc:h2:` for tests and compatibility checks.

Anything else should fail at startup instead of guessing a Hibernate driver or
dialect.

## Profiles And Environments

Default local runtime:

- targets PostgreSQL on `localhost`
- hides error message, binding, and stacktrace details
- exposes only `health` and `info` actuator endpoints
- rejects PostgreSQL startup when the datasource username or password is blank
- rejects inherited default admin credentials at startup

Compose runtime:

- requires `SIHSALUS_POSTGRES_PASSWORD`
- requires `SIHSALUS_ADMIN_PASSWORD`
- binds backend and PostgreSQL to localhost by default
- uses `/openmrs/data` for OpenMRS application data
- uses `/opt/sihsalus/reference-sources/sihsalus-content` for static content
- enables OCL static import and fails startup on OCL import errors by default

Test runtime:

- uses H2 for fast Spring Boot smoke coverage
- uses the `test` profile
- sets a test-only admin password
- disables the heavy OCL import path in the general boot smoke suite

H2 tests are useful for wiring and contract checks. They are not a substitute for
PostgreSQL Liquibase and runtime smoke validation.

## Stabilization Checklist

Use this checklist for every runtime-facing change:

- Keep `sihsalus-core-boot` as the single executable composition root.
- Keep `.omod` lifecycle code out of the production startup path.
- Keep Liquibase table names aligned with OpenMRS `DatabaseUpdater` expectations.
- Add or update a boot smoke test when changing static module wiring.
- Add or update a PostgreSQL dry-run note when changing Liquibase.
- Add explicit environment validation instead of relying on blank defaults.
- Do not broaden component scans without a focused test proving the new beans.
- Do not rename OpenMRS contracts unless the compatibility migration is documented.
- Verify request `Context` session behavior when adding HTTP filters or controllers.
- Verify authorization interceptors when moving OpenMRS service beans.
- Treat OCL, attachments, XStream, admin credentials, and scheduler credentials as
  production-risk areas.
- Use `docs/runtime-hardening.md` for release-facing runtime checks.
- Prefer fixing CodeQL/runtime findings over suppressing them.

## Immediate Gaps

These are stabilization gaps, not reasons to restart the architecture:

- The test profile uses H2, so PostgreSQL-only migration and SQL behavior still need
  dedicated smoke coverage.
- `ServiceContext` wiring is explicit and large; changes need coverage to avoid
  silently dropping an OpenMRS service.
- Static module tests prove wiring for many modules, but not every clinical workflow.
- Attachment malware scanning, attachment backup/restore, audit, and outage behavior
  remain release-readiness workstreams.
- Object-level authorization coverage needs to move from representative smoke tests
  to workflow-specific tests.
- The Docker image expects local static content during build; missing content should
  be treated as a deliberate deployment decision, not an accidental local state leak.

## What We Gain

This approach keeps the parts that matter for compatibility while giving Sihsalus a
modern operations surface:

- one executable backend instead of a dynamic module container
- repeatable CI and container builds
- clearer startup failure modes
- centralized migrations
- safer secret handling
- easier health checks and deployment automation
- focused modernization without breaking OpenMRS-compatible contracts
