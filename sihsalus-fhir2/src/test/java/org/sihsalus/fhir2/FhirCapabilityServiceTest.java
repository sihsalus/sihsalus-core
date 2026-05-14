package org.sihsalus.fhir2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FhirCapabilityServiceTest {

    @Test
    void producesR4CapabilityStatement() {
        assertEquals("4.0.1", new FhirCapabilityService().capabilityStatement().get("fhirVersion"));
    }
}
