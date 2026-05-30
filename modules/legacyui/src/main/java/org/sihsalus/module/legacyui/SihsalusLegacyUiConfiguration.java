package org.sihsalus.module.legacyui;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.legacyui.api.LegacyUIService;
import org.openmrs.module.legacyui.api.impl.LegacyUIImpl;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
    basePackages = {
      "org.openmrs.hl7.web.controller",
      "org.openmrs.module.web.controller",
      "org.openmrs.web.controller"
    })
public class SihsalusLegacyUiConfiguration {

  @Bean
  LegacyUIService legacyUiService() {
    return new LegacyUIImpl();
  }

  @Bean
  SmartInitializingSingleton legacyUiServiceRegistrar(
      ServiceContext serviceContext, LegacyUIService legacyUiService) {
    return () -> serviceContext.setService(LegacyUIService.class, legacyUiService);
  }
}
