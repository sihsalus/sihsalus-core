package org.sihsalus.initializer;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.initializer.api.InitializerService;
import org.openmrs.module.initializer.api.InitializerServiceImpl;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusInitializerConfiguration {

    @Bean
    InitializerService initializerService() {
        return new InitializerServiceImpl();
    }

    @Bean
    SmartInitializingSingleton initializerServiceRegistrar(
            ServiceContext serviceContext, InitializerService initializerService) {
        return () -> serviceContext.setService(InitializerService.class, initializerService);
    }
}
