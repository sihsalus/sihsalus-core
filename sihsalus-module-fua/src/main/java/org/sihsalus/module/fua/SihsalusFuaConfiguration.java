package org.sihsalus.module.fua;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.api.FuaEstadoService;
import org.openmrs.module.fua.api.FuaEstadoVersionService;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.fua.api.FuaVersionService;
import org.openmrs.module.fua.api.dao.FuaDao;
import org.openmrs.module.fua.api.dao.FuaEstadoDao;
import org.openmrs.module.fua.api.dao.FuaEstadoVersionDao;
import org.openmrs.module.fua.api.dao.FuaVersionDao;
import org.openmrs.module.fua.api.impl.FuaEstadoServiceImpl;
import org.openmrs.module.fua.api.impl.FuaEstadoVersionServiceImpl;
import org.openmrs.module.fua.api.impl.FuaServiceImpl;
import org.openmrs.module.fua.api.impl.FuaVersionServiceImpl;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = FuaConfig.class)
public class SihsalusFuaConfiguration {

    @Bean
    FuaService fuaService(FuaDao fuaDao) {
        FuaServiceImpl service = new FuaServiceImpl();
        service.setDao(fuaDao);
        return service;
    }

    @Bean
    FuaEstadoService fuaEstadoService(FuaEstadoDao fuaEstadoDao) {
        FuaEstadoServiceImpl service = new FuaEstadoServiceImpl();
        service.setDao(fuaEstadoDao);
        return service;
    }

    @Bean
    FuaVersionService fuaVersionService(FuaVersionDao fuaVersionDao) {
        FuaVersionServiceImpl service = new FuaVersionServiceImpl();
        service.setDao(fuaVersionDao);
        return service;
    }

    @Bean
    FuaEstadoVersionService fuaEstadoVersionService(FuaEstadoVersionDao fuaEstadoVersionDao) {
        FuaEstadoVersionServiceImpl service = new FuaEstadoVersionServiceImpl();
        service.setDao(fuaEstadoVersionDao);
        return service;
    }

    @Bean
    SmartInitializingSingleton fuaServiceRegistrar(
            ServiceContext serviceContext,
            FuaService fuaService,
            FuaEstadoService fuaEstadoService,
            FuaVersionService fuaVersionService,
            FuaEstadoVersionService fuaEstadoVersionService) {
        return () -> {
            serviceContext.setService(FuaService.class, fuaService);
            serviceContext.setService(FuaEstadoService.class, fuaEstadoService);
            serviceContext.setService(FuaVersionService.class, fuaVersionService);
            serviceContext.setService(FuaEstadoVersionService.class, fuaEstadoVersionService);
        };
    }
}
