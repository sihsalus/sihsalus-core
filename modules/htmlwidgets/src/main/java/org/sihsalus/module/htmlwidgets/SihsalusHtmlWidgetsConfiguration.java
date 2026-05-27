package org.sihsalus.module.htmlwidgets;

import org.openmrs.annotation.Handler;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsService;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsServiceImpl;
import org.openmrs.module.htmlwidgets.service.db.HibernateHtmlWidgetsDAO;
import org.openmrs.module.htmlwidgets.service.db.HtmlWidgetsDAO;
import org.openmrs.module.htmlwidgets.web.controller.ConceptSearchController;
import org.openmrs.module.htmlwidgets.web.handler.WidgetHandler;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackageClasses = {ConceptSearchController.class, WidgetHandler.class},
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Handler.class))
public class SihsalusHtmlWidgetsConfiguration {

  @Bean
  HtmlWidgetsDAO htmlWidgetsDao(DbSessionFactory dbSessionFactory) {
    HibernateHtmlWidgetsDAO dao = new HibernateHtmlWidgetsDAO();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean
  HtmlWidgetsService htmlWidgetsService(HtmlWidgetsDAO htmlWidgetsDao) {
    HtmlWidgetsServiceImpl service = new HtmlWidgetsServiceImpl();
    service.setDao(htmlWidgetsDao);
    return service;
  }

  @Bean
  SmartInitializingSingleton htmlWidgetsServiceRegistrar(
      ServiceContext serviceContext, HtmlWidgetsService htmlWidgetsService) {
    return () -> serviceContext.setService(HtmlWidgetsService.class, htmlWidgetsService);
  }
}
