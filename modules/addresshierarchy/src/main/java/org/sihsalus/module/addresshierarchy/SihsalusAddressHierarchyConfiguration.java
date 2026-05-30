package org.sihsalus.module.addresshierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.openmrs.api.EventListeners;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.addresshierarchy.AddressCacheResetSupport;
import org.openmrs.module.addresshierarchy.db.AddressHierarchyDAO;
import org.openmrs.module.addresshierarchy.db.hibernate.HibernateAddressHierarchyDAO;
import org.openmrs.module.addresshierarchy.i18n.DisabledI18nCache;
import org.openmrs.module.addresshierarchy.i18n.I18nCache;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyServiceImpl;
import org.openmrs.module.addresshierarchy.validator.AddressHierarchyLevelValidator;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.openmrs.module.addresshierarchy.web.controller")
public class SihsalusAddressHierarchyConfiguration {

  private static final Logger log =
      LoggerFactory.getLogger(SihsalusAddressHierarchyConfiguration.class);

  @Bean
  HibernateMappingContributor addressHierarchyHibernateMappingContributor() {
    return () ->
        List.of(
            "org/openmrs/module/addresshierarchy/AddressHierarchyLevel.hbm.xml",
            "org/openmrs/module/addresshierarchy/AddressHierarchyEntry.hbm.xml",
            "org/openmrs/module/addresshierarchy/AddressToEntryMap.hbm.xml");
  }

  @Bean
  AddressHierarchyDAO addressHierarchyDAO(SessionFactory sessionFactory) {
    HibernateAddressHierarchyDAO dao = new HibernateAddressHierarchyDAO();
    dao.setSessionFactory(sessionFactory);
    return dao;
  }

  @Bean
  I18nCache addressHierarchyI18nCache() {
    return new DisabledI18nCache();
  }

  @Bean
  AddressHierarchyService addressHierarchyService(
      AddressHierarchyDAO addressHierarchyDAO, I18nCache i18nCache) {
    AddressHierarchyServiceImpl service = new AddressHierarchyServiceImpl();
    service.setAddressHierarchyDAO(addressHierarchyDAO);
    service.setI18nCache(i18nCache);
    return service;
  }

  @Bean
  SmartInitializingSingleton addressHierarchyServiceRegistrar(
      ServiceContext serviceContext, AddressHierarchyService addressHierarchyService) {
    return () -> serviceContext.setService(AddressHierarchyService.class, addressHierarchyService);
  }

  @Bean
  AddressCacheResetSupport addressCacheResetSupport() {
    return new AddressCacheResetSupport();
  }

  @Bean
  SmartInitializingSingleton addressHierarchyGlobalPropertyListenerRegistrar(
      EventListeners eventListeners, AddressCacheResetSupport addressCacheResetSupport) {
    return () ->
        eventListeners.setGlobalPropertyListeners(
            new ArrayList<>(List.of(addressCacheResetSupport)));
  }

  @Bean
  AddressHierarchyLevelValidator addressHierarchyLevelValidator() {
    return new AddressHierarchyLevelValidator();
  }

  @Bean
  SmartInitializingSingleton addressHierarchyStartupInitializer(
      AddressHierarchyService addressHierarchyService,
      @Value("${sihsalus.addresshierarchy.initialize-on-startup:false}") boolean enabled) {
    return () -> {
      if (enabled) {
        addressHierarchyService.initializeFullAddressCache();
        addressHierarchyService.initI18nCache();
      }
    };
  }

  @Bean(destroyMethod = "shutdown")
  ScheduledExecutorService addressHierarchyCacheScheduler(
      AddressHierarchyService addressHierarchyService,
      @Value("${sihsalus.addresshierarchy.cache-scheduler.enabled:false}") boolean enabled,
      @Value("${sihsalus.addresshierarchy.cache-scheduler.initial-delay-ms:1200000}")
          long initialDelayMs,
      @Value("${sihsalus.addresshierarchy.cache-scheduler.period-ms:600000}") long periodMs) {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    if (enabled) {
      executor.scheduleWithFixedDelay(
          () -> initializeFullAddressCache(addressHierarchyService),
          initialDelayMs,
          periodMs,
          TimeUnit.MILLISECONDS);
    }
    return executor;
  }

  private void initializeFullAddressCache(AddressHierarchyService addressHierarchyService) {
    try {
      addressHierarchyService.initializeFullAddressCache();
    } catch (Exception e) {
      log.warn("Failed to initialize address hierarchy full address cache", e);
    }
  }
}
