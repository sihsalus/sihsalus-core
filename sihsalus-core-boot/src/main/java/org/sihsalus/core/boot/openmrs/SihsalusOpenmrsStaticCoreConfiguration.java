package org.sihsalus.core.boot.openmrs;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.hibernate.SessionFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.CohortService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ConditionService;
import org.openmrs.api.DatatypeService;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.EventListeners;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.MedicationDispenseService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OpenmrsApplicationContextConfig;
import org.openmrs.api.OrderService;
import org.openmrs.api.OrderSetService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.SerializationService;
import org.openmrs.api.UserService;
import org.openmrs.api.VisitService;
import org.openmrs.api.cache.CacheConfig;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.db.hibernate.HibernateSessionFactoryBean;
import org.openmrs.api.impl.GlobalLocaleList;
import org.openmrs.api.impl.PersonNameGlobalPropertyListener;
import org.openmrs.hl7.HL7Service;
import org.openmrs.logging.LoggingConfigurationGlobalPropertyListener;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.notification.AlertService;
import org.openmrs.notification.MessageService;
import org.openmrs.util.ConfigUtil;
import org.openmrs.util.HttpClient;
import org.openmrs.util.LocaleUtility;
import org.openmrs.util.LocationUtility;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

@Configuration
@Import({OpenmrsApplicationContextConfig.class, CacheConfig.class})
@ComponentScan(
        basePackages = {
            "org.openmrs.aop",
            "org.openmrs.api.impl",
            "org.openmrs.api.db.hibernate",
            "org.openmrs.api.db.hibernate.search.session",
            "org.openmrs.api.db.hibernate.search.lucene",
            "org.openmrs.api.storage",
            "org.openmrs.api.stream",
            "org.openmrs.customdatatype.datatype",
            "org.openmrs.hl7.db.hibernate",
            "org.openmrs.hl7.handler",
            "org.openmrs.hl7.impl",
            "org.openmrs.messagesource.impl",
            "org.openmrs.notification.db.hibernate",
            "org.openmrs.notification.impl",
            "org.openmrs.obs.handler",
            "org.openmrs.patient.impl",
            "org.openmrs.scheduler.db.hibernate",
            "org.openmrs.serialization",
            "org.openmrs.validator"
        },
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "org\\.openmrs\\.api\\.db\\.hibernate\\.search\\.elasticsearch\\..*"))
public class SihsalusOpenmrsStaticCoreConfiguration {

    @Bean
    static BeanFactoryPostProcessor openmrsRuntimePropertiesConfigurer() {
        return new OpenmrsRuntimePropertiesConfigurer();
    }

    @Bean(name = "liquibase")
    SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
        return liquibase;
    }

    @Bean(name = "sessionFactory")
    @DependsOn("liquibase")
    HibernateSessionFactoryBean sessionFactory(ObjectProvider<HibernateMappingContributor> mappingContributors) {
        HibernateSessionFactoryBean sessionFactory = new HibernateSessionFactoryBean();
        sessionFactory.setConfigLocations(new ClassPathResource("hibernate.cfg.xml"));
        sessionFactory.setPackagesToScan("org.openmrs");
        String[] mappingResources =
                mappingContributors.orderedStream()
                        .map(HibernateMappingContributor::mappingResources)
                        .flatMap(List::stream)
                        .toArray(String[]::new);
        sessionFactory.setMappingResources(mappingResources);
        return sessionFactory;
    }

    @Bean(name = "dbSessionFactory")
    DbSessionFactory dbSessionFactory(SessionFactory sessionFactory) {
        return new DbSessionFactory(sessionFactory);
    }

    @Bean
    GlobalLocaleList globalLocaleList() {
        return new GlobalLocaleList();
    }

    @Bean
    LocaleUtility localeUtility() {
        return new LocaleUtility();
    }

    @Bean
    LocationUtility locationUtility() {
        return new LocationUtility();
    }

    @Bean
    ConfigUtil configUtilGlobalPropertyListener() {
        return new ConfigUtil();
    }

    @Bean
    PersonNameGlobalPropertyListener personNameGlobalPropertyListener() {
        return new PersonNameGlobalPropertyListener();
    }

    @Bean
    LoggingConfigurationGlobalPropertyListener loggingConfigurationGlobalPropertyListener() {
        return new LoggingConfigurationGlobalPropertyListener();
    }

    @Bean
    EventListeners openmrsEventListeners(
            LocaleUtility localeUtility,
            LocationUtility locationUtility,
            ConfigUtil configUtilGlobalPropertyListener,
            PersonNameGlobalPropertyListener personNameGlobalPropertyListener,
            LoggingConfigurationGlobalPropertyListener loggingConfigurationGlobalPropertyListener,
            GlobalLocaleList globalLocaleList) {
        EventListeners eventListeners = new EventListeners();
        eventListeners.setGlobalPropertyListenersToEmpty(false);
        eventListeners.setGlobalPropertyListeners(
                new ArrayList<>(
                        java.util.List.of(
                        localeUtility,
                        locationUtility,
                        configUtilGlobalPropertyListener,
                        personNameGlobalPropertyListener,
                        loggingConfigurationGlobalPropertyListener,
                        globalLocaleList)));
        return eventListeners;
    }

    @Bean
    HttpClient implementationIdHttpClient() throws MalformedURLException {
        return new HttpClient("https://implementation.openmrs.org");
    }

    @Bean(name = "serviceContext", destroyMethod = "destroyInstance")
    ServiceContext serviceContext(
            PatientService patientService,
            PersonService personService,
            ConceptService conceptService,
            UserService userService,
            ObsService obsService,
            EncounterService encounterService,
            LocationService locationService,
            OrderService orderService,
            ConditionService conditionService,
            DiagnosisService diagnosisService,
            MedicationDispenseService medicationDispenseService,
            OrderSetService orderSetService,
            FormService formService,
            AdministrationService administrationService,
            DatatypeService datatypeService,
            ProgramWorkflowService programWorkflowService,
            CohortService cohortService,
            ObjectProvider<MessageService> messageService,
            SerializationService serializationService,
            ObjectProvider<AlertService> alertService,
            ObjectProvider<HL7Service> hl7Service,
            MessageSourceService messageSourceService,
            VisitService visitService,
            ProviderService providerService) {
        ServiceContext serviceContext = ServiceContext.getInstance();
        serviceContext.setPatientService(patientService);
        serviceContext.setPersonService(personService);
        serviceContext.setConceptService(conceptService);
        serviceContext.setUserService(userService);
        serviceContext.setObsService(obsService);
        serviceContext.setEncounterService(encounterService);
        serviceContext.setLocationService(locationService);
        serviceContext.setOrderService(orderService);
        serviceContext.setConditionService(conditionService);
        serviceContext.setDiagnosisService(diagnosisService);
        serviceContext.setMedicationDispenseService(medicationDispenseService);
        serviceContext.setOrderSetService(orderSetService);
        serviceContext.setFormService(formService);
        serviceContext.setAdministrationService(administrationService);
        serviceContext.setDatatypeService(datatypeService);
        serviceContext.setProgramWorkflowService(programWorkflowService);
        serviceContext.setCohortService(cohortService);
        messageService.ifAvailable(serviceContext::setMessageService);
        serviceContext.setSerializationService(serializationService);
        alertService.ifAvailable(serviceContext::setAlertService);
        hl7Service.ifAvailable(serviceContext::setHl7Service);
        serviceContext.setMessageSourceService(messageSourceService);
        serviceContext.setVisitService(visitService);
        serviceContext.setProviderService(providerService);
        return serviceContext;
    }

    @Bean(name = "context", initMethod = "setAuthenticationScheme")
    Context openmrsContext(ServiceContext serviceContext, ContextDAO contextDAO) {
        Context context = new Context();
        context.setServiceContext(serviceContext);
        context.setContextDAO(contextDAO);
        return context;
    }
}
