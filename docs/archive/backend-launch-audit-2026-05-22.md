# SIH Salus Backend Launch Audit

Date: 2026-05-22
Repository: `/home/alvax/omrs/sihsalus-core`
Scope: backend monorepo, OpenMRS compatibility surfaces, Spring Boot runtime, REST/FHIR APIs, persistence, migrations, deployment defaults, security, privacy, reliability, and supply chain.

## A. Executive Risk Summary

Recommendation: **No-go for production**. A critical anonymous patient-data exposure was confirmed and fixed in this pass, but production readiness still requires a clinical authorization model beyond coarse OpenMRS privileges, consent/emergency-access policy, tamper-resistant audit events, backup/restore proof, SBOM/vulnerability gates, and failure-mode/load testing.

Top launch blockers:

| Blocker | Evidence | State |
| --- | --- | --- |
| Anonymous REST/FHIR patient read path existed after fork | Old boot smoke test read `/rest/v1/patient/{uuid}` and `/api/fhir/r4/Patient/{uuid}` with no credentials; `PatientService#getPatientByUuid` has `@Authorized(GET_PATIENTS)` only on the interface at `sihsalus-core-api/src/main/java/org/openmrs/api/PatientService.java:109`, while the implementation only had `@Transactional` at `sihsalus-core-api/src/main/java/org/openmrs/api/impl/PatientServiceImpl.java:1207`. | **Fixed and retested** |
| No BOLA/consent layer for patient-scoped clinical data | REST/FHIR now requires authentication, but the implemented control is still privilege based, not patient/encounter/provider/team/consent scoped. | Open |
| Audit model is not sufficient for patient-data access, emergency access, impersonation, or admin actions | Envers tables are generated at `sihsalus-core-api/src/main/java/org/openmrs/api/db/hibernate/HibernateSessionFactoryBean.java:216`, but read access and `becomeUser` are not AuditEvent-style events; impersonation logs only debug at `sihsalus-core-api/src/main/java/org/openmrs/api/context/UserContext.java:161`. | Open |
| Deployment defaults are unsafe for real environments | Default DB username/password in `sihsalus-core-boot/src/main/resources/application.yml:5` and `compose.yml:5`; no backup, TLS, secrets manager, or restore validation. | Open |
| Supply-chain/release governance is incomplete | Root POM imports `org.openmrs:openmrs-bom:3.0.0-SNAPSHOT` at `pom.xml:88`; CI only runs `mvn verify` at `.github/workflows/ci.yml:27`; no SBOM/signature/vulnerability gate found. | Open |

Top patient-safety/privacy risks: broad privilege grants expose all patients, file attachments default to empty allow-list (`sihsalus-module-attachments/src/main/resources/org/openmrs/module/attachments/liquibase.xml:50`), OCL subscription URL/token are persisted in global properties and outbound requests follow redirects with token headers (`sihsalus-module-openconceptlab/src/main/java/org/openmrs/module/openconceptlab/client/OclClient.java:67` and `:406`), purge APIs still physically delete clinical rows and complex data (`sihsalus-core-api/src/main/java/org/openmrs/api/impl/ObsServiceImpl.java:316`, `PatientServiceImpl.java:347`).

## B. Risk Register

| ID | Severity | Confidence | Category | Affected components | Evidence | Exploit/failure scenario | Impact | Recommended fix | Compatibility/migration impact | Test/retest plan | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SIH-BE-001 | Critical | Confirmed | Security, Healthcare Privacy | REST `/rest/v1/patient/*`, FHIR `/api/fhir/r4/Patient/*`, service AOP | `PatientService#getPatientByUuid` secured at interface `PatientService.java:109`; impl lacks `@Authorized` at `PatientServiceImpl.java:1207`; before fix boot smoke read patient anonymously. | Internet or internal user reads patient demographics and identifiers by UUID without login. | PHI breach, regulatory failure, unsafe launch. | Wire OpenMRS AOP into Boot, resolve interface `@Authorized`, protect REST/FHIR paths, map FHIR auth failures to 401/403. | Backward compatible with OpenMRS privilege contract; anonymous clinical reads now fail. | Added boot tests for anonymous 401 and authenticated 200. | **Fixed** |
| SIH-BE-002 | Critical | Confirmed | Architecture, Security, Compatibility | Static OpenMRS module migration | Boot scan omitted `org.openmrs.aop` before fix; FHIR config excluded `org.openmrs.module.fhir2.spring.*` and did not import FHIR AOP. | Forked static-module runtime silently drops OpenMRS security extension points. | Any service relying on inherited annotations can become public. | Treat AOP/security configs as compatibility surfaces and assert them in boot tests. | Existing interfaces preserved; AOP bean renamed to avoid Spring transaction bean collision. | `openmrsServiceAuthorizationIsEnforcedForInterfaceAnnotations` passes. | **Fixed for core/FHIR read path; broader module audit remains** |
| SIH-BE-003 | High | Likely | Security | All REST/FHIR/admin APIs | No `spring-security`, `spring-boot-starter-security`, or `SecurityFilterChain` found by repository search; current REST `AuthorizationFilter` says it does not fail missing/invalid credentials at `AuthorizationFilter.java:33`. | Brute force, inconsistent auth, no centralized session/token policy, no standard CSRF/CORS/header controls. | Account compromise and inconsistent API behavior. | Add Spring Security resource server/session strategy, hard fail invalid credentials, rate limiting, secure headers, CORS policy, auth tests per API family. | Requires compatibility shim for legacy OpenMRS Basic auth and sessions. | ASVS auth suite plus negative tests for missing/bad credentials and rate limits. | Open |
| SIH-BE-004 | High | Confirmed | Healthcare Privacy, Security | Clinical resources, FHIR, REST | Current enforced control is privilege based; no patient/encounter/provider-team/consent decision point found in patient/FHIR read flow. | User with `Get Patients` reads any patient, including restricted charts, VIP, mental health, or non-assigned care team patients. | BOLA and consent breach. | Add patient-scope access decision service used by REST/FHIR/service methods; include break-glass workflow. | New policy must be configurable and initially audit-only to avoid breaking existing deployments. | Two-patient BOLA tests, consent-denied tests, emergency-access tests. | Open |
| SIH-BE-005 | High | Confirmed | Security, Ops | Secrets/deployment | Defaults `sihsalus/sihsalus` in `application.yml:5` and `compose.yml:5`; S3 static keys supported in properties at `S3StorageService.java:88`. | Deployed defaults or leaked config allow DB/S3 access. | Full PHI compromise. | Fail startup in non-dev profile if defaults are used; integrate secrets manager; document rotation. | Requires profile/config migration. | Startup tests for prod profile with defaults rejected. | Open |
| SIH-BE-006 | High | Likely | Healthcare Privacy, Auditability | Audit/provenance, impersonation | `becomeUser` only checks superuser/daemon and logs debug at `UserContext.java:161`; Envers is mutation revisioning, not patient-access AuditEvent. | Admin impersonates clinician or reads patient data without tamper-resistant record. | Non-repudiation failure, privacy incident response impossible. | Add append-only AuditEvent store for read/write/export/search/login/impersonation/break-glass, signed/hash chained or WORM-forwarded. | New tables/events; no API break if async. | Audit event tests for patient read, FHIR read, export, impersonation, failed auth. | Open |
| SIH-BE-007 | High | Confirmed | Migration, Reliability | Liquibase | Master changelog aggregates core plus many module changelogs at `db.changelog-master.xml:8`; test startup applied 1,653 changesets and marked some preconditions as ran. | Hospital upgrade partially applies unrelated module schema or silently marks changesets despite unmet objects. | Upgrade outage or data drift. | Version module migrations, split install vs upgrade, validate PostgreSQL dry run, forbid future/unreleased changesets in release branch. | Requires migration governance and release notes. | Clean PostgreSQL install, OpenMRS upgrade fixture, rollback/diff checks. | Open |
| SIH-BE-008 | Medium | Confirmed | Performance, Reliability | Hibernate/Search | Runtime warnings: HBM XML deprecated; Hibernate Search uses `LATEST`; multiple getter ambiguity for `Patient.dead`, `PersonName.voided`, `Patient.personVoided`; Java 26 Lucene native warnings. | Search index field binding changes between starts or future Java/Hibernate upgrades fail. | Incorrect patient search, startup fragility. | Pin Lucene version, set explicit field access, migrate HBM/XML mappings incrementally, run search regression tests. | Mapping migration needs compatibility review. | Patient/concept search tests across restart and Java 21. | Open |
| SIH-BE-009 | Medium | Confirmed | Security, File Upload | Attachments/storage | Attachment privileges and defaults at `attachments/liquibase.xml:14`; `attachments.allowedFileExtensions` defaults empty at `:50`; storage key appends sanitized filename only removing file separator at `BaseStorageService.java:84`; local path containment exists at `LocalStorageService.java:142`. | Unrestricted file type upload, malware storage, PHI exfil via attachment endpoints. | Malware and PHI exposure. | Require explicit allow-list, MIME sniffing, AV scanning, quarantine, per-patient BOLA, object encryption. | Defaults change behavior; migration must set safe defaults per deployment. | Upload deny/allow tests, AV stub tests, path traversal tests. | Open |
| SIH-BE-010 | High | Likely | Security, Integration | Open Concept Lab client | Subscription URL/token stored in global properties at `ImportServiceImpl.java:355` and `:421`; URL host is rewritten but not allow-listed at `:467`; client follows redirects at `OclClient.java:67`; token header added at `:406`. | Admin-controlled or compromised subscription URL redirects token or SSRFs internal services. | Token leakage, internal network scanning, poisoned terminology imports. | Restrict scheme/host allow-list, disallow redirects across host, secret storage, import provenance/signature checks. | May reject existing custom OCL endpoints without migration allow-list. | SSRF/redirect tests and signed import fixture. | Open |
| SIH-BE-011 | High | Needs Validation | Reliability, Ops | Backups/failure modes | Only compose Postgres service found; no backup/restore/failure runbooks in scanned deploy files. | DB/PACS/FHIR/Keycloak outage causes unrecoverable downtime or silent data loss. | Patient-care disruption. | Define RPO/RTO, PITR, restore drills, runbooks, synthetic checks, failure tests. | Operational, no API break. | Quarterly restore drill evidence and chaos tests. | Open |
| SIH-BE-012 | Medium | Confirmed | Supply Chain | Maven/CI | SNAPSHOT BOM at `pom.xml:88`; CI only `mvn verify` at `.github/workflows/ci.yml:27`; no SBOM/vuln/signing workflow found. | Dependency changes after test, vulnerable component enters release. | Compromise through dependencies. | Pin release BOM, generate CycloneDX SBOM, run OWASP Dependency-Check/Snyk/OSV, verify artifacts, cache policy. | Release process change. | CI must fail on critical vulns or unsigned unexpected artifacts. | Open |

## C. Architecture Assessment

Current architecture map:

| Area | Evidence |
| --- | --- |
| Maven monorepo | Root reactor modules at `pom.xml:16`, including `sihsalus-core-api`, `sihsalus-core-liquibase`, `sihsalus-fhir2`, `sihsalus-webservices-rest`, module jars, and `sihsalus-core-boot`. |
| Runtime entry point | Spring Boot scans `org.sihsalus`; OpenMRS static core config imports OpenMRS app context/cache and scans legacy packages at `SihsalusOpenmrsStaticCoreConfiguration.java:59`. |
| Persistence | Hibernate session factory uses `hibernate.cfg.xml`, scans `org.openmrs`, and module mapping contributors at `SihsalusOpenmrsStaticCoreConfiguration.java:102`. |
| Migrations | Boot and Spring Liquibase use `db.changelog-master.xml` at `application.yml:8` and `SihsalusOpenmrsStaticCoreConfiguration.java:94`. |
| Public/admin APIs | REST controllers under `/rest/v1`, system info under `/api/system/info`, FHIR metadata/read under `/api/fhir` and `/ws/fhir2`. |
| Compatibility surfaces | Legacy `org.openmrs.*` packages are retained; REST resources are statically registered; FHIR providers are imported with OpenMRS FHIR AOP. |

Target architecture proposal:

| Domain | Target |
| --- | --- |
| Module boundaries | `sihsalus-core-api` owns clinical domain/service contracts; module jars provide bounded capabilities; Boot only wires runtime; no module should bypass service authorization through DAO access. |
| API boundary | All external APIs pass through a single Spring Security chain plus legacy compatibility filters. Clinical REST/FHIR controllers call service-layer policy checks, not DAOs directly. |
| Authorization | Two-layer model: coarse privilege check plus patient-scope decision service for BOLA/consent/team/location/emergency access. |
| Audit/provenance | Append-only AuditEvent model for login, read, search, export, write, void, purge, merge, impersonation, emergency access, and integration syncs. |
| Persistence/migration | Release-scoped Liquibase with module contexts, PostgreSQL dry-run validation, index review, and explicit rollback notes. |
| Observability | Metrics for auth failures, patient reads, FHIR latency, DB pool, Liquibase duration, job failures, PACS/FHIR/OCL availability, backup status. |

Migration strategy:

1. Keep OpenMRS package/API compatibility while adding SIH Salus policy adapters.
2. Introduce new security/audit tables and run them alongside existing metadata/Envers.
3. Add audit-only patient-scope authorization first, compare logs, then enforce per deployment.
4. Split module migrations by release and require clean PostgreSQL install plus upgrade-fixture validation.

## D. Security Assessment

ASVS/SCVS control review:

| Control area | Current state | Gap |
| --- | --- | --- |
| Authentication | Legacy Basic/session auth filter now wired for REST/FHIR protected paths. | No Spring Security chain, token validation, rate limiting, lockout policy, uniform invalid-credential behavior. |
| Authorization | OpenMRS AOP and FHIR AOP now wired; interface annotations resolved. | No patient-object authorization or consent model. |
| Session/token handling | Legacy `Context` sessions used; REST filter does not hard fail invalid credentials. | Need centralized session/token policy and secure headers. |
| Secrets | Env overrides exist but defaults are weak. | Need prod default rejection and secrets manager. |
| Injection/deserialization | HQL queries mostly parameterized in reviewed OCL paths; XStream has whitelist and auth requirement. | XStream remains high-risk and must be fuzzed/allow-list audited; reporting SQL/Groovy evaluators need separate review. |
| SSRF/file upload | OCL accepts configured URL/token; storage has path containment. | Need OCL host allow-list/redirect controls and attachment MIME/AV controls. |
| Supply chain | Maven CI exists. | No SBOM/vulnerability/signature gate; SNAPSHOT BOM. |

BOLA review: unauthenticated patient reads are fixed, but BOLA is still not satisfied. `GET_PATIENTS` permits all-patient access unless a higher-level UI or deployment policy restricts it. Before production, patient reads, encounter reads, obs/orders, attachments, imaging studies, reports, and FHIR resources must all call a patient-scope access policy.

## E. Performance and Reliability Assessment

Observed risks:

| Area | Evidence | Risk |
| --- | --- | --- |
| Hibernate mappings | Runtime HBM XML deprecation warnings across OpenMRS mappings. | Future Hibernate upgrade fragility. |
| Search | Runtime warned that Hibernate Search uses `LATEST` Lucene and ambiguous getters. | Search field instability and Java upgrade risk. |
| Migrations | 1,653 changesets applied in boot test startup. | Slow startup/upgrade and poor module release isolation. |
| Thread pools | REST uses fixed 5-thread executor at `SystemRestConfiguration.java:53`. | Backpressure and tuning not tied to workload/SLO. |
| Packaging | Reactor packaging passes, but with many deprecation/unsafe warnings. | Technical debt for Java/Hibernate upgrade path. |
| Failure modes | No validated DB/Keycloak/FHIR/PACS/OCL outage tests found. | Unknown hospital downtime behavior. |

Recommendations: add Gatling/k6 clinical flows, DB pool/load metrics, explain-plan review for patient dashboard/FHIR searches, migration timing budgets, index diff review, synthetic dependency checks, and chaos tests for DB/OCL/PACS/FHIR outages.

## F. Healthcare Privacy and Interoperability Assessment

| Area | Assessment |
| --- | --- |
| FHIR exposure | CapabilityStatement is public; read endpoints now require auth for protected resources. Need `_search`, history, include/revinclude, and bulk/export rules before enabling. |
| Consent/access control | No confirmed consent model or patient-scope PDP. Must be implemented before production. |
| Provenance/audit | Envers and OpenMRS audit columns cover mutations, not read/access/export provenance. Need AuditEvent-like store. |
| Patient merge | Merge logs exist and move clinical objects in `PatientServiceImpl.java:558`; needs regression suite for obs/orders/visits/programs and audit events. |
| Deletion/voiding | Voiding semantics exist, but purge APIs can physically delete patient/obs data. Production must restrict purge to break-glass admin workflows with dual control and immutable audit. |
| Emergency access | No confirmed break-glass flow. Must add reason capture, temporary scope, notification, and review queue. |

## G. Implemented Changes

| Files changed | Risk addressed | Design and compatibility notes | Tests/retest |
| --- | --- | --- | --- |
| `sihsalus-core-boot/src/main/java/org/sihsalus/core/boot/openmrs/SihsalusOpenmrsStaticCoreConfiguration.java` | OpenMRS service AOP not wired in Boot. | Added `org.openmrs.aop` to static core scan to preserve inherited OpenMRS service contracts. | Full boot smoke passes. |
| `sihsalus-core-api/src/main/java/org/openmrs/aop/AuthorizationAdvice.java` | Interface-level `@Authorized` ignored for proxied implementation methods. | Resolves annotations from implemented interfaces when concrete method lacks annotation. | Added `openmrsServiceAuthorizationIsEnforcedForInterfaceAnnotations`. |
| `sihsalus-core-api/src/main/java/org/openmrs/aop/AOPConfig.java` | Bean collision between OpenMRS compatibility `transactionAttributeSource` and Spring transaction infrastructure. | Renamed compatibility bean to `openmrsTransactionAttributeSource`; standard Spring bean remains available under canonical name. | Boot context starts and full smoke passes. |
| `sihsalus-fhir2/src/main/java/org/sihsalus/fhir2/Fhir2Configuration.java` | FHIR AOP excluded by component scan. | Imported `FhirAopConfiguration` explicitly while preserving scan exclusion. | FHIR authenticated 404 and patient read tests pass. |
| `sihsalus-fhir2/src/main/java/org/sihsalus/fhir2/FhirR4ReadController.java` | FHIR auth failures did not map to FHIR OperationOutcome. | Converts `APIAuthenticationException` to 401/403 OperationOutcome. | Anonymous FHIR Patient read gets 401 OperationOutcome. |
| `sihsalus-core-boot/src/main/java/org/sihsalus/core/boot/openmrs/OpenmrsContextSessionFilter.java` | Filter order needed deterministic OpenMRS session before auth. | Ordered context session filter before API auth wrapper. | REST/FHIR MockMvc tests pass. |
| `sihsalus-webservices-rest/src/main/java/org/sihsalus/webservices/rest/SystemRestConfiguration.java` | REST/FHIR API auth filter was not registered in Boot static runtime. | Added ordered wrapper delegating to OpenMRS `AuthorizationFilter` for REST/FHIR paths only. | Anonymous REST Patient read gets 401; authenticated read gets 200. |
| `sihsalus-core-boot/src/test/java/org/sihsalus/core/boot/SihsalusCoreApplicationTest.java` | Missing regression coverage for anonymous patient read and service AOP. | Updated patient REST/FHIR test to require auth, added service authorization test, authenticated wiring tests, and H2-only compatibility shim. | 27 boot smoke tests pass. |

Commands run:

| Command | Result |
| --- | --- |
| `mvn -pl sihsalus-core-boot -am -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SihsalusCoreApplicationTest#patientRegistryPatientIsReadableThroughRestAndFhir test` | Baseline confirmed anonymous patient REST/FHIR read behavior before fix. |
| `mvn -pl sihsalus-core-boot -am -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SihsalusCoreApplicationTest#patientRegistryPatientRequiresAuthenticationThroughRestAndFhir+openmrsServiceAuthorizationIsEnforcedForInterfaceAnnotations test` | Passed after fixes. |
| `mvn -pl sihsalus-core-boot -am -Denforcer.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SihsalusCoreApplicationTest test` | Passed: 27 tests, 0 failures/errors. |
| `mvn -Denforcer.skip=true -DskipTests verify` | Passed with tests skipped; first sandbox attempt failed because `~/.m2` was read-only, rerun approved. |

Verification limitations: local machine has Java 26, while the project enforces Java 21 (`pom.xml:187`) and CI uses Java 21 (`.github/workflows/ci.yml:20`). Commands above used `-Denforcer.skip=true`. A final Java 21 CI run is required before merge/release.

## H. Final Launch Checklist

Must fix before production:

| Item | Exit criterion |
| --- | --- |
| Patient-scope BOLA/consent | Deny cross-patient access in REST/FHIR/service tests without assignment/consent/break-glass. |
| Spring Security/API gateway | Central auth chain, token/session policy, rate limiting, secure headers, invalid credential failure. |
| AuditEvent/provenance | Immutable read/write/export/search/impersonation/break-glass audit with alerting. |
| Secrets/deployment | No default secrets in prod, TLS, secret rotation, S3/DB least privilege. |
| Backup/restore | Documented PITR and successful restore drill with RPO/RTO evidence. |
| Supply chain | Release BOM, SBOM, vulnerability gate, dependency provenance. |
| Migration gates | Clean PostgreSQL install, upgrade fixture, rollback notes, index review, module migration contexts. |

Should fix before pilot:

| Item | Exit criterion |
| --- | --- |
| Attachment/imaging hardening | MIME allow-list, AV scan, object encryption, per-patient authorization tests. |
| OCL integration hardening | URL allow-list, no cross-host token redirects, signed/provenance import checks. |
| Hibernate/Search cleanup | Pin Lucene version and remove ambiguous getter warnings. |
| Load/failure tests | Patient dashboard, FHIR read/search, order/obs, attachment, billing workflows under realistic concurrency. |

Can fix post-launch only if risk-accepted:

| Item | Condition |
| --- | --- |
| HBM XML migration | Accept only with pinned Hibernate version and search regression tests. |
| Legacy XStream retirement | Keep only if whitelist is reviewed and serializer endpoints are not externally exposed. |
| Deprecated Java API cleanup | Accept only with Java 21 LTS support window and upgrade backlog. |

Concrete backend state:

| State | Result |
| --- | --- |
| Verified | Repo structure, Maven reactor, Boot runtime wiring, REST/FHIR patient read behavior, OpenMRS service AOP enforcement, Liquibase startup path, package build with tests skipped. |
| Fixed | Anonymous patient read through REST/FHIR blocked; interface `@Authorized` enforcement restored; FHIR auth errors return OperationOutcome; Boot smoke tests updated. |
| Still risky | Object-level authorization, consent, audit events, secrets/deployment, migrations, supply chain, backup/restore, failure testing, attachment/OCL hardening. |
| Production decision | **No-go** until must-fix checklist is complete and independently retested on Java 21/PostgreSQL with realistic clinical workflows. |
