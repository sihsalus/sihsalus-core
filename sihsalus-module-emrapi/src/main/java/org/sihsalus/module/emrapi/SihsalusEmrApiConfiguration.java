package org.sihsalus.module.emrapi;

import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.account.AccountService;
import org.openmrs.module.emrapi.account.AccountServiceImpl;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.adt.AdtServiceImpl;
import org.openmrs.module.emrapi.concept.EmrConceptDAO;
import org.openmrs.module.emrapi.concept.EmrConceptService;
import org.openmrs.module.emrapi.concept.EmrConceptServiceImpl;
import org.openmrs.module.emrapi.concept.HibernateEmrConceptDAO;
import org.openmrs.module.emrapi.db.DbSessionDAO;
import org.openmrs.module.emrapi.db.DbSessionDAOImpl;
import org.openmrs.module.emrapi.db.DbSessionUtil;
import org.openmrs.module.emrapi.db.EmrApiDAO;
import org.openmrs.module.emrapi.db.EmrApiDAOImpl;
import org.openmrs.module.emrapi.db.EmrEncounterDAO;
import org.openmrs.module.emrapi.db.HibernateEmrEncounterDAO;
import org.openmrs.module.emrapi.diagnosis.CoreDiagnosisService;
import org.openmrs.module.emrapi.diagnosis.DiagnosisService;
import org.openmrs.module.emrapi.diagnosis.DiagnosisServiceImpl;
import org.openmrs.module.emrapi.diagnosis.EmrDiagnosisDAO;
import org.openmrs.module.emrapi.diagnosis.EmrDiagnosisDAOImpl;
import org.openmrs.module.emrapi.diagnosis.ObsGroupDiagnosisService;
import org.openmrs.module.emrapi.disposition.DispositionService;
import org.openmrs.module.emrapi.disposition.DispositionServiceImpl;
import org.openmrs.module.emrapi.domainwrapper.DomainWrapperFactory;
import org.openmrs.module.emrapi.exitfromcare.ExitFromCareService;
import org.openmrs.module.emrapi.exitfromcare.ExitFromCareServiceImpl;
import org.openmrs.module.emrapi.maternal.MaternalService;
import org.openmrs.module.emrapi.maternal.MaternalServiceImpl;
import org.openmrs.module.emrapi.patient.EmrPatientDAO;
import org.openmrs.module.emrapi.patient.EmrPatientProfileService;
import org.openmrs.module.emrapi.patient.EmrPatientProfileServiceImpl;
import org.openmrs.module.emrapi.patient.EmrPatientService;
import org.openmrs.module.emrapi.patient.EmrPatientServiceImpl;
import org.openmrs.module.emrapi.patient.HibernateEmrPatientDAO;
import org.openmrs.module.emrapi.person.image.EmrPersonImageService;
import org.openmrs.module.emrapi.person.image.EmrPersonImageServiceImpl;
import org.openmrs.module.emrapi.procedure.HibernateProcedureDAO;
import org.openmrs.module.emrapi.procedure.ProcedureDAO;
import org.openmrs.module.emrapi.procedure.ProcedureService;
import org.openmrs.module.emrapi.procedure.ProcedureServiceImpl;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusEmrApiConfiguration {

    @Bean
    EmrApiProperties emrApiProperties(
            MetadataMappingService metadataMappingService,
            ConceptService conceptService,
            AdministrationService adminService,
            LocationService locationService,
            UserService userService,
            PersonService personService,
            ProviderService providerService) {
        EmrApiProperties properties = new EmrApiProperties();
        properties.setMetadataMappingService(metadataMappingService);
        properties.setConceptService(conceptService);
        properties.setAdministrationService(adminService);
        properties.setLocationService(locationService);
        properties.setUserService(userService);
        properties.setPersonService(personService);
        properties.setProviderService(providerService);
        return properties;
    }

    @Bean
    EmrApiDAO emrApiDAO(DbSessionFactory dbSessionFactory) {
        EmrApiDAOImpl dao = new EmrApiDAOImpl();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    EmrConceptDAO emrConceptDAO(DbSessionFactory dbSessionFactory) {
        HibernateEmrConceptDAO dao = new HibernateEmrConceptDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    EmrConceptService emrConceptService(
            EmrConceptDAO emrConceptDAO, EmrApiProperties emrApiProperties, ConceptService conceptService) {
        EmrConceptServiceImpl service = new EmrConceptServiceImpl();
        service.setDao(emrConceptDAO);
        service.setEmrApiProperties(emrApiProperties);
        service.setConceptService(conceptService);
        return service;
    }

    @Bean
    DispositionService dispositionService(ConceptService conceptService, EmrConceptService emrConceptService) {
        return new DispositionServiceImpl(conceptService, emrConceptService);
    }

    @Bean
    SmartInitializingSingleton emrApiPropertiesDispositionBinder(
            EmrApiProperties emrApiProperties, DispositionService dispositionService) {
        return () -> emrApiProperties.setDispositionService(dispositionService);
    }

    @Bean
    DomainWrapperFactory domainWrapperFactory() {
        return new DomainWrapperFactory();
    }

    @Bean
    AdtService adtService(
            EmrApiProperties emrApiProperties,
            EncounterService encounterService,
            VisitService visitService,
            LocationService locationService,
            DispositionService dispositionService,
            EmrApiDAO emrApiDAO,
            EmrConceptService emrConceptService,
            ProviderService providerService,
            PatientService patientService,
            DomainWrapperFactory domainWrapperFactory) {
        AdtServiceImpl service = new AdtServiceImpl();
        service.setEmrApiProperties(emrApiProperties);
        service.setEncounterService(encounterService);
        service.setVisitService(visitService);
        service.setLocationService(locationService);
        service.setDispositionService(dispositionService);
        service.setEmrApiDAO(emrApiDAO);
        service.setEmrConceptService(emrConceptService);
        service.setProviderService(providerService);
        service.setPatientService(patientService);
        service.setDomainWrapperFactory(domainWrapperFactory);
        service.setPatientMergeActions(List.of());
        return service;
    }

    @Bean
    EmrPatientDAO emrPatientDAO(DbSessionFactory dbSessionFactory, EmrApiProperties emrApiProperties) {
        HibernateEmrPatientDAO dao = new HibernateEmrPatientDAO();
        dao.setSessionFactory(dbSessionFactory);
        dao.setEmrApiProperties(emrApiProperties);
        return dao;
    }

    @Bean
    EmrPatientService emrPatientService(
            EmrPatientDAO emrPatientDAO,
            EmrApiProperties emrApiProperties,
            PatientService patientService,
            AdtService adtService) {
        EmrPatientServiceImpl service = new EmrPatientServiceImpl();
        service.setDao(emrPatientDAO);
        service.setEmrApiProperties(emrApiProperties);
        service.setPatientService(patientService);
        service.setAdtService(adtService);
        return service;
    }

    @Bean
    AccountService accountService(
            UserService userService,
            PersonService personService,
            ProviderService providerService,
            DomainWrapperFactory domainWrapperFactory,
            EmrApiProperties emrApiProperties,
            EmrApiDAO emrApiDAO) {
        AccountServiceImpl service = new AccountServiceImpl();
        service.setUserService(userService);
        service.setPersonService(personService);
        service.setProviderService(providerService);
        service.setDomainWrapperFactory(domainWrapperFactory);
        service.setEmrApiProperties(emrApiProperties);
        service.setEmrApiDAO(emrApiDAO);
        return service;
    }

    @Bean
    MaternalService maternalService(AdtService adtService, EmrApiProperties emrApiProperties, EmrApiDAO emrApiDAO) {
        MaternalServiceImpl service = new MaternalServiceImpl();
        service.setAdtService(adtService);
        service.setEmrApiProperties(emrApiProperties);
        service.setEmrApiDAO(emrApiDAO);
        return service;
    }

    @Bean
    ExitFromCareService exitFromCareService(
            EmrApiProperties emrApiProperties,
            VisitService visitService,
            PatientService patientService,
            ProgramWorkflowService programWorkflowService,
            AdtService adtService) {
        ExitFromCareServiceImpl service = new ExitFromCareServiceImpl();
        service.setEmrApiProperties(emrApiProperties);
        service.setVisitService(visitService);
        service.setPatientService(patientService);
        service.setProgramWorkflowService(programWorkflowService);
        service.setAdtService(adtService);
        return service;
    }

    @Bean
    EmrPersonImageService emrPersonImageService(EmrApiProperties emrApiProperties) {
        EmrPersonImageServiceImpl service = new EmrPersonImageServiceImpl();
        service.setEmrApiProperties(emrApiProperties);
        return service;
    }

    @Bean
    EmrPatientProfileService emrPatientProfileService(
            PatientService patientService, PersonService personService, EmrPersonImageService emrPersonImageService) {
        EmrPatientProfileServiceImpl service = new EmrPatientProfileServiceImpl();
        service.setPatientService(patientService);
        service.setPersonService(personService);
        service.setEmrPersonImageService(emrPersonImageService);
        return service;
    }

    @Bean
    EmrDiagnosisDAO emrDiagnosisDAO(DbSessionFactory dbSessionFactory) {
        EmrDiagnosisDAOImpl dao = new EmrDiagnosisDAOImpl();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    CoreDiagnosisService coreDiagnosisService(EmrDiagnosisDAO emrDiagnosisDAO, EmrApiDAO emrApiDAO) {
        CoreDiagnosisService service = new CoreDiagnosisService();
        service.setEmrDiagnosisDAO(emrDiagnosisDAO);
        service.setEmrApiDAO(emrApiDAO);
        return service;
    }

    @Bean
    ObsGroupDiagnosisService obsGroupDiagnosisService(
            EmrApiProperties emrApiProperties,
            ObsService obsService,
            EncounterService encounterService,
            EmrApiDAO emrApiDAO) {
        ObsGroupDiagnosisService service = new ObsGroupDiagnosisService();
        service.setEmrApiProperties(emrApiProperties);
        service.setObsService(obsService);
        service.setEncounterService(encounterService);
        service.setEmrApiDAO(emrApiDAO);
        return service;
    }

    @Bean
    DiagnosisService emrDiagnosisService(
            CoreDiagnosisService coreDiagnosisService,
            ObsGroupDiagnosisService obsGroupDiagnosisService,
            AdministrationService adminService) {
        DiagnosisServiceImpl service = new DiagnosisServiceImpl();
        service.setCoreDiagnosisService(coreDiagnosisService);
        service.setObsGroupDiagnosisService(obsGroupDiagnosisService);
        service.setAdminService(adminService);
        return service;
    }

    @Bean
    ProcedureDAO procedureDAO(SessionFactory sessionFactory) {
        return new HibernateProcedureDAO(sessionFactory);
    }

    @Bean
    ProcedureService procedureService(ProcedureDAO procedureDAO) {
        ProcedureServiceImpl service = new ProcedureServiceImpl();
        service.setProcedureDAO(procedureDAO);
        return service;
    }

    @Bean
    EmrEncounterDAO emrEncounterDAO(DbSessionFactory dbSessionFactory) {
        HibernateEmrEncounterDAO dao = new HibernateEmrEncounterDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    DbSessionDAO emrApiDbSessionDAO(DbSessionFactory dbSessionFactory) {
        DbSessionDAOImpl dao = new DbSessionDAOImpl();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    DbSessionUtil dbSessionUtil(DbSessionDAO emrApiDbSessionDAO) {
        DbSessionUtil util = new DbSessionUtil();
        util.setDbSessionDAO(emrApiDbSessionDAO);
        return util;
    }

    @Bean
    SmartInitializingSingleton emrApiServiceRegistrar(
            ServiceContext serviceContext,
            AccountService accountService,
            AdtService adtService,
            MaternalService maternalService,
            ExitFromCareService exitFromCareService,
            EmrConceptService emrConceptService,
            EmrPatientService emrPatientService,
            EmrPersonImageService emrPersonImageService,
            EmrPatientProfileService emrPatientProfileService,
            DispositionService dispositionService,
            DiagnosisService emrDiagnosisService,
            ProcedureService procedureService) {
        return () -> {
            serviceContext.setService(AccountService.class, accountService);
            serviceContext.setService(AdtService.class, adtService);
            serviceContext.setService(MaternalService.class, maternalService);
            serviceContext.setService(ExitFromCareService.class, exitFromCareService);
            serviceContext.setService(EmrConceptService.class, emrConceptService);
            serviceContext.setService(EmrPatientService.class, emrPatientService);
            serviceContext.setService(EmrPersonImageService.class, emrPersonImageService);
            serviceContext.setService(EmrPatientProfileService.class, emrPatientProfileService);
            serviceContext.setService(DispositionService.class, dispositionService);
            serviceContext.setService(DiagnosisService.class, emrDiagnosisService);
            serviceContext.setService(ProcedureService.class, procedureService);
        };
    }
}
