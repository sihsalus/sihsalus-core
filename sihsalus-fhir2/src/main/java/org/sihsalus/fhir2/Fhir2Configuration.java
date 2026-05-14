package org.sihsalus.fhir2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Fhir2Configuration {

    @Bean
    FhirCapabilityService fhirCapabilityService() {
        return new FhirCapabilityService();
    }

    @Bean
    FhirMetadataController fhirMetadataController(FhirCapabilityService service) {
        return new FhirMetadataController(service);
    }
}
