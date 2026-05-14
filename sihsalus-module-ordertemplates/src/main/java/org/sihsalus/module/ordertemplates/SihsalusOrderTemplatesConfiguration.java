package org.sihsalus.module.ordertemplates;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ordertemplates.api.OrderTemplatesService;
import org.openmrs.module.ordertemplates.api.dao.OrderTemplatesDao;
import org.openmrs.module.ordertemplates.api.dao.impl.HibernateOrderTemplatesDao;
import org.openmrs.module.ordertemplates.api.impl.OrderTemplatesServiceImpl;
import org.openmrs.module.ordertemplates.web.controller.OrderTemplatesRestController;
import org.openmrs.module.ordertemplates.web.resource.OrderTemplatesResource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {OrderTemplatesRestController.class, OrderTemplatesResource.class})
public class SihsalusOrderTemplatesConfiguration {

    @Bean
    OrderTemplatesDao orderTemplatesDao(DbSessionFactory dbSessionFactory) {
        HibernateOrderTemplatesDao dao = new HibernateOrderTemplatesDao();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    OrderTemplatesService orderTemplatesService(OrderTemplatesDao orderTemplatesDao) {
        OrderTemplatesServiceImpl service = new OrderTemplatesServiceImpl();
        service.setDao(orderTemplatesDao);
        return service;
    }

    @Bean
    SmartInitializingSingleton orderTemplatesServiceRegistrar(
            ServiceContext serviceContext, OrderTemplatesService orderTemplatesService) {
        return () -> serviceContext.setService(OrderTemplatesService.class, orderTemplatesService);
    }
}
