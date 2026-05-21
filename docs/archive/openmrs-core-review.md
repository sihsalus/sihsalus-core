# OpenMRS Core Review

Review date: 2026-05-14

Sources checked:

- `openmrs/openmrs-core` GitHub metadata
- `openmrs-core` `master` branch root `pom.xml`

## Observations

- Default branch: `master`
- Latest GitHub release: `2.7.6`
- Latest release publish date: 2025-09-12
- Current master project version: `3.0.0-SNAPSHOT`
- Current master Java baseline: `maven.compiler.release=21`
- Current master minimum Maven requirement: `3.8.0`
- Current master has modules: `bom`, `tools`, `test`, `api`, `web`, `webapp`, `liquibase`, `test-suite`
- Current master uses modern quality tooling: Surefire, Failsafe-style integration profiles, Spotless, Checkstyle, SpotBugs with FindSecBugs, JaCoCo, Maven Enforcer, Liquibase Maven plugin
- Current master depends on Jakarta Servlet API `6.1.0`

## Implications For Sihsalus

Sihsalus should not start new code from an old Java baseline. Java 21 is the right target for new code and for modernization planning.

The risk is not Java 21 itself. The risk is importing mixed-era modules without classpath, servlet namespace, Spring, Hibernate, Liquibase, and OpenMRS API compatibility checks.

The current Sihsalus distro uses OpenMRS `2.8.6`, so OpenMRS Core `master` must be treated as a future-facing reference, not the immediate runtime source of truth.

## Initial Import Strategy

1. Keep a pristine reference to the Sihsalus distro baseline.
2. Keep a pristine reference to upstream OpenMRS source or release tags.
3. Import one module or backend concern at a time.
4. Record the upstream version/tag/commit for every imported block.
5. Keep Sihsalus patches small and visible.
6. Run compile, unit tests, and at least one integration smoke test before accepting an import.

## Open Questions

- Which OpenMRS Core line is the current production backend using?
- Which custom modules are mandatory for first boot?
- Which database version and changelog state are considered canonical?
- Are there existing backend patches outside module boundaries?
