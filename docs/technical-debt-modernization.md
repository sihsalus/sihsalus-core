# Technical Debt Modernization Plan

This document defines stabilization-era modernization targets for Sihsalus Core. The static module migration is complete, so technical-debt work should now reduce release risk in the migrated runtime. The goal is not to rewrite OpenMRS code wholesale. The goal is to contain inherited risk, make behavior testable, and modernize the parts that Sihsalus owns or actively changes.

## Priorities

Stabilization work should favor narrow, verifiable fixes over broad mechanical cleanup. A patch should usually map to a CodeQL warning, CI failure, PostgreSQL smoke gap, security hardening item, or known runtime failure mode.

1. Null and resource safety

   Fix nullable dereferences, nullable Boolean unboxing, unsafe reflection arguments, and manual resource cleanup before larger refactors. Prefer guard clauses, `Boolean.TRUE.equals(...)`, `try-with-resources`, and clear failure paths in batch jobs.

2. Mutable internal state

   Do not expose mutable DTO collections directly. Prefer unmodifiable views, defensive copies, and explicit methods such as `addX`, `removeX`, `clearX`, or `putX`.

   Be careful with OpenMRS/Hibernate entities: many existing getters intentionally expose live collections for ORM compatibility. Do not make those immutable without checking persistence behavior and callers.

3. Report and batch-job structure

   Stock/reporting jobs should be broken into small steps: restore state, prepare filters, open staging files, process one buffer, write output, persist progress, and finish/fail the job. Avoid long methods that mix paging, file I/O, CSV writing, service lookups, and cancellation handling.

4. Dynamic string contracts

   Reduce stringly typed code around property names, reflection-style message methods, global properties, report keys, and module identifiers. Use constants, enums, and small adapter methods where compatibility allows.

5. Deprecated and unsafe Java APIs

   Replace `Class#newInstance`, raw types, unchecked casts, deprecated Hibernate calls, and broad `catch (Exception)` blocks when touching nearby code. Do not perform mechanical rewrites across all imported code without tests.

6. Module boundaries

   Sihsalus-specific code should live behind `org.sihsalus.*` packages unless it intentionally preserves an OpenMRS extension point. Imported `org.openmrs.*` code should be treated as compatibility code and changed conservatively.

## Antipatterns To Remove

- `InputStream in = null` plus `finally { in.close(); }`
- `catch (Exception e) { /* pass */ }` without a documented reason
- public getters that return mutable DTO `List`, `Set`, or `Map` fields
- service and report code that mutates ORM entities from many unrelated layers
- nullable `Boolean` values used as primitives
- arrays and method arguments indexed before length/null checks
- silent fallback after failed file, zip, or metadata reads
- report jobs with shared mutable fields for per-run state
- duplicated CodeQL fixes without a module-level pattern

## Do Not Modernize Yet

- Do not rename OpenMRS packages, database tables, global properties, REST paths, FHIR paths, or module identifiers without a migration plan.
- Do not convert ORM entity collection getters to immutable views unless all persistence and caller behavior has been audited.
- Do not upgrade Spring, Hibernate, HAPI FHIR, or OpenMRS baselines as a side effect of small quality fixes.
- Do not add new abstraction layers just to hide imported OpenMRS code. Add boundaries only where Sihsalus behavior is genuinely owned.

## First Modules To Clean

- `sihsalus-module-stockmanagement`: import jobs, CSV reports, paging state, permission checks.
- `sihsalus-module-reporting`: query builders, report utilities, evaluator null handling, SQL safety.
- `sihsalus-fhir2`: DAO sorting/search helpers and null-safe query construction.
- `sihsalus-module-appointments`: mapper mutation boundaries and DTO collection exposure.
- `sihsalus-module-billing`: receipt generation, file output, and reflection-based extension points.

## Verification Standard

For each modernization patch:

- compile the affected module with `-am`
- add or update tests when behavior changes
- keep CodeQL fixes local to the warning pattern
- document intentional compatibility exceptions in code comments or this document

Example:

```bash
mvn -pl modules/stockmanagement -am -DskipTests compile
mvn -pl modules/reporting -am -DskipTests compile
```
