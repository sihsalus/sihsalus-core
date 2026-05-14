package org.sihsalus.core.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaticModuleCatalogTest {

    @Test
    void includesFhirAndNoOmodRuntimeModule() {
        assertTrue(StaticModuleCatalog.modules().stream().anyMatch(module -> module.id().equals("fhir2")));
        assertTrue(StaticModuleCatalog.modules().stream().noneMatch(module -> module.id().equals("omod-runtime")));
    }

    @Test
    void idgenIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("idgen")
                        && module.sourceModule().equals("idgen-api")
                        && module.baselineVersion().equals("6.0.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void authenticationIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("authentication")
                        && module.sourceModule().equals("authentication-api")
                        && module.baselineVersion().equals("2.4.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void oauth2LoginIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("oauth2login")
                        && module.sourceModule().equals("oauth2login-api")
                        && module.baselineVersion().equals("1.6.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void addressHierarchyIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("addresshierarchy")
                        && module.sourceModule().equals("addresshierarchy-api")
                        && module.baselineVersion().equals("3.0.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void patientDocumentsIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("patientdocuments")
                        && module.sourceModule().equals("patientdocuments-omod")
                        && module.baselineVersion().equals("1.1.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void cohortIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("cohort")
                        && module.sourceModule().equals("cohort-omod")
                        && module.baselineVersion().equals("3.7.3")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void metadataMappingIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("metadatamapping")
                        && module.sourceModule().equals("metadatamapping-api")
                        && module.baselineVersion().equals("2.1.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void emrApiIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("emrapi")
                        && module.sourceModule().equals("emrapi-api")
                        && module.baselineVersion().equals("3.5.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void o3FormsIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("o3forms")
                        && module.sourceModule().equals("o3forms-omod")
                        && module.baselineVersion().equals("2.3.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void calculationIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("calculation")
                        && module.sourceModule().equals("calculation-api")
                        && module.baselineVersion().equals("2.1.0-SNAPSHOT")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void htmlWidgetsIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("htmlwidgets")
                        && module.sourceModule().equals("htmlwidgets-omod")
                        && module.baselineVersion().equals("2.0.1")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void stockManagementIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("stockmanagement")
                        && module.sourceModule().equals("stockmanagement-api")
                        && module.baselineVersion().equals("3.0.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void fuaIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("fua")
                        && module.sourceModule().equals("fua-omod")
                        && module.baselineVersion().equals("1.0.75")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void imagingIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("imaging")
                        && module.sourceModule().equals("imaging-omod")
                        && module.baselineVersion().equals("1.2.2")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void attachmentsIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("attachments")
                        && module.sourceModule().equals("attachments-omod")
                        && module.baselineVersion().equals("4.0.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void serializationXstreamIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("serialization-xstream")
                        && module.sourceModule().equals("serialization.xstream-api")
                        && module.baselineVersion().equals("0.3.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void orderTemplatesIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("ordertemplates")
                        && module.sourceModule().equals("ordertemplates-omod")
                        && module.baselineVersion().equals("2.2.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void reportingIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("reporting")
                        && module.sourceModule().equals("reporting-omod")
                        && module.baselineVersion().equals("2.1.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void reportingRestIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("reportingrest")
                        && module.sourceModule().equals("reportingrest-omod")
                        && module.baselineVersion().equals("2.0.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }

    @Test
    void legacyUiIsStaticInternalSourceImport() {
        assertTrue(StaticModuleCatalog.modules().stream()
                .anyMatch(module -> module.id().equals("legacyui")
                        && module.sourceModule().equals("legacyui-omod")
                        && module.baselineVersion().equals("2.1.0")
                        && module.status() == SihsalusModuleStatus.STATIC_INTERNAL));
    }
}
