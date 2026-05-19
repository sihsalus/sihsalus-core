package org.sihsalus.module.patientflags;

import java.util.List;
import org.openmrs.api.ConditionService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
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
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusPatientFlagsConfiguration {

    @Bean
    HibernateMappingContributor patientFlagsHibernateMappingContributor() {
        return () -> List.of(
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
    SmartInitializingSingleton patientFlagsServiceRegistrar(ServiceContext serviceContext, FlagService flagService) {
        return () -> serviceContext.setService(FlagService.class, flagService);
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
}
