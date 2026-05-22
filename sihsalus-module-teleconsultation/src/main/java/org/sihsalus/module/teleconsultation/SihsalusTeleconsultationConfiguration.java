package org.sihsalus.module.teleconsultation;

import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.bahmni.module.teleconsultation.api.impl.TeleconsultationServiceImpl;
import org.bahmni.module.teleconsultation.web.controller.TeleconsultationController;
import org.openmrs.api.context.ServiceContext;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = TeleconsultationController.class)
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
