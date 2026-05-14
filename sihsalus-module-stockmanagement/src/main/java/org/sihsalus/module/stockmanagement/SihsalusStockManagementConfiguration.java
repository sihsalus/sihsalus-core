package org.sihsalus.module.stockmanagement;

import java.lang.reflect.Proxy;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.stockmanagement.StockManagementConfig;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.validator.BatchJobValidator;
import org.openmrs.module.stockmanagement.api.validator.StockItemDTOValidator;
import org.openmrs.module.stockmanagement.api.validator.StockItemPackagingUOMValidator;
import org.openmrs.module.stockmanagement.api.validator.StockOperationActionValidator;
import org.openmrs.module.stockmanagement.api.validator.StockOperationDTOValidator;
import org.openmrs.module.stockmanagement.api.validator.StockRuleValidator;
import org.openmrs.module.stockmanagement.api.validator.UserRoleScopeDTOValidator;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;

@Configuration
public class SihsalusStockManagementConfiguration {

    @Bean
    StockManagementConfig stockManagementConfig() {
        return new StockManagementConfig();
    }

    @Bean
    StockManagementService stockManagementService(
            SessionFactory sessionFactory, TransactionManager transactionManager) {
        ClassLoader classLoader = StockManagementService.class.getClassLoader();
        return (StockManagementService) Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] {StockManagementService.class},
                new PartialStockManagementService(sessionFactory, transactionManager));
    }

    @Bean
    SmartInitializingSingleton stockManagementServiceRegistrar(
            ServiceContext serviceContext, StockManagementService stockManagementService) {
        return () -> serviceContext.setService(StockManagementService.class, stockManagementService);
    }

    @Bean
    StockItemPackagingUOMValidator stockItemPackagingUOMValidator() {
        return new StockItemPackagingUOMValidator();
    }

    @Bean
    StockOperationDTOValidator stockOperationDTOValidator() {
        return new StockOperationDTOValidator();
    }

    @Bean
    StockRuleValidator stockRuleValidator() {
        return new StockRuleValidator();
    }

    @Bean
    UserRoleScopeDTOValidator userRoleScopeDTOValidator() {
        return new UserRoleScopeDTOValidator();
    }

    @Bean
    StockItemDTOValidator stockItemDTOValidator() {
        return new StockItemDTOValidator();
    }

    @Bean
    BatchJobValidator batchJobValidator() {
        return new BatchJobValidator();
    }

    @Bean
    StockOperationActionValidator stockOperationActionValidator() {
        return new StockOperationActionValidator();
    }
}
