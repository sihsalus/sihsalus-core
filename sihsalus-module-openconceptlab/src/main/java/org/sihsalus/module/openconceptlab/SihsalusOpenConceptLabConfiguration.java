package org.sihsalus.module.openconceptlab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.Extension;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ImportServiceImpl;
import org.openmrs.module.openconceptlab.OclConceptService;
import org.openmrs.module.openconceptlab.OclConceptServiceImpl;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.module.openconceptlab.client.OclClient;
import org.openmrs.module.openconceptlab.extension.html.AdminList;
import org.openmrs.module.openconceptlab.extension.html.HighlightSubscribedConcept;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.importer.Saver;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.openconceptlab.web.rest.controller.OpenConceptLabRestController;
import org.openmrs.module.openconceptlab.web.rest.resources.ImportResource;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.initializer.StaticSihsalusContentLoader;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ComponentScan(basePackageClasses = {OpenConceptLabRestController.class, ImportResource.class})
public class SihsalusOpenConceptLabConfiguration {

  private static final String ADMIN_LIST_EXTENSION_POINT = "org.openmrs.admin.list";

  private static final String CONCEPT_HEADER_EXTENSION_POINT =
      "org.openmrs.dictionary.conceptHeader";

  private static final Map<String, String> OPEN_CONCEPT_LAB_GLOBAL_PROPERTIES =
      Map.of(
          OpenConceptLabConstants.GP_SUBSCRIPTION_URL, "The OCL subscription URL",
          OpenConceptLabConstants.GP_SCHEDULED_DAYS,
              "Interval in days when the process is repeated",
          OpenConceptLabConstants.GP_SCHEDULED_TIME,
              "The the time when the process should be carried on",
          OpenConceptLabConstants.GP_TOKEN, "The OCL API Token");

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
  Saver openConceptLabSaver(
      ConceptService conceptService, ImportService openConceptLabImportService) {
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
      Importer openConceptLabImporter, ImportService openConceptLabImportService) {
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

  @Bean
  SmartInitializingSingleton openConceptLabStaticInitializer(
      AdministrationService administrationService, ImportService openConceptLabImportService) {
    return () -> {
      ensureGlobalProperties(administrationService);
      registerAdminListExtension();
      registerHighlightSubscribedConceptExtension(openConceptLabImportService);
    };
  }

  @Bean
  SihsalusOpenConceptLabStaticContentImporter openConceptLabStaticContentImporter(
      Importer openConceptLabImporter,
      ImportService openConceptLabImportService,
      AdministrationService administrationService,
      StaticSihsalusContentLoader contentLoader,
      Environment environment) {
    return new SihsalusOpenConceptLabStaticContentImporter(
        openConceptLabImporter,
        openConceptLabImportService,
        administrationService,
        contentLoader,
        environment.getProperty("sihsalus.ocl.static-import.enabled", Boolean.class, true),
        environment.getProperty("sihsalus.ocl.static-import.fail-on-errors", Boolean.class, false));
  }

  private static void ensureGlobalProperties(AdministrationService administrationService) {
    boolean openedSession = !Context.isSessionOpen();
    if (openedSession) {
      Context.openSession();
    }

    Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
    Context.addProxyPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
    try {
      OPEN_CONCEPT_LAB_GLOBAL_PROPERTIES.forEach(
          (property, description) -> {
            if (administrationService.getGlobalPropertyObject(property) == null) {
              administrationService.saveGlobalProperty(
                  new GlobalProperty(property, "", description));
            }
          });
    } finally {
      Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
      Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
      if (openedSession) {
        Context.closeSession();
      }
    }
  }

  private static void registerAdminListExtension() {
    registerExtension(ADMIN_LIST_EXTENSION_POINT, new AdminList());
  }

  private static void registerHighlightSubscribedConceptExtension(ImportService importService) {
    HighlightSubscribedConcept extension = new HighlightSubscribedConcept();
    extension.setService(importService);
    registerExtension(CONCEPT_HEADER_EXTENSION_POINT, extension);
  }

  private static void registerExtension(String pointId, Extension extension) {
    extension.setPointId(pointId);
    List<Extension> extensions =
        ModuleFactory.getExtensionMap()
            .computeIfAbsent(
                pointId + Extension.EXTENSION_ID_SEPARATOR + "html", key -> new ArrayList<>());
    if (extensions.stream()
        .noneMatch(existing -> existing.getClass().equals(extension.getClass()))) {
      extensions.add(extension);
    }
  }
}
