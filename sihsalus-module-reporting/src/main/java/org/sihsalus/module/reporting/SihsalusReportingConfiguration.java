package org.sihsalus.module.reporting;

import java.util.ArrayList;
import java.util.List;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Handler;
import org.openmrs.api.EventListeners;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.SerializedObjectDAO;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.reporting.ReportingConstants;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionServiceImpl;
import org.openmrs.module.reporting.cohort.query.db.CohortQueryDAO;
import org.openmrs.module.reporting.cohort.query.db.hibernate.HibernateCohortQueryDAO;
import org.openmrs.module.reporting.cohort.query.service.CohortQueryService;
import org.openmrs.module.reporting.cohort.query.service.CohortQueryServiceImpl;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDataDefinition;
import org.openmrs.module.reporting.data.encounter.service.EncounterDataService;
import org.openmrs.module.reporting.data.encounter.service.EncounterDataServiceImpl;
import org.openmrs.module.reporting.data.obs.definition.ObsDataDefinition;
import org.openmrs.module.reporting.data.obs.service.ObsDataService;
import org.openmrs.module.reporting.data.obs.service.ObsDataServiceImpl;
import org.openmrs.module.reporting.data.patient.definition.PatientDataDefinition;
import org.openmrs.module.reporting.data.patient.service.PatientDataService;
import org.openmrs.module.reporting.data.patient.service.PatientDataServiceImpl;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.data.person.service.PersonDataService;
import org.openmrs.module.reporting.data.person.service.PersonDataServiceImpl;
import org.openmrs.module.reporting.data.visit.service.VisitDataService;
import org.openmrs.module.reporting.data.visit.service.VisitDataServiceImpl;
import org.openmrs.module.reporting.definition.service.SerializedDefinitionService;
import org.openmrs.module.reporting.definition.service.SerializedDefinitionServiceImpl;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.service.DataSetDefinitionService;
import org.openmrs.module.reporting.dataset.definition.service.DataSetDefinitionServiceImpl;
import org.openmrs.module.reporting.dataset.query.service.BaseDataSetQueryService;
import org.openmrs.module.reporting.dataset.query.service.DataSetQueryService;
import org.openmrs.module.reporting.dataset.query.service.db.DataSetQueryDAO;
import org.openmrs.module.reporting.dataset.query.service.db.HibernateDataSetQueryDAO;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.openmrs.module.reporting.evaluation.service.EvaluationServiceImpl;
import org.openmrs.module.reporting.indicator.Indicator;
import org.openmrs.module.reporting.indicator.dimension.Dimension;
import org.openmrs.module.reporting.indicator.dimension.service.DimensionService;
import org.openmrs.module.reporting.indicator.dimension.service.DimensionServiceImpl;
import org.openmrs.module.reporting.indicator.service.IndicatorService;
import org.openmrs.module.reporting.indicator.service.IndicatorServiceImpl;
import org.openmrs.module.reporting.query.encounter.definition.EncounterQuery;
import org.openmrs.module.reporting.query.encounter.service.EncounterQueryService;
import org.openmrs.module.reporting.query.encounter.service.EncounterQueryServiceImpl;
import org.openmrs.module.reporting.query.obs.definition.ObsQuery;
import org.openmrs.module.reporting.query.obs.service.ObsQueryService;
import org.openmrs.module.reporting.query.obs.service.ObsQueryServiceImpl;
import org.openmrs.module.reporting.query.person.definition.PersonQuery;
import org.openmrs.module.reporting.query.person.service.PersonQueryService;
import org.openmrs.module.reporting.query.person.service.PersonQueryServiceImpl;
import org.openmrs.module.reporting.query.visit.service.VisitQueryService;
import org.openmrs.module.reporting.query.visit.service.VisitQueryServiceImpl;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionServiceImpl;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.report.service.ReportServiceImpl;
import org.openmrs.module.reporting.report.service.db.HibernateReportDAO;
import org.openmrs.module.reporting.report.service.db.ReportDAO;
import org.openmrs.module.reporting.report.task.DeleteOldReportsTask;
import org.openmrs.module.reporting.report.task.PersistCachedReportsTask;
import org.openmrs.module.reporting.report.task.QueueScheduledReportsTask;
import org.openmrs.module.reporting.report.task.ReportingTask;
import org.openmrs.module.reporting.report.task.ReportingTimerTask;
import org.openmrs.module.reporting.report.task.RunQueuedReportsTask;
import org.openmrs.module.reporting.serializer.ReportingSerializer;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackageClasses = ReportingConstants.class,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Handler.class))
public class SihsalusReportingConfiguration {

    @Bean
    HibernateMappingContributor reportingHibernateMappingContributor() {
        return () -> List.of("ReportDesign.hbm.xml", "ReportRequest.hbm.xml");
    }

    @Bean
    SmartInitializingSingleton reportingTimerTaskStaticInitializer() {
        return () -> {
            ReportingTimerTask.setDaemonToken(null);
            ReportingTimerTask.setEnabled(true);
        };
    }

    @Bean
    ReportingSerializer reportingSerializer() throws Exception {
        return new ReportingSerializer();
    }

    @Bean
    SmartInitializingSingleton reportingSerializedObjectTypeRegistrar(SerializedObjectDAO serializedObjectDAO) {
        return () -> reportingSupportedTypes().forEach(serializedObjectDAO::registerSupportedType);
    }

    @Bean
    SerializedDefinitionService reportingSerializedDefinitionService(
            SerializedObjectDAO serializedObjectDAO, ReportingSerializer reportingSerializer) {
        SerializedDefinitionServiceImpl service = new SerializedDefinitionServiceImpl();
        service.setDao(serializedObjectDAO);
        service.setSerializer(reportingSerializer);
        return service;
    }

    @Bean
    EvaluationService reportingEvaluationService() {
        return new EvaluationServiceImpl();
    }

    @Bean
    ReportDefinitionService reportingReportDefinitionService() {
        return new ReportDefinitionServiceImpl();
    }

    @Bean
    ReportDAO reportingReportDao(DbSessionFactory dbSessionFactory) {
        HibernateReportDAO dao = new HibernateReportDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    ReportService reportingReportService(
            ReportDAO reportingReportDao, @Qualifier("runQueuedReportsTask") ReportingTimerTask runQueuedReportsTask) {
        ReportServiceImpl service = new ReportServiceImpl();
        service.setReportDAO(reportingReportDao);
        service.setRunQueuedReportsTask(runQueuedReportsTask);
        return service;
    }

    @Bean
    PersonDataService reportingPersonDataService() {
        return new PersonDataServiceImpl();
    }

    @Bean
    PatientDataService reportingPatientDataService() {
        return new PatientDataServiceImpl();
    }

    @Bean
    VisitDataService reportingVisitDataService() {
        return new VisitDataServiceImpl();
    }

    @Bean
    EncounterDataService reportingEncounterDataService() {
        return new EncounterDataServiceImpl();
    }

    @Bean
    ObsDataService reportingObsDataService() {
        return new ObsDataServiceImpl();
    }

    @Bean
    PersonQueryService reportingPersonQueryService() {
        return new PersonQueryServiceImpl();
    }

    @Bean
    VisitQueryService reportingVisitQueryService() {
        return new VisitQueryServiceImpl();
    }

    @Bean
    EncounterQueryService reportingEncounterQueryService() {
        return new EncounterQueryServiceImpl();
    }

    @Bean
    ObsQueryService reportingObsQueryService() {
        return new ObsQueryServiceImpl();
    }

    @Bean
    CohortDefinitionService reportingCohortDefinitionService() {
        return new CohortDefinitionServiceImpl();
    }

    @Bean
    DataSetDefinitionService reportingDataSetDefinitionService() {
        return new DataSetDefinitionServiceImpl();
    }

    @Bean
    IndicatorService reportingIndicatorService() {
        return new IndicatorServiceImpl();
    }

    @Bean
    DimensionService reportingDimensionService() {
        return new DimensionServiceImpl();
    }

    @Bean
    CohortQueryDAO reportingCohortQueryDao(DbSessionFactory dbSessionFactory) {
        HibernateCohortQueryDAO dao = new HibernateCohortQueryDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    DataSetQueryDAO reportingDataSetQueryDao(DbSessionFactory dbSessionFactory) {
        HibernateDataSetQueryDAO dao = new HibernateDataSetQueryDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    CohortQueryService reportingCohortQueryService(CohortQueryDAO reportingCohortQueryDao) {
        CohortQueryServiceImpl service = new CohortQueryServiceImpl();
        service.setCohortQueryDAO(reportingCohortQueryDao);
        return service;
    }

    @Bean
    DataSetQueryService reportingDataSetQueryService(DataSetQueryDAO reportingDataSetQueryDao) {
        BaseDataSetQueryService service = new BaseDataSetQueryService();
        service.setDao(reportingDataSetQueryDao);
        return service;
    }

    @Bean
    ReportingTimerTask queueScheduledReportsTask(DbSessionFactory dbSessionFactory) {
        return reportingTimerTask(QueueScheduledReportsTask.class, dbSessionFactory);
    }

    @Bean
    ReportingTimerTask runQueuedReportsTask(DbSessionFactory dbSessionFactory) {
        return reportingTimerTask(RunQueuedReportsTask.class, dbSessionFactory);
    }

    @Bean
    ReportingTimerTask deleteOldReportsTask(DbSessionFactory dbSessionFactory) {
        return reportingTimerTask(DeleteOldReportsTask.class, dbSessionFactory);
    }

    @Bean
    ReportingTimerTask persistCachedReportsTask(DbSessionFactory dbSessionFactory) {
        return reportingTimerTask(PersistCachedReportsTask.class, dbSessionFactory);
    }

    @Bean
    ReportingConstants reportingConstants() {
        return new ReportingConstants();
    }

    @Bean
    SmartInitializingSingleton reportingGlobalPropertyListenerRegistrar(
            EventListeners eventListeners, ReportingConstants reportingConstants) {
        return () -> eventListeners.setGlobalPropertyListeners(new ArrayList<>(List.of(reportingConstants)));
    }

    @Bean
    SmartInitializingSingleton reportingServiceRegistrar(
            ServiceContext serviceContext,
            SerializedDefinitionService reportingSerializedDefinitionService,
            EvaluationService reportingEvaluationService,
            ReportDefinitionService reportingReportDefinitionService,
            ReportService reportingReportService,
            PersonDataService reportingPersonDataService,
            PatientDataService reportingPatientDataService,
            VisitDataService reportingVisitDataService,
            EncounterDataService reportingEncounterDataService,
            ObsDataService reportingObsDataService,
            PersonQueryService reportingPersonQueryService,
            VisitQueryService reportingVisitQueryService,
            EncounterQueryService reportingEncounterQueryService,
            ObsQueryService reportingObsQueryService,
            CohortDefinitionService reportingCohortDefinitionService,
            DataSetDefinitionService reportingDataSetDefinitionService,
            IndicatorService reportingIndicatorService,
            DimensionService reportingDimensionService,
            CohortQueryService reportingCohortQueryService,
            DataSetQueryService reportingDataSetQueryService) {
        return () -> {
            serviceContext.setService(SerializedDefinitionService.class, reportingSerializedDefinitionService);
            serviceContext.setService(EvaluationService.class, reportingEvaluationService);
            serviceContext.setService(ReportDefinitionService.class, reportingReportDefinitionService);
            serviceContext.setService(ReportService.class, reportingReportService);
            serviceContext.setService(PersonDataService.class, reportingPersonDataService);
            serviceContext.setService(PatientDataService.class, reportingPatientDataService);
            serviceContext.setService(VisitDataService.class, reportingVisitDataService);
            serviceContext.setService(EncounterDataService.class, reportingEncounterDataService);
            serviceContext.setService(ObsDataService.class, reportingObsDataService);
            serviceContext.setService(PersonQueryService.class, reportingPersonQueryService);
            serviceContext.setService(VisitQueryService.class, reportingVisitQueryService);
            serviceContext.setService(EncounterQueryService.class, reportingEncounterQueryService);
            serviceContext.setService(ObsQueryService.class, reportingObsQueryService);
            serviceContext.setService(CohortDefinitionService.class, reportingCohortDefinitionService);
            serviceContext.setService(DataSetDefinitionService.class, reportingDataSetDefinitionService);
            serviceContext.setService(IndicatorService.class, reportingIndicatorService);
            serviceContext.setService(DimensionService.class, reportingDimensionService);
            serviceContext.setService(CohortQueryService.class, reportingCohortQueryService);
            serviceContext.setService(DataSetQueryService.class, reportingDataSetQueryService);
        };
    }

    private ReportingTimerTask reportingTimerTask(
            Class<? extends ReportingTask> taskClass, DbSessionFactory dbSessionFactory) {
        ReportingTimerTask task = new ReportingTimerTask();
        task.setTaskClass(taskClass);
        task.setSessionFactory(dbSessionFactory);
        return task;
    }

    private List<Class<? extends OpenmrsObject>> reportingSupportedTypes() {
        return List.of(
                supportedType(PersonQuery.class),
                supportedType(CohortDefinition.class),
                supportedType(EncounterQuery.class),
                supportedType(ObsQuery.class),
                supportedType(PersonDataDefinition.class),
                supportedType(PatientDataDefinition.class),
                supportedType(EncounterDataDefinition.class),
                supportedType(ObsDataDefinition.class),
                supportedType(Indicator.class),
                supportedType(Dimension.class),
                supportedType(DataSetDefinition.class),
                supportedType(ReportDefinition.class));
    }

    @SuppressWarnings("unchecked")
    private Class<? extends OpenmrsObject> supportedType(Class<?> type) {
        return (Class<? extends OpenmrsObject>) type;
    }
}
