# OpenMRS Distro Source Clones

Clone date: 2026-05-14

Location:

```text
reference-sources/openmrs-distro-modules
```

Separate SIH Salus content checkout:

```text
reference-sources/sihsalus-content
```

The active source repositories from the distro POM were cloned as shallow reference checkouts. The POM versions are tracked in `repositories.tsv`; the table below records the checked-out branch and commit used locally.

| Component | Branch | Commit | Repository |
| --- | --- | --- | --- |
| addresshierarchy | master | `873bb73` | `https://github.com/openmrs/openmrs-module-addresshierarchy.git` |
| appointments | master | `a150aa6` | `https://github.com/Bahmni/openmrs-module-appointments.git` |
| authentication | main | `58592bb` | `https://github.com/openmrs/openmrs-module-authentication.git` |
| attachments | master | `86251a3` | `https://github.com/openmrs/openmrs-module-attachments.git` |
| bedmanagement | master | `ccd2764` | `https://github.com/openmrs/openmrs-module-bedmanagement.git` |
| billing | main | `651dce6` | `https://github.com/openmrs/openmrs-module-billing.git` |
| calculation | master | `2031746` | `https://github.com/openmrs/openmrs-module-calculation.git` |
| cohort | master | `b719991` | `https://github.com/openmrs/openmrs-module-cohort.git` |
| emrapi | master | `3c0c14c` | `https://github.com/openmrs/openmrs-module-emrapi.git` |
| event | master | `36952b9` | `https://github.com/openmrs/openmrs-module-event.git` |
| fhir2 | master | `9e57e9f` | `https://github.com/openmrs/openmrs-module-fhir2.git` |
| fua | main | `cf7cb50` | `https://github.com/proyecto-santaclotilde/openmrs-module-fua.git` |
| htmlwidgets | master | `64f8ff2` | `https://github.com/openmrs/openmrs-module-htmlwidgets.git` |
| idgen | master | `1f7ace4` | `https://github.com/openmrs/openmrs-module-idgen.git` |
| imaging | main | `fc9dfb6` | `https://github.com/proyecto-santaclotilde/openmrs-module-imaging.git` |
| initializer | main | `ca0ef6f` | `https://github.com/mekomsolutions/openmrs-module-initializer.git` |
| legacyui | master | `da208b4` | `https://github.com/openmrs/openmrs-module-legacyui.git` |
| metadatamapping | master | `8b7df0c` | `https://github.com/openmrs/openmrs-module-metadatamapping.git` |
| o3forms | main | `ff1f7c0` | `https://github.com/openmrs/openmrs-module-o3forms.git` |
| openconceptlab | master | `83c96f1` | `https://github.com/openmrs/openmrs-module-openconceptlab.git` |
| openmrs-webapp | master | `9490dcf` | `https://github.com/openmrs/openmrs-core.git` |
| ordertemplates | main | `804c07e` | `https://github.com/openmrs/openmrs-module-ordertemplates.git` |
| oauth2login | master | `fe01437` | `https://github.com/openmrs/openmrs-module-oauth2login.git` |
| patientdocuments | main | `28ad254` | `https://github.com/openmrs/openmrs-module-patientdocuments.git` |
| patientflags | master | `c10dd6b` | `https://github.com/openmrs/openmrs-module-patientflags.git` |
| queue | main | `e6ffdda` | `https://github.com/openmrs/openmrs-module-queue.git` |
| reference-content | main | `cd7d604` | `https://github.com/openmrs/openmrs-content-referenceapplication.git` |
| reference-demo-content | main | `0163849` | `https://github.com/openmrs/openmrs-content-referenceapplication-demo.git` |
| referencedemodata | master | `1e4bd4b` | `https://github.com/openmrs/openmrs-module-referencedemodata.git` |
| reporting | master | `8c28986` | `https://github.com/openmrs/openmrs-module-reporting.git` |
| reportingrest | master | `0fca35e` | `https://github.com/openmrs/openmrs-module-reportingrest.git` |
| serialization-xstream | master | `268e0ef` | `https://github.com/openmrs/openmrs-module-serialization.xstream.git` |
| sihsalus-content | main | `01dfded` | `https://github.com/sihsalus/sihsalus-content.git` |
| sihsalus-interop | main | `3968f2e` | `https://github.com/proyecto-santaclotilde/openmrs-module-sihsalusinterop.git` |
| stockmanagement | master | `c1ea462` | `https://github.com/openmrs/openmrs-module-stockmanagement.git` |
| teleconsultation | main | `0ba2c13` | `https://github.com/Bahmni/openmrs-module-teleconsultation.git` |
| webservices-rest | master | `628b667` | `https://github.com/openmrs/openmrs-module-webservices.rest.git` |

## Notes

- `sihsalusinterop-omod` is commented out in the provided POM, but it was cloned because the repository and `1.0.3` tag exist.
- `sihsalus-content` also exists as a separate checkout at `reference-sources/sihsalus-content`, because it is distribution content/configuration rather than module source.
- These are default-branch shallow clones, intended as local source references. They are not yet checked out to the exact Maven release tags or snapshot commits from the distro POM.
