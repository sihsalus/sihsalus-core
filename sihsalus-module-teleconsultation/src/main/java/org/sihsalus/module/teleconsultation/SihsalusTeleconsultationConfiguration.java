package org.sihsalus.module.teleconsultation;

import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.bahmni.module.teleconsultation.api.impl.TeleconsultationServiceImpl;
import org.openmrs.api.context.ServiceContext;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusTeleconsultationConfiguration {

    @Bean
    TeleconsultationService teleconsultationService() {
        return new TeleconsultationServiceImpl();
    }

    @Bean
    SmartInitializingSingleton teleconsultationServiceRegistrar(
            ServiceContext serviceContext, TeleconsultationService teleconsultationService) {
        return () -> serviceContext.setService(TeleconsultationService.class, teleconsultationService);
    }
}
