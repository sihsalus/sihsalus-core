package org.sihsalus.module.sihsalusinterop;

import ca.uhn.fhir.context.FhirContext;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.sihsalusinterop.api.DyakuSenderService;
import org.openmrs.module.sihsalusinterop.api.dao.InteropQueueDao;
import org.openmrs.module.sihsalusinterop.api.impl.DyakuSenderServiceImpl;
import org.openmrs.module.sihsalusinterop.api.service.BundleBuilderService;
import org.openmrs.module.sihsalusinterop.web.controller.InteropQueueController;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ComponentScan(basePackageClasses = InteropQueueController.class)
public class SihsalusInteropConfiguration {

    @Bean
    HibernateMappingContributor sihsalusInteropHibernateMappingContributor() {
        return () -> List.of("SihSalusInterop.hbm.xml");
    }

    @Bean("fhirR4")
    FhirContext fhirR4() {
        return FhirContext.forR4();
    }

    @Bean("sihsalusinterop.InteropQueueDao")
    InteropQueueDao sihsalusInteropQueueDao(SessionFactory sessionFactory) {
        InteropQueueDao dao = new InteropQueueDao();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean("sihsalusinterop.DyakuSenderService")
    DyakuSenderService dyakuSenderService(InteropQueueDao sihsalusInteropQueueDao) {
        DyakuSenderServiceImpl service = new DyakuSenderServiceImpl();
        service.setDao(sihsalusInteropQueueDao);
        return service;
    }

    @Bean("sihsalusinterop.BundleBuilderService")
    BundleBuilderService bundleBuilderService() {
        return new BundleBuilderService();
    }

    @Bean
    InitializingBean registerSihsalusInteropServices(
            ServiceContext serviceContext,
            DyakuSenderService dyakuSenderService) {
        return () -> serviceContext.setService(DyakuSenderService.class, dyakuSenderService);
    }
}
