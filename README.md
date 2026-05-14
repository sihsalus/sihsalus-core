# Sihsalus Core

Sihsalus Core is the backend foundation for the Sihsalus distribution. It starts from OpenMRS compatibility and evolves incrementally toward a maintained, secure, production-ready core.

This repository is intentionally small at the beginning. Code is brought in only when its ownership, compatibility impact, test strategy, and security posture are clear.

## Current Phase

Phase 0: repository foundation and technical baseline.

Goals:

- document the architecture direction before importing code
- keep OpenMRS runtime contracts compatible
- define the Java modernization baseline
- establish security and migration rules
- prepare CI and deployment structure incrementally

## Compatibility Rule

Do not rename OpenMRS technical contracts casually. Package names, module identifiers, extension points, API paths, event names, database schema assumptions, and runtime configuration keys must remain compatible unless a migration plan exists.

Sihsalus branding and product experience can evolve independently from OpenMRS compatibility.

## Distro Baseline

The initial compatibility baseline is the current Sihsalus distro, not OpenMRS Core `master`.

Current distro reference:

- repository: `sihsalus/sihsalus`
- distro parent: `3.7.0-SNAPSHOT`
- OpenMRS runtime: `2.8.6`
- module baseline: see `baseline/sihsalus-distro.properties`

## Java Baseline

Sihsalus Core targets Java 21 for new backend work and modernization work.

This matches the current OpenMRS core development branch, which uses `maven.compiler.release=21`. The current Sihsalus distro is still based on OpenMRS `2.8.6`, so imported code must be validated against the distro baseline before being modernized.

## Repository Layout

```text
baseline/   Version pins from the current Sihsalus distro
docs/       Architecture, migration, and security decisions
ops/        Deployment and operations notes
scripts/    Local automation scripts
```

## First Milestone

The first milestone is not a fork dump. It is a runnable minimal core profile with documented compatibility boundaries, CI, and a clear module import process.
