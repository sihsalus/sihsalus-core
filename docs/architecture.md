# Architecture Direction

Sihsalus Core will be built as a compatible backend foundation for the Sihsalus distribution.

The first architectural decision is to avoid a broad fork dump. Code enters this repository only when it has an owner, a compatibility boundary, and a verification path.

## Layers

1. Platform compatibility

   OpenMRS data and API contracts remain useful migration references. Package names and database assumptions from imported code should remain stable until an explicit migration exists.

2. Static modular monolith

   SIH Salus modules are Maven reactor modules loaded by normal Spring/application composition. Runtime module install, unload, refresh, `.omod` packaging, and dynamic discovery are not part of the target runtime.

3. Sihsalus backend extensions

   Sihsalus-specific services and modules should live behind clear package/module boundaries. New Java code should use `org.sihsalus.*` unless it is intentionally preserving an OpenMRS extension point.

4. Operations

   Local development, CI, container builds, secrets, backups, and observability are first-class concerns. A module is not production-ready just because it compiles.

## Non-Goals For The Initial Skeleton

- rename OpenMRS core packages
- complete all backend behavior
- rewrite authentication
- finish database schema ownership decisions

## Compatibility Rule

Branding can be Sihsalus. Technical contracts remain OpenMRS-compatible until explicitly migrated.
