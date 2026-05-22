package org.sihsalus.fhir2;

import java.io.IOException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.openmrs.module.fhir2.spring.FhirAopConfiguration;

@Configuration
@Import(FhirAopConfiguration.class)
@ComponentScan(
        basePackages = {
            "org.openmrs.module.fhir2.api",
            "org.openmrs.module.fhir2.providers.r4"
        },
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "org\\.openmrs\\.module\\.fhir2\\.spring\\..*"))
public class Fhir2Configuration {

    @Bean
    FhirContext fhirR3() {
        FhirContext fhirContext = FhirContext.forDstu3Cached();
        fhirContext.setRestfulClientFactory(new ApacheRestfulClientFactory(fhirContext));
        return fhirContext;
    }

    @Bean
    FhirContext fhirR4() {
        FhirContext fhirContext = FhirContext.forR4Cached();
        fhirContext.setRestfulClientFactory(new ApacheRestfulClientFactory(fhirContext));
        return fhirContext;
    }

    @Bean
    ApacheRestfulClientFactory restfulClientFactoryR3(@Qualifier("fhirR3") FhirContext fhirR3) {
        return new ApacheRestfulClientFactory(fhirR3);
    }

    @Bean
    ApacheRestfulClientFactory restfulClientFactoryR4(@Qualifier("fhirR4") FhirContext fhirR4) {
        return new ApacheRestfulClientFactory(fhirR4);
    }

    @Bean
    UcumEssenceService baseUcumService() throws IOException, UcumException {
        return new UcumEssenceService(new ClassPathResource("ucum-essence.xml").getInputStream());
    }

    @Bean
    UcumEssenceService fhirUcumService() throws IOException, UcumException {
        return new UcumEssenceService(new ClassPathResource("ucum-fhir-essence.xml").getInputStream());
    }
}
