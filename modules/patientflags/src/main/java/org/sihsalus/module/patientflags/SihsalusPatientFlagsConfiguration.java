package org.sihsalus.module.patientflags;

import java.util.List;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConditionService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.patientflags.aop.ConditionServiceAdvice;
import org.openmrs.module.patientflags.aop.EncounterServiceAdvice;
import org.openmrs.module.patientflags.aop.ObsServiceAdvice;
import org.openmrs.module.patientflags.aop.OrderServiceAdvice;
import org.openmrs.module.patientflags.aop.PatientServiceAdvice;
import org.openmrs.module.patientflags.aop.ProgramWorkflowServiceAdvice;
import org.openmrs.module.patientflags.api.FlagService;
import org.openmrs.module.patientflags.api.impl.FlagServiceImpl;
import org.openmrs.module.patientflags.db.FlagDAO;
import org.openmrs.module.patientflags.db.hibernate.HibernateFlagDAO;
import org.openmrs.module.patientflags.web.PatientFlagsRestController;
import org.openmrs.module.patientflags.web.RefAppConfiguration;
import org.openmrs.module.patientflags.web.validators.FlagValidator;
import org.openmrs.module.patientflags.web.validators.PatientFlagsPropertiesValidator;
import org.openmrs.module.patientflags.web.validators.PriorityValidator;
import org.openmrs.module.patientflags.web.validators.TagValidator;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusPatientFlagsConfiguration {

  private static final String GP_PATIENT_HEADER_DISPLAY = "patientflags.patientHeaderDisplay";

  private static final String GP_PATIENT_OVERVIEW_DISPLAY = "patientflags.patientOverviewDisplay";

  private static final String GP_USERNAME = "patientflags.username";

  @Bean
  HibernateMappingContributor patientFlagsHibernateMappingContributor() {
    return () ->
        List.of(
            "DisplayPoint.hbm.xml",
            "Flag.hbm.xml",
            "PatientFlag.hbm.xml",
            "Priority.hbm.xml",
            "Tag.hbm.xml");
  }

  @Bean
  FlagDAO patientFlagsDao(DbSessionFactory dbSessionFactory) {
    HibernateFlagDAO dao = new HibernateFlagDAO();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean
  FlagService flagService(FlagDAO patientFlagsDao) {
    FlagServiceImpl service = new FlagServiceImpl();
    service.setFlagDAO(patientFlagsDao);
    return service;
  }

  @Bean
  PatientFlagsRestController patientFlagsRestController() {
    return new PatientFlagsRestController();
  }

  @Bean
  FlagValidator flagValidator() {
    return new FlagValidator();
  }

  @Bean
  TagValidator tagValidator() {
    return new TagValidator();
  }

  @Bean
  PriorityValidator priorityValidator() {
    return new PriorityValidator();
  }

  @Bean
  PatientFlagsPropertiesValidator patientFlagsPropertiesValidator() {
    return new PatientFlagsPropertiesValidator();
  }

  @Bean
  static RefAppConfiguration patientFlagsRefAppConfiguration() {
    return new RefAppConfiguration();
  }

  @Bean
  SmartInitializingSingleton patientFlagsServiceRegistrar(
      ServiceContext serviceContext, FlagService flagService) {
    return () -> serviceContext.setService(FlagService.class, flagService);
  }

  @Bean
  SmartInitializingSingleton patientFlagsStaticInitializer(
      AdministrationService administrationService) {
    return () -> ensureGlobalProperties(administrationService);
  }

  @Bean
  SmartInitializingSingleton patientFlagsAdviceRegistrar(ServiceContext serviceContext) {
    return () -> {
      serviceContext.addAdvice(EncounterService.class, new EncounterServiceAdvice());
      serviceContext.addAdvice(ObsService.class, new ObsServiceAdvice());
      serviceContext.addAdvice(OrderService.class, new OrderServiceAdvice());
      serviceContext.addAdvice(PatientService.class, new PatientServiceAdvice());
      serviceContext.addAdvice(ConditionService.class, new ConditionServiceAdvice());
      serviceContext.addAdvice(ProgramWorkflowService.class, new ProgramWorkflowServiceAdvice());
    };
  }

  private static void ensureGlobalProperties(AdministrationService administrationService) {
    boolean openedSession = !Context.isSessionOpen();
    if (openedSession) {
      Context.openSession();
    }

    Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
    Context.addProxyPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
    try {
      ensureGlobalProperty(
          administrationService,
          GP_PATIENT_HEADER_DISPLAY,
          "true",
          "DO NOT MODIFY HERE: use \"manage flag global properties\" to modify; true/false whether or not to display flags in the Patient Dashboard overview");
      ensureGlobalProperty(
          administrationService,
          GP_PATIENT_OVERVIEW_DISPLAY,
          "true",
          "DO NOT MODIFY HERE: use \"manage flag global properties\" to modify; true/false whether or not to display flags in the Patient Dashboard header");
      String schedulerUsername = administrationService.getGlobalProperty("scheduler.username");
      ensureGlobalProperty(
          administrationService,
          GP_USERNAME,
          schedulerUsername == null ? "" : schedulerUsername,
          "DO NOT MODIFY HERE: user \"manage flag global properties\" to modify; Username for the OpenMRS user that will evaluate Groovy flags");
    } finally {
      Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
      Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
      if (openedSession) {
        Context.closeSession();
      }
    }
  }

  private static void ensureGlobalProperty(
      AdministrationService administrationService,
      String property,
      String defaultValue,
      String description) {
    if (administrationService.getGlobalPropertyObject(property) == null) {
      administrationService.saveGlobalProperty(
          new GlobalProperty(property, defaultValue, description));
    }
  }
}
