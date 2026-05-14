package org.sihsalus.module.calculation;

import java.util.List;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.calculation.ImplementationConfiguredCalculationProvider;
import org.openmrs.calculation.api.CalculationRegistrationService;
import org.openmrs.calculation.api.CalculationRegistrationServiceImpl;
import org.openmrs.calculation.db.CalculationRegistrationDAO;
import org.openmrs.calculation.db.HibernateCalculationRegistrationDAO;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.patient.PatientCalculationServiceImpl;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusCalculationConfiguration {

    @Bean
    HibernateMappingContributor calculationHibernateMappingContributor() {
        return () -> List.of("org/openmrs/calculation/CalculationRegistration.hbm.xml");
    }

    @Bean
    CalculationRegistrationDAO calculationRegistrationDao(DbSessionFactory dbSessionFactory) {
        HibernateCalculationRegistrationDAO dao = new HibernateCalculationRegistrationDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    CalculationRegistrationService calculationRegistrationService(CalculationRegistrationDAO calculationRegistrationDao) {
        CalculationRegistrationServiceImpl service = new CalculationRegistrationServiceImpl();
        service.setDao(calculationRegistrationDao);
        return service;
    }

    @Bean
    PatientCalculationService patientCalculationService() {
        return new PatientCalculationServiceImpl();
    }

    @Bean
    ImplementationConfiguredCalculationProvider implementationConfiguredCalculationProvider() {
        return new ImplementationConfiguredCalculationProvider();
    }

    @Bean
    SmartInitializingSingleton calculationServiceRegistrar(
            ServiceContext serviceContext,
            PatientCalculationService patientCalculationService,
            CalculationRegistrationService calculationRegistrationService) {
        return () -> {
            serviceContext.setService(PatientCalculationService.class, patientCalculationService);
            serviceContext.setService(CalculationRegistrationService.class, calculationRegistrationService);
        };
    }
}
