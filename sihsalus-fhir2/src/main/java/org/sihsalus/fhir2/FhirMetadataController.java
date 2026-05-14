package org.sihsalus.fhir2;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirMetadataController {

    private final FhirCapabilityService service;

    FhirMetadataController(FhirCapabilityService service) {
        this.service = service;
    }

    @GetMapping({"/api/fhir/metadata", "/api/fhir/CapabilityStatement"})
    Map<String, Object> metadata() {
        return service.capabilityStatement();
    }
}
