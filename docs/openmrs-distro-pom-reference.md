# OpenMRS Distro POM Reference

Review date: 2026-05-14

Source: `distro-emr-configuration` Maven POM provided as migration reference.

## Purpose

This POM describes an OpenMRS EMR distribution package, not only a Java library.

Its job is to assemble a deployable OpenMRS-based product with:

- OpenMRS webapp runtime.
- Required OpenMRS modules.
- Sihsalus-specific modules.
- Bahmni modules.
- Configuration files.
- SPA assets and SPA configuration.
- Optional demo-content profile.
- Validation profile for generated OpenMRS configuration.

For SIH Salus Core, this file is a reference for the platform capabilities that must be preserved or intentionally replaced during migration.

## Maven Identity

- Parent: `org.openmrs:distro-emr:3.7.0-SNAPSHOT`
- Artifact: `distro-emr-configuration`
- Name: `OpenMRS distro`
- Packaging: `pom`

The artifact is a distribution descriptor. It primarily coordinates dependencies, resources, generated distro files, and packaging steps.

## Generated Layout

The build defines a distribution base directory under:

```text
target/distro-emr-configuration
```

Important generated directories:

- `openmrs_config`: OpenMRS Initializer/configuration files.
- `openmrs_core`: OpenMRS core runtime content.
- `openmrs_modules`: bundled `.omod` modules.
- `spa`: frontend single-page app assets.
- `spa_config`: frontend configuration.

This layout should be treated as the deployment shape to reproduce or map into SIH Salus Core.

## Runtime Baseline

Core runtime:

- OpenMRS webapp/core: `2.8.6`
- Distro parent: `3.7.0-SNAPSHOT`
- Build tool source/target for distro tools: Java 8

Migration implication:

- Existing module compatibility is anchored to OpenMRS `2.8.6`.
- New SIH Salus Core code can target a modern Java baseline, but imported module behavior must be checked against this OpenMRS runtime first.

## Functional Modules

### Bootstrap And Configuration

- `initializer-omod` `2.11.0`: loads metadata/configuration into OpenMRS.
- `metadatamapping-omod` `2.0.0`: maps metadata identifiers across systems.
- `openconceptlab-omod` `3.0.0`: concept dictionary integration with OCL.
- Content packages:
  - `sihsalus-content` `1.8.30`
  - `reference-content` `1.4.0`
  - `reference-demo-content` `1.8.0-SNAPSHOT`

### APIs And Interoperability

- `webservices.rest-omod` `3.4.1`: REST API support.
- `fhir2-omod` `4.0.0-SNAPSHOT`: FHIR API support.
- `event-omod` `4.0.0`: eventing/integration support.
- `sihsalusinterop-omod` is present but commented out.

### Authentication And Security

- `authentication-omod` `2.3.0`. The SIH Salus Core static import currently uses local `authentication-api` source from `2.4.0-SNAPSHOT`; the `2.3.0` value remains the distro dependency baseline.
- `oauth2login-omod` `1.5.0`

These are part of the login/session/authentication surface that frontend and integrations may depend on.

### Patient And Clinical Data

- `idgen-omod` `5.0.4`: patient or identifier generation. The SIH Salus Core static import currently uses local `idgen-api` source from `6.0.0-SNAPSHOT`; the `5.0.4` value remains the distro dependency baseline.
- `addresshierarchy-omod` `2.21.0`: structured address hierarchy. The SIH Salus Core static import currently uses local `addresshierarchy-api` source from `3.0.0-SNAPSHOT`; the `2.21.0` value remains the distro dependency baseline.
- `patientdocuments-omod` `1.1.0-SNAPSHOT`: patient document management.
- `attachments-omod` `4.0.0`: generic file attachments.
- `cohort-omod` `3.7.3`: patient cohorts.
- `patientflags-omod` `3.0.10`: patient alerts/flags.
- `o3forms-omod` `2.3.0`: OpenMRS 3 form support.
- `emrapi-omod` `3.4.0`: EMR service APIs.
- `ordertemplates-omod` `2.2.0`: order templates are versioned in properties, though no dependency is listed in the provided POM.

### Care Delivery Workflow

- `queue-omod` `3.0.0`: patient queue workflows.
- `appointments-omod` `2.1.0-20250318.070530-1`: appointments.
- `teleconsultation-omod` `2.1.0-20250318.154145-1`: teleconsultation.
- `bedmanagement-omod` `7.2.0`: beds/admission-related workflows.

### Reporting

- `reporting-omod` `2.1.0`
- `reportingrest-omod` `2.0.0`
- `calculation-omod` `2.0.0`
- `htmlwidgets-omod` `2.0.1`
- `serialization.xstream-api` `0.3.0`

The calculation, htmlwidgets, and xstream modules are required by reporting.

### Billing, Stock, Imaging, And Local Extensions

- `stockmanagement-omod` `3.0.0`
- `billing-omod` `2.2.0`
- `fua-omod` `1.0.75`
- `imaging-omod` `1.2.2`

`fua` and `imaging` come from `io.github.proyecto-santaclotilde`, so they should be treated as SIH Salus/local ecosystem dependencies rather than generic OpenMRS modules.

### Legacy UI

- `legacyui-omod` `2.1.0`

This indicates that some workflows may still depend on classic OpenMRS UI behavior even if the product uses a modern SPA.

## Build Pipeline

The POM uses these build steps:

1. `maven-resources-plugin`
   - Copies and filters `distro.properties` and `distro-no-demo.properties` into `target`.

2. `maven-compiler-plugin`
   - Compiles distribution build tools with Java 8 source/target.

3. `openmrs-sdk-maven-plugin`
   - Runs `build-distro`.
   - Reads `target/distro.properties`.
   - Outputs the SDK distro into `target/sdk-distro`.

4. `exec-maven-plugin`
   - Runs `org.openmrs.distro.tools.NormalizeOclExports`.
   - Normalizes OCL exports under `target/sdk-distro/web/openmrs_config/ocl`.

5. `maven-assembly-plugin`
   - Packages the final distribution artifact using `src/main/assembly/assembly.xml`.

## Profiles

### `no-demo`

Builds the distro using:

```text
target/distro-no-demo.properties
```

This produces a production-like distro without demo data.

### `validator`

Runs OpenMRS packager validation against:

```text
target/sdk-distro/web/openmrs_config
```

This profile is important for migration because it validates generated configuration, not just Java compilation.

## Migration Meaning For SIH Salus Core

The distro represents the current product surface SIH Salus Core must understand:

- Runtime compatibility with OpenMRS `2.8.6`.
- Module loading and version compatibility.
- Initializer-driven metadata.
- REST and FHIR APIs.
- Authentication and OAuth login behavior.
- Patient, encounter, forms, documents, reporting, queue, appointments, teleconsultation, beds, stock, billing, FUA, and imaging workflows.
- SPA and SPA configuration packaging.
- Demo vs no-demo distribution modes.
- Configuration validation as a required quality gate.

## Suggested Migration Inventory

For each module or capability, record:

- Whether SIH Salus Core will keep it, replace it, or defer it.
- Source repository and version.
- OpenMRS API dependencies.
- Database tables and migrations.
- REST/FHIR endpoints exposed.
- Frontend routes or widgets depending on it.
- Required Initializer configuration.
- Required content packages.
- Java compatibility status.
- Smoke test result.

## Initial Priority Order

Recommended first-pass migration order:

1. Distribution properties and generated layout.
2. OpenMRS runtime baseline.
3. Initializer configuration and content packages.
4. Authentication/OAuth.
5. REST and FHIR APIs.
6. Patient identifiers, address hierarchy, documents, and attachments.
7. EMR API, O3 forms, concepts, metadata mapping, and OCL.
8. Queue, appointments, teleconsultation, and bed management.
9. Reporting stack.
10. Stock, billing, FUA, and imaging.
11. Legacy UI dependencies.
12. Validation profile and release assembly.
