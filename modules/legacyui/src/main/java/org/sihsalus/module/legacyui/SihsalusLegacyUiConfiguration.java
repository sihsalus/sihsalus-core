package org.sihsalus.module.legacyui;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.legacyui.api.LegacyUIService;
import org.openmrs.module.legacyui.api.impl.LegacyUIImpl;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
