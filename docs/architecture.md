# Architecture Direction

Sihsalus Core will be built as a compatible backend foundation for the Sihsalus distribution.

The first architectural decision is to avoid a broad fork dump. Code enters this repository only when it has an owner, a compatibility boundary, and a verification path.

## Layers

1. Platform compatibility

   OpenMRS runtime contracts remain stable unless a migration plan exists. This includes module identifiers, REST paths, database expectations, extension configuration, and authentication/session behavior consumed by the existing frontend.

2. Sihsalus backend extensions

   Sihsalus-specific services and modules should live behind clear package/module boundaries. New Java code should use `org.sihsalus.*` unless it is intentionally preserving an OpenMRS extension point.

3. Operations

   Local development, CI, container builds, secrets, backups, and observability are first-class concerns. A module is not production-ready just because it compiles.

## Non-Goals For Phase 0

- rename OpenMRS core packages
- import all backend code
- rewrite authentication
- change database schema ownership
- replace the OpenMRS module system

## Compatibility Rule

Branding can be Sihsalus. Technical contracts remain OpenMRS-compatible until explicitly migrated.

