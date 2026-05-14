# Java Modernization Baseline

## Decision

New Sihsalus Core work targets Java 21. Existing distro-compatible imports are evaluated against the current Sihsalus distro first.

## Rationale

The current `openmrs-core` development branch declares:

- project version: `3.0.0-SNAPSHOT`
- `maven.compiler.release=21`
- minimum Maven version: `3.8.0`
- Jakarta Servlet API: `6.1.0`

GitHub reports the latest OpenMRS Core release as `2.7.6`, published on 2025-09-12. The Sihsalus distro currently uses OpenMRS `2.8.6`, so the distro baseline takes precedence for import compatibility.

Sihsalus Core will use Java 21 for new code because it aligns with current OpenMRS core development and gives a long-term runtime target. That does not mean every existing distro module is assumed Java 21-ready.

## Rules

- Use Java 21 for new modules and services.
- Use the Sihsalus distro versions as the initial compatibility target.
- Do not introduce Java APIs beyond the configured Maven release.
- Prefer Jakarta namespace compatibility when importing newer OpenMRS code.
- Keep compatibility notes when importing code from OpenMRS 2.7.x or older module branches.
- Avoid modernization rewrites during import; first make behavior reproducible, then modernize.

## Verification

The parent `pom.xml` enforces:

- Java `[21,22)`
- Maven `3.9.6+`
- UTF-8 source and reporting encodings
- Maven compiler release 21
