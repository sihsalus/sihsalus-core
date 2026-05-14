package org.sihsalus.webservices.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemRestConfiguration {

    @Bean
    SystemStatusController systemStatusController() {
        return new SystemStatusController();
    }
}
