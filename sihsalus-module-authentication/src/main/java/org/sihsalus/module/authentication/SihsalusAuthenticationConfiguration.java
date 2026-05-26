package org.sihsalus.module.authentication;

import java.util.Properties;
import org.openmrs.UserSessionListener;
import org.openmrs.api.context.AuthenticationScheme;
import org.openmrs.api.context.Context;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.openmrs.module.authentication.AuthenticationUserSessionListener;
import org.openmrs.module.authentication.AuthenticationUtil;
import org.openmrs.module.authentication.DelegatingAuthenticationScheme;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SihsalusAuthenticationConfiguration {

  @Bean
  @Primary
  AuthenticationScheme authenticationScheme() {
    AuthenticationConfig.registerClassLoader(getClass().getClassLoader());
    return new DelegatingAuthenticationScheme();
  }

  @Bean
  UserSessionListener authenticationUserSessionListener() {
    return new AuthenticationUserSessionListener();
  }

  @Bean
  SmartInitializingSingleton authenticationConfigInitializer() {
    return () -> {
      Properties runtimeProperties = Context.getRuntimeProperties();
      AuthenticationConfig.setConfig(
          AuthenticationUtil.getPropertiesWithPrefix(
              runtimeProperties, AuthenticationConfig.PREFIX, false));
    };
  }
}
