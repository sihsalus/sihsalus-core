package org.sihsalus.module.metadatamapping;

import java.util.List;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.openmrs.module.metadatamapping.api.db.MetadataMappingDAO;
import org.openmrs.module.metadatamapping.api.db.hibernate.HibernateMetadataMappingDAO;
import org.openmrs.module.metadatamapping.api.impl.MetadataMappingServiceImpl;
import org.openmrs.module.metadatamapping.api.wrapper.ConceptAdapter;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusMetadataMappingConfiguration {

    @Bean
    HibernateMappingContributor metadataMappingHibernateMappingContributor() {
        return () -> List.of(
                "MetadataSource.hbm.xml",
                "MetadataTermMapping.hbm.xml",
                "MetadataSet.hbm.xml",
                "MetadataSetMember.hbm.xml");
    }

    @Bean
    MetadataMappingDAO metadataMappingDAO(DbSessionFactory dbSessionFactory) {
        HibernateMetadataMappingDAO dao = new HibernateMetadataMappingDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    ConceptAdapter metadataMappingConceptAdapter() {
        return new ConceptAdapter();
    }

    @Bean
    MetadataMappingService metadataMappingService(
            ConceptService conceptService,
            AdministrationService adminService,
            MetadataMappingDAO metadataMappingDAO,
            ConceptAdapter conceptAdapter) {
        MetadataMappingServiceImpl service = new MetadataMappingServiceImpl();
        service.setConceptService(conceptService);
        service.setAdminService(adminService);
        service.setDao(metadataMappingDAO);
        service.setConceptAdapter(conceptAdapter);
        return service;
    }

    @Bean
    SmartInitializingSingleton metadataMappingServiceRegistrar(
            ServiceContext serviceContext, MetadataMappingService metadataMappingService) {
        return () -> serviceContext.setService(MetadataMappingService.class, metadataMappingService);
    }
}
