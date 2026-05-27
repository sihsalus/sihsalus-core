# Architecture Direction

Sihsalus Core will be built as a compatible backend foundation for the Sihsalus distribution.

The static module migration is complete. The current architectural decision is to keep the OpenMRS data model as the canonical core model while stabilizing the `.omod`-free runtime. New changes should improve reliability, security, observability, test coverage, or explicit Sihsalus product behavior.

## Layers

1. Platform compatibility

   OpenMRS data model, tables, Liquibase history, entities, and service semantics are preserved as the core compatibility base. Package names and database assumptions from imported code should remain stable until an explicit migration exists.

2. Static modular monolith

   SIH Salus modules are Maven reactor modules loaded by normal Spring/application composition. Runtime module install, unload, refresh, `.omod` packaging, and dynamic discovery are not part of the target runtime.

   FHIR and REST are separate adapters and contracts. They share the same OpenMRS/SIH Salus domain model and services; they should not be merged into a common transport DTO layer.

   Current imported API adapters:

   - `sihsalus-fhir2` contains the upstream FHIR2 API package as local source under `org.openmrs.module.fhir2.*`, adapted to the local Jakarta/Hibernate baseline and compiled without the FHIR OMOD activator. A Spring MVC R4 read adapter exposes imported providers at `/api/fhir/r4/{resourceType}/{id}` and `/ws/fhir2/R4/{resourceType}/{id}` while full servlet parity remains a later compatibility decision.
   - `sihsalus-webservices-rest` contains the upstream Web Services REST common package as local source under `org.openmrs.module.webservices.*`, compiled without module install/start/stop wrappers or dynamic module enumeration. The REST v1 controller and core REST services are statically registered so concrete resources can be imported without the OMOD runtime.
   - `sihsalus-module-authentication` contains the upstream Authentication API package as local source under `org.openmrs.module.authentication.*`, currently sourced from `authentication-api` `2.4.0-SNAPSHOT` while the distro dependency baseline lists `authentication-omod` `2.3.0`. It wires the OpenMRS `AuthenticationScheme` override and user session listener through static Spring configuration; legacy servlet filters and OMOD activator are not runtime entrypoints.
   - `sihsalus-module-oauth2login` contains the upstream OAuth2 Login API package as local source under `org.openmrs.module.oauth2login.*`, currently sourced from `oauth2login-api` `1.6.0-SNAPSHOT` while the distro dependency baseline lists `oauth2login-omod` `1.5.0`. It registers the OAuth2 user-info authentication scheme through static Spring wiring without the OMOD activator, `ModuleFactory`, or daemon-token lifecycle. Legacy OAuth2 web filters/controllers remain outside the runtime entrypoint for this cut.
   - `sihsalus-module-idgen` contains the upstream ID Generation API package as local source under `org.openmrs.module.idgen.*`, currently sourced from idgen `6.0.0-SNAPSHOT` even though the distro dependency baseline lists `idgen-omod` `5.0.4`. It is wired as a static internal module, with Java Spring configuration, centralized Liquibase, and no OMOD activator.
   - `sihsalus-module-addresshierarchy` contains the upstream Address Hierarchy API package as local source under `org.openmrs.module.addresshierarchy.*`, currently sourced from `addresshierarchy-api` `3.0.0-SNAPSHOT` while the distro dependency baseline lists `addresshierarchy-omod` `2.21.0`. It contributes Hibernate mappings, Liquibase, DAO/service wiring, cache listener, and optional cache scheduling through static Spring configuration, without the OMOD activator or `ModuleFactory`.
   - `sihsalus-core-boot` wires OpenMRS services, DAOs, Hibernate mappings, storage, cache, request `Context` sessions, and Liquibase through static Spring configuration. Hibernate mapping discovery no longer asks the OpenMRS module runtime for started modules.

   The runtime baseline is Spring Boot 4 plus Spring Framework 7 and Hibernate 7, aligned with the imported OpenMRS `master` source. Boot logging uses Log4j2 to avoid mixing the OpenMRS Log4j binding with Boot's default Logback bridge.

3. Sihsalus backend extensions

   Sihsalus-specific services and modules should live behind clear package/module boundaries. New Java code should use `org.sihsalus.*` unless it is intentionally preserving an OpenMRS extension point.

4. Operations

   Local development, CI, container builds, secrets, backups, and observability are first-class concerns. A module is not production-ready just because it compiles.

## Stabilization Focus

- keep the static runtime free from `.omod` lifecycle dependencies
- verify module services, REST/FHIR adapters, Liquibase order, and PostgreSQL behavior with repeatable checks
- reduce inherited reliability/security debt where Sihsalus actively owns the code path
- document compatibility exceptions before changing OpenMRS package names, database shape, REST paths, FHIR paths, or module identifiers

## Non-Goals For Stabilization

- rename OpenMRS core packages
- rewrite inherited backend behavior without product or reliability need
- rewrite authentication
- replace the OpenMRS persistent model

## Compatibility Rule

Branding can be Sihsalus. The core database model remains OpenMRS-compatible unless an explicit, reviewed migration replaces a specific part of it.
