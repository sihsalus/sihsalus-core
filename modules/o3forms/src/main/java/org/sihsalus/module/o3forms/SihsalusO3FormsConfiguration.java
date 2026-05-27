package org.sihsalus.module.o3forms;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.o3forms.api.O3FormsService;
import org.openmrs.module.o3forms.api.impl.O3FormsServiceImpl;
import org.openmrs.module.o3forms.web.rest.O3FormsResourceController;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = O3FormsResourceController.class)
public class SihsalusO3FormsConfiguration {

  @Bean
  O3FormsService o3FormsService() {
    return new O3FormsServiceImpl();
  }

  @Bean
  SmartInitializingSingleton o3FormsServiceRegistrar(
      ServiceContext serviceContext, O3FormsService o3FormsService) {
    return () -> serviceContext.setService(O3FormsService.class, o3FormsService);
  }
}
