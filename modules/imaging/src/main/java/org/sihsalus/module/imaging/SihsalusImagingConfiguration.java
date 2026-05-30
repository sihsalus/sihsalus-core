package org.sihsalus.module.imaging;

import java.util.List;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.imaging.ImagingProperties;
import org.openmrs.module.imaging.api.DicomStudyService;
import org.openmrs.module.imaging.api.OrthancConfigurationService;
import org.openmrs.module.imaging.api.RequestProcedureService;
import org.openmrs.module.imaging.api.RequestProcedureStepService;
import org.openmrs.module.imaging.api.client.OrthancHttpClient;
import org.openmrs.module.imaging.api.dao.DicomStudyDao;
import org.openmrs.module.imaging.api.dao.OrthancConfigurationDao;
import org.openmrs.module.imaging.api.dao.RequestProcedureDao;
import org.openmrs.module.imaging.api.dao.RequestProcedureStepDao;
import org.openmrs.module.imaging.api.impl.DicomStudyServiceImpl;
import org.openmrs.module.imaging.api.impl.OrthancConfigurationServiceImpl;
import org.openmrs.module.imaging.api.impl.RequestProcedureServiceImpl;
import org.openmrs.module.imaging.api.impl.RequestProcedureStepServiceImpl;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.openmrs.module.imaging.web.controller")
public class SihsalusImagingConfiguration {

  @Bean
  HibernateMappingContributor imagingHibernateMappingContributor() {
    return () ->
        List.of(
            "OrthancConfiguration.hbm.xml",
            "DicomStudy.hbm.xml",
            "RequestProcedure.hbm.xml",
            "RequestProcedureStep.hbm.xml");
  }

  @Bean
  OrthancHttpClient orthancHttpClient() {
    return new OrthancHttpClient();
  }

  @Bean("imagingProperties")
  ImagingProperties imagingProperties() {
    return new ImagingProperties();
  }

  @Bean(name = "imaging.OrthancConfigurationDao")
  OrthancConfigurationDao orthancConfigurationDao(DbSessionFactory dbSessionFactory) {
    OrthancConfigurationDao dao = new OrthancConfigurationDao();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean(name = "imaging.DicomStudyDao")
  DicomStudyDao dicomStudyDao(DbSessionFactory dbSessionFactory) {
    DicomStudyDao dao = new DicomStudyDao();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean(name = "imaging.RequestProcedureDao")
  RequestProcedureDao requestProcedureDao(DbSessionFactory dbSessionFactory) {
    RequestProcedureDao dao = new RequestProcedureDao();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean(name = "imaging.RequestProcedureStepDao")
  RequestProcedureStepDao requestProcedureStepDao(DbSessionFactory dbSessionFactory) {
    RequestProcedureStepDao dao = new RequestProcedureStepDao();
    dao.setSessionFactory(dbSessionFactory);
    return dao;
  }

  @Bean(name = "imaging.OrthancConfigurationService")
  OrthancConfigurationService orthancConfigurationService(
      OrthancConfigurationDao orthancConfigurationDao, OrthancHttpClient orthancHttpClient) {
    OrthancConfigurationServiceImpl service = new OrthancConfigurationServiceImpl();
    service.setDao(orthancConfigurationDao);
    service.setHttpClient(orthancHttpClient);
    return service;
  }

  @Bean(name = "imaging.DicomStudyService")
  DicomStudyService dicomStudyService(
      DicomStudyDao dicomStudyDao, OrthancHttpClient orthancHttpClient) {
    DicomStudyServiceImpl service = new DicomStudyServiceImpl();
    service.setDao(dicomStudyDao);
    service.setHttpClient(orthancHttpClient);
    return service;
  }

  @Bean(name = "imaging.RequestProcedureService")
  RequestProcedureService requestProcedureService(RequestProcedureDao requestProcedureDao) {
    RequestProcedureServiceImpl service = new RequestProcedureServiceImpl();
    service.setDao(requestProcedureDao);
    return service;
  }

  @Bean(name = "imaging.RequestProcedureStepService")
  RequestProcedureStepService requestProcedureStepService(
      RequestProcedureStepDao requestProcedureStepDao) {
    RequestProcedureStepServiceImpl service = new RequestProcedureStepServiceImpl();
    service.setDao(requestProcedureStepDao);
    return service;
  }

  @Bean
  SmartInitializingSingleton imagingServiceRegistrar(
      ServiceContext serviceContext,
      OrthancConfigurationService orthancConfigurationService,
      DicomStudyService dicomStudyService,
      RequestProcedureService requestProcedureService,
      RequestProcedureStepService requestProcedureStepService) {
    return () -> {
      serviceContext.setService(OrthancConfigurationService.class, orthancConfigurationService);
      serviceContext.setService(DicomStudyService.class, dicomStudyService);
      serviceContext.setService(RequestProcedureService.class, requestProcedureService);
      serviceContext.setService(RequestProcedureStepService.class, requestProcedureStepService);
    };
  }
}
