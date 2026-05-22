package org.sihsalus.module.bedmanagement;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.Extension;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.bedmanagement.BedManagementActivator;
import org.openmrs.module.bedmanagement.constants.BedManagementProperties;
import org.openmrs.module.bedmanagement.dao.BedManagementDao;
import org.openmrs.module.bedmanagement.dao.BedTagMapDao;
import org.openmrs.module.bedmanagement.dao.impl.BedManagementDaoImpl;
import org.openmrs.module.bedmanagement.dao.impl.BedTagMapDaoImpl;
import org.openmrs.module.bedmanagement.extension.html.AdminList;
import org.openmrs.module.bedmanagement.service.BedManagementService;
import org.openmrs.module.bedmanagement.service.BedTagMapService;
import org.openmrs.module.bedmanagement.service.impl.BedManagementServiceImpl;
import org.openmrs.module.bedmanagement.service.impl.BedTagMapServiceImpl;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackageClasses = BedManagementActivator.class,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Handler.class))
public class SihsalusBedManagementConfiguration {

    private static final String ADMIN_LIST_EXTENSION_POINT = "org.openmrs.admin.list";

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

    @Bean
    SmartInitializingSingleton bedManagementStaticInitializer() {
        return () -> {
            initializeBedManagementProperties();
            registerAdminListExtension();
        };
    }

    private static void initializeBedManagementProperties() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        try {
            BedManagementProperties.initalize();
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    private static void registerAdminListExtension() {
        AdminList adminList = new AdminList();
        adminList.setPointId(ADMIN_LIST_EXTENSION_POINT);
        List<Extension> extensions = ModuleFactory.getExtensionMap()
                .computeIfAbsent(ADMIN_LIST_EXTENSION_POINT + Extension.EXTENSION_ID_SEPARATOR + "html", key -> new ArrayList<>());
        if (extensions.stream().noneMatch(AdminList.class::isInstance)) {
            extensions.add(adminList);
        }
    }
}
