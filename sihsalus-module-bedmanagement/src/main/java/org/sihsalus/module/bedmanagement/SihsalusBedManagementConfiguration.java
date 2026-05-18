package org.sihsalus.module.bedmanagement;

import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.bedmanagement.dao.BedManagementDao;
import org.openmrs.module.bedmanagement.dao.BedTagMapDao;
import org.openmrs.module.bedmanagement.dao.impl.BedManagementDaoImpl;
import org.openmrs.module.bedmanagement.dao.impl.BedTagMapDaoImpl;
import org.openmrs.module.bedmanagement.entity.Bed;
import org.openmrs.module.bedmanagement.rest.resource.BedResource;
import org.openmrs.module.bedmanagement.service.BedManagementService;
import org.openmrs.module.bedmanagement.service.BedTagMapService;
import org.openmrs.module.bedmanagement.service.impl.BedManagementServiceImpl;
import org.openmrs.module.bedmanagement.service.impl.BedTagMapServiceImpl;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {Bed.class, BedResource.class})
public class SihsalusBedManagementConfiguration {

    @Bean
    HibernateMappingContributor bedManagementHibernateMappingContributor() {
        return () -> List.of(
                "Bed.hbm.xml",
                "BedLocationMapping.hbm.xml",
                "BedPatientAssignment.hbm.xml",
                "BedTag.hbm.xml",
                "BedTagMap.hbm.xml",
                "BedType.hbm.xml");
    }

    @Bean
    BedManagementDao bedManagementDao(SessionFactory sessionFactory) {
        BedManagementDaoImpl dao = new BedManagementDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    BedTagMapDao bedTagMapDao(SessionFactory sessionFactory) {
        BedTagMapDaoImpl dao = new BedTagMapDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    BedManagementService bedManagementService(BedManagementDao bedManagementDao, LocationService locationService) {
        BedManagementServiceImpl service = new BedManagementServiceImpl();
        service.setDao(bedManagementDao);
        service.setLocationService(locationService);
        return service;
    }

    @Bean
    BedTagMapService bedTagMapService(BedTagMapDao bedTagMapDao) {
        BedTagMapServiceImpl service = new BedTagMapServiceImpl();
        service.setDao(bedTagMapDao);
        return service;
    }

    @Bean
    SmartInitializingSingleton bedManagementServiceRegistrar(
            ServiceContext serviceContext,
            BedManagementService bedManagementService,
            BedTagMapService bedTagMapService) {
        return () -> {
            serviceContext.setService(BedManagementService.class, bedManagementService);
            serviceContext.setService(BedTagMapService.class, bedTagMapService);
        };
    }
}
