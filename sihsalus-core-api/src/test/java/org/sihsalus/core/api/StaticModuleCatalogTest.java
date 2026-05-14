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
}
