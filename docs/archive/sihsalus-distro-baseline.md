# Sihsalus Distro Baseline

Review date: 2026-05-14

Source of truth:

- Repository: `sihsalus/sihsalus`
- Branch checked: `main`
- Commit checked: `c6d4e6a5`
- Files checked: root `pom.xml`, `backend/pom.xml`, `backend/distro.properties`, `backend/distro-no-demo.properties`

## Baseline Decision

The initial compatibility baseline for Sihsalus Core is the current Sihsalus distro, not OpenMRS Core `master`.

OpenMRS Core `master` is useful as a modernization signal, but imports and runtime compatibility must first be checked against the versions already used by the Sihsalus distro.

## Distro Versions

- Distro parent version: `3.7.0-SNAPSHOT`
- OpenMRS webapp/core runtime: `2.8.6`
- Initializer: `2.11.0`
- FHIR2: `4.0.0-SNAPSHOT`
- Webservices REST: `3.4.1`
- Authentication: `2.3.0`
- OAuth2 Login: `1.5.0`
- EMR API: `3.4.0`
- O3 Forms: `2.3.0`
- Queue: `3.0.0`
- Appointments: `2.1.0-20250318.070530-1`
- Teleconsultation: `2.1.0-20250318.154145-1`
- Bed Management: `7.2.0`
- Stock Management: `3.0.0`
- Billing: `2.2.0`
- FUA: `1.0.75`
- Imaging: `1.2.2`
- Sihsalus content package: `1.8.30`

The full machine-readable list is stored in `config/baseline/sihsalus-distro.properties`.

## Java Implication

The distro backend still contains Java 8-era build configuration for its distro build tools (`source=1.8`, `target=1.8`). OpenMRS Core `master` has moved to Java 21.

Therefore:

- Sihsalus Core will keep Java 21 as the modernization target for new code.
- Compatibility imports from the current distro must be evaluated against OpenMRS `2.8.6` first.
- A Java 21 migration is not assumed safe for every existing module until each module is compiled and smoke-tested.

## Import Rule

Every imported backend module or concern must record:

- source repository
- source branch/tag/commit
- version from the Sihsalus distro
- Java compatibility status
- OpenMRS API compatibility status
- runtime smoke test result
- known Sihsalus patches
