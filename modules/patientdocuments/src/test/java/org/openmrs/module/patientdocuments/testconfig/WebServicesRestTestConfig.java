package org.openmrs.module.patientdocuments.testconfig;

import static org.mockito.Mockito.mock;

import org.openmrs.module.webservices.rest.web.api.RestHelperService;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServicesRestTestConfig {

  @Bean
  public RestService restService() {
    return mock(RestService.class);
  }

  @Bean(name = "restHelperService")
  public RestHelperService restHelperService() {
    return mock(RestHelperService.class);
  }
}
