package org.sihsalus.module.billing;

import java.util.List;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusBillingConfiguration {

    @Bean
    HibernateMappingContributor billingHibernateMappingContributor() {
        return () -> List.of("Bill.hbm.xml", "Cashier.hbm.xml", "SequentialReceiptNumberGenerator.hbm.xml");
    }
}
