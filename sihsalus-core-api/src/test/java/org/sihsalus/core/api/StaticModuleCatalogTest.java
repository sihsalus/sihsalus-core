package org.sihsalus.core.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaticModuleCatalogTest {

    @Test
    void includesFhirAndNoOmodRuntimeModule() {
        assertTrue(StaticModuleCatalog.modules().stream().anyMatch(module -> module.id().equals("fhir2")));
        assertTrue(StaticModuleCatalog.modules().stream().noneMatch(module -> module.id().equals("omod-runtime")));
    }
}
