package org.sihsalus.module.cohort;

import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.cohort.CohortAttribute;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.CohortMemberAttribute;
import org.openmrs.module.cohort.CohortMemberAttributeType;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.cohort.api.dao.CohortAttributeDao;
import org.openmrs.module.cohort.api.dao.CohortAttributeTypeDao;
import org.openmrs.module.cohort.api.dao.CohortDao;
import org.openmrs.module.cohort.api.dao.CohortMemberAttributeDao;
import org.openmrs.module.cohort.api.dao.CohortMemberAttributeTypeDao;
import org.openmrs.module.cohort.api.dao.CohortMemberDao;
import org.openmrs.module.cohort.api.dao.CohortTypeDao;
import org.openmrs.module.cohort.api.dao.GenericDao;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;
import org.openmrs.module.cohort.api.impl.CohortMemberServiceImpl;
import org.openmrs.module.cohort.api.impl.CohortServiceImpl;
import org.openmrs.module.cohort.api.impl.CohortTypeServiceImpl;
import org.openmrs.module.cohort.web.resource.CohortMainRestController;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {CohortM.class, CohortMainRestController.class})
public class SihsalusCohortConfiguration {

  @Bean
  HibernateMappingContributor cohortHibernateMappingContributor() {
    return () ->
        List.of(
            "CohortMemberAttribute.hbm.xml",
            "CohortMemberAttributeType.hbm.xml",
            "CohortAttribute.hbm.xml",
            "CohortAttributeType.hbm.xml");
  }

  @Bean
  GenericDao<CohortM> cohortDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortDao(sessionFactory, searchQueryHandler);
  }

  @Bean
  GenericDao<CohortAttribute> cohortAttributeDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortAttributeDao(sessionFactory, searchQueryHandler);
  }

  @Bean
  GenericDao<CohortAttributeType> cohortAttributeTypeDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortAttributeTypeDao(sessionFactory, searchQueryHandler);
  }

  @Bean
  GenericDao<CohortMember> cohortMemberDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortMemberDao(sessionFactory, searchQueryHandler);
  }

  @Bean
  GenericDao<CohortMemberAttributeType> cohortMemberAttributeTypeDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortMemberAttributeTypeDao(sessionFactory, searchQueryHandler);
  }

  @Bean
  GenericDao<CohortMemberAttribute> cohortMemberAttributeDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortMemberAttributeDao(sessionFactory, searchQueryHandler);
  }

  @Bean
  GenericDao<CohortType> cohortTypeDao(
      SessionFactory sessionFactory, SearchQueryHandler searchQueryHandler) {
    return new CohortTypeDao(sessionFactory, searchQueryHandler);
  }

  @Bean("cohort.cohortService")
  CohortService cohortService(
      GenericDao<CohortM> cohortDao,
      GenericDao<CohortAttribute> cohortAttributeDao,
      GenericDao<CohortAttributeType> cohortAttributeTypeDao) {
    return new CohortServiceImpl(cohortDao, cohortAttributeDao, cohortAttributeTypeDao);
  }

  @Bean("cohort.cohortMemberService")
  CohortMemberService cohortMemberService(
      GenericDao<CohortMember> cohortMemberDao,
      GenericDao<CohortMemberAttributeType> cohortMemberAttributeTypeDao,
      GenericDao<CohortMemberAttribute> cohortMemberAttributeDao) {
    return new CohortMemberServiceImpl(
        cohortMemberDao, cohortMemberAttributeTypeDao, cohortMemberAttributeDao);
  }

  @Bean("cohort.cohortTypeService")
  CohortTypeService cohortTypeService(GenericDao<CohortType> cohortTypeDao) {
    return new CohortTypeServiceImpl(cohortTypeDao);
  }

  @Bean
  SmartInitializingSingleton cohortServiceRegistrar(
      ServiceContext serviceContext,
      CohortService cohortService,
      CohortMemberService cohortMemberService,
      CohortTypeService cohortTypeService) {
    return () -> {
      serviceContext.setService(CohortService.class, cohortService);
      serviceContext.setService(CohortMemberService.class, cohortMemberService);
      serviceContext.setService(CohortTypeService.class, cohortTypeService);
    };
  }
}
