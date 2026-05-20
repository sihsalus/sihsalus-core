package org.sihsalus.module.datafilter;

import org.hibernate.SessionFactory;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.datafilter.impl.api.DataFilterService;
import org.openmrs.module.datafilter.impl.api.db.DataFilterDAO;
import org.openmrs.module.datafilter.impl.api.db.hibernate.HibernateDataFilterDAO;
import org.openmrs.module.datafilter.impl.api.impl.DataFilterServiceImpl;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusDataFilterConfiguration {

    @Bean
    DataFilterDAO dataFilterDAO(SessionFactory sessionFactory) {
        HibernateDataFilterDAO dao = new HibernateDataFilterDAO();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    DataFilterService dataFilterService(DataFilterDAO dataFilterDAO) {
        DataFilterServiceImpl service = new DataFilterServiceImpl();
        service.setDao(dataFilterDAO);
        return service;
    }

    @Bean
    SmartInitializingSingleton dataFilterServiceRegistrar(
            ServiceContext serviceContext, DataFilterService dataFilterService) {
        return () -> serviceContext.setService(DataFilterService.class, dataFilterService);
    }
}
