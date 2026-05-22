package org.sihsalus.module.sihsalusinterop;

import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.sihsalusinterop.api.DyakuSenderService;
import org.openmrs.module.sihsalusinterop.api.advice.EncounterSavedAdvice;
import org.openmrs.module.sihsalusinterop.api.dao.InteropQueueDao;
import org.openmrs.module.sihsalusinterop.api.impl.DyakuSenderServiceImpl;
import org.openmrs.module.sihsalusinterop.api.service.BundleBuilderService;
import org.openmrs.module.sihsalusinterop.web.controller.InteropQueueController;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.sihsalus.core.api.StaticModuleTaskRunner;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ComponentScan(basePackageClasses = InteropQueueController.class)
public class SihsalusInteropConfiguration {

    private static final Log log = LogFactory.getLog(SihsalusInteropConfiguration.class);

    private static final long QUEUE_PROCESSOR_REPEAT_INTERVAL_MILLIS = 300_000L;

    @Bean
    HibernateMappingContributor sihsalusInteropHibernateMappingContributor() {
        return () -> List.of("SihSalusInterop.hbm.xml");
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

    @Bean(name = "sihsalusinterop.taskScheduler", destroyMethod = "shutdown")
    ThreadPoolTaskScheduler sihsalusInteropTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("sihsalusinterop-");
        return scheduler;
    }

    @Bean
    InitializingBean registerSihsalusInteropServices(
            ServiceContext serviceContext,
            DyakuSenderService dyakuSenderService) {
        return () -> serviceContext.setService(DyakuSenderService.class, dyakuSenderService);
    }

    @Bean
    SmartInitializingSingleton sihsalusInteropStaticInitializer(
            ServiceContext serviceContext,
            ThreadPoolTaskScheduler sihsalusInteropTaskScheduler) {
        return () -> {
            serviceContext.addAdvice(EncounterService.class, new EncounterSavedAdvice());
            sihsalusInteropTaskScheduler.scheduleAtFixedRate(
                    SihsalusInteropConfiguration::processInteropQueue,
                    new Date(System.currentTimeMillis() + QUEUE_PROCESSOR_REPEAT_INTERVAL_MILLIS),
                    QUEUE_PROCESSOR_REPEAT_INTERVAL_MILLIS);
        };
    }

    private static void processInteropQueue() {
        try {
            StaticModuleTaskRunner.runAuthenticated(() -> Context.getService(DyakuSenderService.class).processQueue());
        }
        catch (Exception e) {
            log.warn("No se pudo procesar la cola automatica de interop", e);
        }
    }
}
