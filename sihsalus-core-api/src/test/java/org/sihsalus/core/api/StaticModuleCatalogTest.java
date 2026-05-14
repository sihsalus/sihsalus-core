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
}
