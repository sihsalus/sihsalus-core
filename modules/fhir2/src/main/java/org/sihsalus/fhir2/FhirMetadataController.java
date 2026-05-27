package org.sihsalus.fhir2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirMetadataController {

  private final FhirCapabilityService service;

  FhirMetadataController(FhirCapabilityService service) {
    this.service = service;
  }

  @GetMapping(
      value = {
        "/api/fhir/metadata",
        "/api/fhir/CapabilityStatement",
        "/api/fhir/r4/metadata",
        "/ws/fhir2/R4/metadata"
      },
      produces = {"application/fhir+json", "application/json"})
  String metadata() {
    return service.capabilityStatementJson();
  }
}
