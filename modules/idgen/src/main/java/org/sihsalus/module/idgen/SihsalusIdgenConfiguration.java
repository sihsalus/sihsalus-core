package org.sihsalus.module.idgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.openmrs.api.EventListeners;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.idgen.IdentifierPool;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.RemoteIdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.prefixprovider.LocationBasedPrefixProvider;
import org.openmrs.module.idgen.processor.IdentifierPoolProcessor;
import org.openmrs.module.idgen.processor.IdentifierSourceProcessor;
import org.openmrs.module.idgen.processor.RemoteIdentifierSourceProcessor;
import org.openmrs.module.idgen.processor.SequentialIdentifierGeneratorProcessor;
import org.openmrs.module.idgen.service.BaseIdentifierSourceService;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.idgen.service.db.HibernateIdentifierSourceDAO;
import org.openmrs.module.idgen.service.db.IdentifierSourceDAO;
import org.openmrs.module.idgen.validator.IdentifierSourceValidator;
import org.openmrs.module.idgen.validator.LuhnMod10IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod25IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod30IdentifierValidator;
import org.openmrs.module.idgen.validator.RemoteIdentifierSourceValidator;
import org.openmrs.module.idgen.validator.SequentialIdentifierGeneratorValidator;
import org.openmrs.patient.IdentifierValidator;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.openmrs.module.idgen.web.controller")
public class SihsalusIdgenConfiguration {

  private static final Logger log = LoggerFactory.getLogger(SihsalusIdgenConfiguration.class);

  @Bean
  HibernateMappingContributor idgenHibernateMappingContributor() {
    return () -> List.of("org/openmrs/module/idgen/IdentifierSource.hbm.xml");
  }

  @Bean
  IdentifierSourceDAO identifierSourceDAO(DbSessionFactory dbSessionFactory) {
    HibernateIdentifierSourceDAO dao = new HibernateIdentifierSourceDAO();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean
  IdentifierPoolProcessor identifierPoolProcessor() {
    return new IdentifierPoolProcessor();
  }

  @Bean
  RemoteIdentifierSourceProcessor remoteIdentifierSourceProcessor() {
    return new RemoteIdentifierSourceProcessor();
  }

  @Bean
  SequentialIdentifierGeneratorProcessor sequentialIdentifierGeneratorProcessor() {
    return new SequentialIdentifierGeneratorProcessor();
  }

  @Bean
  IdentifierSourceService identifierSourceService(
      IdentifierSourceDAO identifierSourceDAO,
      IdentifierPoolProcessor identifierPoolProcessor,
      RemoteIdentifierSourceProcessor remoteIdentifierSourceProcessor,
      SequentialIdentifierGeneratorProcessor sequentialIdentifierGeneratorProcessor) {
    BaseIdentifierSourceService service = new BaseIdentifierSourceService();
    service.setDao(identifierSourceDAO);
    Map<Class<? extends IdentifierSource>, IdentifierSourceProcessor> processors =
        new LinkedHashMap<>();
    processors.put(SequentialIdentifierGenerator.class, sequentialIdentifierGeneratorProcessor);
    processors.put(RemoteIdentifierSource.class, remoteIdentifierSourceProcessor);
    processors.put(IdentifierPool.class, identifierPoolProcessor);
    service.setProcessors(processors);
    return service;
  }

  @Bean
  SmartInitializingSingleton idgenServiceRegistrar(
      ServiceContext serviceContext, IdentifierSourceService identifierSourceService) {
    return () -> serviceContext.setService(IdentifierSourceService.class, identifierSourceService);
  }

  @Bean
  LuhnMod10IdentifierValidator luhnMod10IdentifierValidator() {
    return new LuhnMod10IdentifierValidator();
  }

  @Bean
  LuhnMod25IdentifierValidator luhnMod25IdentifierValidator() {
    return new LuhnMod25IdentifierValidator();
  }

  @Bean
  LuhnMod30IdentifierValidator luhnMod30IdentifierValidator() {
    return new LuhnMod30IdentifierValidator();
  }

  @Bean
  SmartInitializingSingleton idgenIdentifierValidatorRegistrar(
      @Qualifier("identifierValidators") Map<Class<?>, IdentifierValidator> identifierValidators,
      LuhnMod10IdentifierValidator luhnMod10IdentifierValidator,
      LuhnMod25IdentifierValidator luhnMod25IdentifierValidator,
      LuhnMod30IdentifierValidator luhnMod30IdentifierValidator) {
    return () -> {
      identifierValidators.put(LuhnMod10IdentifierValidator.class, luhnMod10IdentifierValidator);
      identifierValidators.put(LuhnMod25IdentifierValidator.class, luhnMod25IdentifierValidator);
      identifierValidators.put(LuhnMod30IdentifierValidator.class, luhnMod30IdentifierValidator);
    };
  }

  @Bean
  IdentifierSourceValidator identifierSourceValidator() {
    return new IdentifierSourceValidator();
  }

  @Bean
  RemoteIdentifierSourceValidator remoteIdentifierSourceValidator() {
    return new RemoteIdentifierSourceValidator();
  }

  @Bean
  SequentialIdentifierGeneratorValidator sequentialIdentifierGeneratorValidator() {
    return new SequentialIdentifierGeneratorValidator();
  }

  @Bean
  LocationBasedPrefixProvider idgenLocationBasedPrefixProvider() {
    return new LocationBasedPrefixProvider();
  }

  @Bean
  SmartInitializingSingleton idgenGlobalPropertyListenerRegistrar(
      EventListeners eventListeners, LocationBasedPrefixProvider idgenLocationBasedPrefixProvider) {
    return () ->
        eventListeners.setGlobalPropertyListeners(
            new ArrayList<>(List.of(idgenLocationBasedPrefixProvider)));
  }

  @Bean(destroyMethod = "shutdown")
  ScheduledExecutorService idgenRefillScheduler(
      IdentifierSourceService identifierSourceService,
      @Value("${sihsalus.idgen.refill-scheduler.enabled:false}") boolean enabled,
      @Value("${sihsalus.idgen.refill-scheduler.initial-delay-ms:10000}") long initialDelayMs,
      @Value("${sihsalus.idgen.refill-scheduler.period-ms:300000}") long periodMs) {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    if (enabled) {
      executor.scheduleWithFixedDelay(
          () -> refillIdentifierPools(identifierSourceService),
          initialDelayMs,
          periodMs,
          TimeUnit.MILLISECONDS);
    }
    return executor;
  }

  private void refillIdentifierPools(IdentifierSourceService identifierSourceService) {
    for (IdentifierSource source : identifierSourceService.getAllIdentifierSources(false)) {
      if (source instanceof IdentifierPool pool && pool.isRefillWithScheduledTask()) {
        try {
          identifierSourceService.checkAndRefillIdentifierPool(pool);
        } catch (Exception e) {
          log.warn("Failed to refill identifier pool: {}", pool.getName(), e);
        }
      }
    }
  }
}
