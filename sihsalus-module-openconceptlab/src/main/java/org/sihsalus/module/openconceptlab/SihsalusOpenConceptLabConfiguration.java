package org.sihsalus.module.openconceptlab;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ImportServiceImpl;
import org.openmrs.module.openconceptlab.OclConceptService;
import org.openmrs.module.openconceptlab.OclConceptServiceImpl;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.importer.Saver;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.openconceptlab.web.rest.resources.ImportResource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ComponentScan(basePackageClasses = {OpenConceptLabRestController.class, ImportResource.class})
public class SihsalusOpenConceptLabConfiguration {

    @Bean(name = "openconceptlab.importService")
    ImportService openConceptLabImportService(
            DbSessionFactory dbSessionFactory,
            AdministrationService administrationService,
            ConceptService conceptService,
            OclConceptService oclConceptService) {
        ImportServiceImpl service = new ImportServiceImpl();
        service.setSessionFactory(dbSessionFactory);
        service.setAdminService(administrationService);
        service.setConceptService(conceptService);
        service.setOclConceptService(oclConceptService);
        return service;
    }

    @Bean(name = "openconceptlab.conceptService")
    OclConceptService openConceptLabConceptService(DbSessionFactory dbSessionFactory) {
        OclConceptServiceImpl service = new OclConceptServiceImpl();
        service.setDbSessionFactory(dbSessionFactory);
        return service;
    }

    @Bean(name = "openconceptlab.oclClient")
    OclClient openConceptLabOclClient() {
        return new OclClient();
    }

    @Bean(name = "openconceptlab.saver")
    Saver openConceptLabSaver(ConceptService conceptService, ImportService openConceptLabImportService) {
        Saver saver = new Saver();
        saver.setConceptService(conceptService);
        saver.setImportService(openConceptLabImportService);
        return saver;
    }

    @Bean(name = "openconceptlab.scheduler", destroyMethod = "shutdown", autowireCandidate = false)
    ThreadPoolTaskScheduler openConceptLabTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("openconceptlab-");
        return scheduler;
    }

    @Bean(name = "openconceptlab.importer")
    Importer openConceptLabImporter(
            ImportService openConceptLabImportService,
            ConceptService conceptService,
            OclConceptService oclConceptService,
            OclClient openConceptLabOclClient,
            Saver openConceptLabSaver) {
        Importer importer = new Importer();
        importer.setImportService(openConceptLabImportService);
        importer.setConceptService(conceptService);
        importer.setOclConceptService(oclConceptService);
        importer.setOclClient(openConceptLabOclClient);
        importer.setSaver(openConceptLabSaver);
        return importer;
    }

    @Bean(name = "openconceptlab.updateScheduler")
    UpdateScheduler openConceptLabUpdateScheduler(
            Importer openConceptLabImporter,
            ImportService openConceptLabImportService) {
        UpdateScheduler scheduler = new UpdateScheduler();
        scheduler.setScheduler(openConceptLabTaskScheduler());
        scheduler.setImporter(openConceptLabImporter);
        scheduler.setImportService(openConceptLabImportService);
        return scheduler;
    }

    @Bean
    SmartInitializingSingleton openConceptLabServiceRegistrar(
            ServiceContext serviceContext,
            ImportService openConceptLabImportService,
            OclConceptService openConceptLabConceptService) {
        return () -> {
            serviceContext.setService(ImportService.class, openConceptLabImportService);
            serviceContext.setService(OclConceptService.class, openConceptLabConceptService);
        };
    }
}
