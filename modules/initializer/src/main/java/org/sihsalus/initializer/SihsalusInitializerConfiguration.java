package org.sihsalus.initializer;

import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD_DISABLED;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD_FAIL_ON_ERROR;

import java.util.List;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.messagesource.impl.MutableResourceBundleMessageSource;
import org.openmrs.module.initializer.api.InitializerService;
import org.openmrs.module.initializer.api.InitializerServiceImpl;
import org.openmrs.module.initializer.api.loaders.Loader;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SihsalusInitializerConfiguration {

  @Bean
  InitializerService initializerService(List<Loader> loaders) {
    return new InitializerServiceImpl(loaders);
  }

  @Bean
  SmartInitializingSingleton initializerServiceRegistrar(
      ServiceContext serviceContext, InitializerService initializerService) {
    return () -> serviceContext.setService(InitializerService.class, initializerService);
  }

  @Bean
  SmartInitializingSingleton sihsalusMessagePropertiesRegistrar(
      MutableResourceBundleMessageSource messageSource) {
    return new SihsalusMessagePropertiesRegistrar(messageSource);
  }

  @Bean
  StaticSihsalusContentLoader staticSihsalusContentLoader(JdbcTemplate jdbcTemplate) {
    return new StaticSihsalusContentLoader(jdbcTemplate);
  }

  @Bean
  Loader conceptClassesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("conceptclasses", 3, contentLoader);
  }

  @Bean
  Loader conceptSourcesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("conceptsources", 4, contentLoader);
  }

  @Bean
  Loader metadataSharingInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("metadatasharing", 5, contentLoader);
  }

  @Bean
  Loader visitTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("visittypes", 6, contentLoader);
  }

  @Bean
  Loader patientIdentifierTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("patientidentifiertypes", 7, contentLoader);
  }

  @Bean
  Loader relationshipTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("relationshiptypes", 8, contentLoader);
  }

  @Bean
  Loader privilegesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("privileges", 10, contentLoader);
  }

  @Bean
  Loader encounterTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("encountertypes", 11, contentLoader);
  }

  @Bean
  Loader encounterRolesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("encounterroles", 12, contentLoader);
  }

  @Bean
  Loader locationTagsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("locationtags", 9, contentLoader);
  }

  @Bean
  Loader rolesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("roles", 13, contentLoader);
  }

  @Bean
  Loader globalPropertiesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("globalproperties", 14, contentLoader);
  }

  @Bean
  Loader addressHierarchyInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("addresshierarchy", 16, contentLoader);
  }

  @Bean
  Loader attributeTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("attributetypes", 15, contentLoader);
  }

  @Bean
  Loader locationsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("locations", 18, contentLoader);
  }

  @Bean
  Loader orderTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("ordertypes", 40, contentLoader);
  }

  @Bean
  Loader appointmentSpecialitiesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("appointmentspecialities", 41, contentLoader);
  }

  @Bean
  Loader appointmentServiceDefinitionsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader(
        "appointmentservicedefinitions", 42, contentLoader);
  }

  @Bean
  Loader billableServicesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("billableservices", 26, contentLoader);
  }

  @Bean
  Loader paymentModesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("paymentmodes", 27, contentLoader);
  }

  @Bean
  Loader cashPointsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("cashpoints", 28, contentLoader);
  }

  @Bean
  Loader personAttributeTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("personattributetypes", 35, contentLoader);
  }

  @Bean
  Loader identifierSourcesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("idgen", 36, contentLoader);
  }

  @Bean
  Loader autoGenerationOptionsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("autogenerationoptions", 37, contentLoader);
  }

  @Bean
  Loader metadataSetsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("metadatasets", 46, contentLoader);
  }

  @Bean
  Loader metadataTermMappingsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("metadatatermmappings", 48, contentLoader);
  }

  @Bean
  Loader cohortTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("cohorttypes", 49, contentLoader);
  }

  @Bean
  Loader cohortAttributeTypesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("cohortattributetypes", 50, contentLoader);
  }

  @Bean
  Loader fhirConceptSourcesInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("fhirconceptsources", 51, contentLoader);
  }

  @Bean
  Loader fhirPatientIdentifierSystemsInitializerLoader(StaticSihsalusContentLoader contentLoader) {
    return new StaticSihsalusContentDomainLoader("fhirpatientidentifiersystems", 52, contentLoader);
  }

  @Bean
  SmartInitializingSingleton sihsalusContentPackageInitializer(
      InitializerService initializerService) {
    return () -> {
      String startupLoadingMode = initializerService.getInitializerConfig().getStartupLoadingMode();
      if (PROPS_STARTUP_LOAD_DISABLED.equalsIgnoreCase(startupLoadingMode)) {
        return;
      }
      try {
        initializerService.loadUnsafe(
            true, PROPS_STARTUP_LOAD_FAIL_ON_ERROR.equalsIgnoreCase(startupLoadingMode));
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load SIH Salus initializer content.", e);
      }
    };
  }
}
