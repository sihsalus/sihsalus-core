package org.sihsalus.module.patientflags;

import java.util.List;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.patientflags.api.FlagService;
import org.openmrs.module.patientflags.api.impl.FlagServiceImpl;
import org.openmrs.module.patientflags.db.FlagDAO;
import org.openmrs.module.patientflags.db.hibernate.HibernateFlagDAO;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class SihsalusPatientFlagsConfiguration {

    @Bean(name = "patientFlagsLiquibase")
    @DependsOn("liquibase")
    SpringLiquibase patientFlagsLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/org/openmrs/module/patientflags/liquibase.xml");
        return liquibase;
    }

    @Bean
    @DependsOn("patientFlagsLiquibase")
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
    SmartInitializingSingleton patientFlagsServiceRegistrar(ServiceContext serviceContext, FlagService flagService) {
        return () -> serviceContext.setService(FlagService.class, flagService);
    }
}
