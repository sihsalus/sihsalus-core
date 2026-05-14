package org.sihsalus.fhir2;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class FhirCapabilityService {

    public Map<String, Object> capabilityStatement() {
        return Map.of(
                "resourceType",
                "CapabilityStatement",
                "status",
                "active",
                "date",
                OffsetDateTime.now().toString(),
                "publisher",
                "SIH Salus",
                "kind",
                "instance",
                "fhirVersion",
                "4.0.1",
                "format",
                List.of("json"),
                "rest",
                List.of(
                        Map.of(
                                "mode",
                                "server",
                                "resource",
                                List.of(
                                        Map.of("type", "Patient", "interaction", List.of(Map.of("code", "read"))),
                                        Map.of("type", "Encounter", "interaction", List.of(Map.of("code", "read"))),
                                        Map.of("type", "Observation", "interaction", List.of(Map.of("code", "read")))))));
    }
}
