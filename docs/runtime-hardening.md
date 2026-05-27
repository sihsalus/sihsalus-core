# Runtime Hardening Runbook

This runbook is the stabilization checklist for runtime risk. It is intentionally
operational: every item must either have a command, a configuration value, a test,
or a named residual risk.

## Release Blockers

Do not cut a release candidate while any of these are true:

- PostgreSQL starts with an empty datasource username or password.
- The bootstrap admin account keeps the inherited default password.
- OCL static content import is enabled but errors are ignored.
- Attachment uploads have no explicit extension allow-list.
- XStream deserialization uses a raw `XStream` instance for request-controlled
  input.
- REST/FHIR endpoints expose clinical objects without an authentication or
  object-level authorization check.
- Liquibase changes have not been validated against PostgreSQL.

## Secrets

Required for containerized runtime:

```text
SIHSALUS_POSTGRES_PASSWORD
SIHSALUS_ADMIN_PASSWORD
```

Required for direct PostgreSQL runtime:

```text
SIHSALUS_DATASOURCE_URL=jdbc:postgresql://...
SIHSALUS_DATASOURCE_USERNAME
SIHSALUS_DATASOURCE_PASSWORD
SIHSALUS_ADMIN_PASSWORD
```

`OpenmrsRuntimePropertiesConfigurer` rejects PostgreSQL runtime when the datasource
username or password is blank. H2 remains allowed for tests.

Verification:

```bash
mvn --batch-mode --no-transfer-progress -pl apps/backend -am \
  -Dtest=OpenmrsRuntimePropertiesConfigurerTest test
```

## OCL Import

Default runtime behavior:

```text
SIHSALUS_OCL_STATIC_IMPORT_ENABLED=true
SIHSALUS_OCL_STATIC_IMPORT_FAIL_ON_ERRORS=true
```

Disable OCL only for infrastructure smoke tests where the goal is to verify the
container and auth surface quickly:

```bash
SIHSALUS_OCL_STATIC_IMPORT_ENABLED=false docker compose up -d backend
```

Release smoke must run once with OCL enabled and valid static content. Import
failures should stop startup instead of leaving a partially imported terminology
baseline.

Operational checks:

```sql
select property, property_value
from global_property
where property like 'sihsalus.ocl.staticImport.sha256.%'
order by property;
```

## Attachments

New installs and upgraded databases with a blank existing value are normalized to
this conservative upload extension allow-list:

```text
attachments.allowedFileExtensions=jpg,jpeg,png,pdf
```

Before production, review and set these global properties explicitly:

```text
attachments.allowedFileExtensions
attachments.deniedFileNames
attachments.maxUploadFileSize
attachments.maxStorageFileSize
attachments.allowWebcam
attachments.allowNoCaption
```

Residual runtime debt:

- antivirus or malware scanning is not wired
- quarantine/review workflow is not wired
- storage backup and restore procedure is not documented
- object-level download authorization needs workflow-specific smoke coverage

## XStream

Current expected behavior:

- serializers are deny-by-default
- configured whitelist types are the only non-primitive deserialization path
- module XStream deserialization requires an authenticated OpenMRS context
- raw `new XStream().fromXML(...)` must not be used for request-controlled input

Verification:

```bash
mvn --batch-mode --no-transfer-progress -pl core/api,modules/serialization-xstream -am \
  -Dtest=SimpleXStreamSerializerTest,XStreamSerializerTest test
```

## Auth

Existing smoke coverage proves that key REST/FHIR routes reject unauthenticated
requests and that the admin session can authenticate through the static runtime.

Minimum local check:

```bash
docker compose up -d --build backend
curl -i http://localhost:8080/rest/v1/patient?q=test
curl -i http://localhost:8080/api/fhir/r4/Patient/example
curl -i -u admin:"$SIHSALUS_ADMIN_PASSWORD" http://localhost:8080/rest/v1/session
```

Residual runtime debt:

- object-level access tests for patient-scoped REST/FHIR resources
- attachment download authorization tests
- OCL admin action authorization tests
- stock/reporting write-path authorization tests

## PostgreSQL Smoke

Use H2 tests for fast wiring feedback. Use PostgreSQL smoke for release confidence.

```bash
export SIHSALUS_POSTGRES_PASSWORD='<local-db-password>'
docker compose up -d postgres

SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --status
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --validate
SIHSALUS_POSTGRES_PASSWORD='<local-db-password>' ./scripts/liquibase-dry-run.sh --sql
```

Review:

```text
target/liquibase-dry-run/update.sql
target/liquibase-dry-run/update.sql.log
```

For release candidates, also run the compose runtime smoke with real static
content and OCL enabled. The CI smoke may disable OCL to keep infrastructure
feedback fast; that does not replace release smoke.

## Residual Risk Register

| Area | Current Control | Remaining Work |
| --- | --- | --- |
| Secrets | Compose requires secrets; PostgreSQL direct runtime rejects blank datasource credentials; admin bootstrap rejects default password. | External secret manager and rotation procedure. |
| OCL | Static import enabled and fail-on-errors by default. | Full enabled release smoke with production-like content. |
| Attachments | New installs get an upload extension allow-list. | Malware scan, quarantine, storage backup, object-level download tests. |
| XStream | Deny-by-default serializer tests exist. | Audit any new serializer endpoints before exposing them. |
| Auth | REST/FHIR unauthenticated smoke coverage exists. | Object-level/BOLA coverage per clinical workflow. |
| PostgreSQL | Liquibase dry-run script exists. | Promote live PostgreSQL smoke to a required release gate. |
