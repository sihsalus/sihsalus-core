package org.sihsalus.module.queue;

import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.api.dao.QueueDao;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.dao.RoomProviderMapDao;
import org.openmrs.module.queue.api.dao.impl.QueueDaoImpl;
import org.openmrs.module.queue.api.dao.impl.QueueEntryDaoImpl;
import org.openmrs.module.queue.api.dao.impl.QueueRoomDaoImpl;
import org.openmrs.module.queue.api.dao.impl.RoomProviderMapDaoImpl;
import org.openmrs.module.queue.api.impl.QueueEntryServiceImpl;
import org.openmrs.module.queue.api.impl.QueueRoomServiceImpl;
import org.openmrs.module.queue.api.impl.QueueServiceImpl;
import org.openmrs.module.queue.api.impl.RoomProviderMapServiceImpl;
import org.openmrs.module.queue.api.sort.BasicPrioritySortWeightGenerator;
import org.openmrs.module.queue.api.sort.ExistingValueSortWeightGenerator;
import org.openmrs.module.queue.api.sort.SortWeightGenerator;
import org.openmrs.module.queue.web.Legacy1xRestController;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = Legacy1xRestController.class)
public class SihsalusQueueConfiguration {

    @Bean
    HibernateMappingContributor queueLiquibaseOrderingContributor() {
        return List::of;
    }

    @Bean
    QueueDao queueDao(SessionFactory sessionFactory) {
        return new QueueDaoImpl(sessionFactory);
    }

    @Bean
    QueueEntryDao queueEntryDao(SessionFactory sessionFactory) {
        return new QueueEntryDaoImpl(sessionFactory);
    }

    @Bean
    QueueRoomDao queueRoomDao(SessionFactory sessionFactory) {
        return new QueueRoomDaoImpl(sessionFactory);
    }

    @Bean
    RoomProviderMapDao roomProviderMapDao(SessionFactory sessionFactory) {
        return new RoomProviderMapDaoImpl(sessionFactory);
    }

    @Bean(name = "queue.QueueService")
    QueueService queueService(QueueDao queueDao) {
        QueueServiceImpl service = new QueueServiceImpl();
        service.setDao(queueDao);
        return service;
    }

    @Bean(name = "queue.QueueEntryService")
    QueueEntryService queueEntryService(
            QueueEntryDao queueEntryDao,
            VisitService visitService,
            AdministrationService administrationService,
            ExistingValueSortWeightGenerator existingValueSortWeightGenerator) {
        QueueEntryServiceImpl service = new QueueEntryServiceImpl();
        service.setDao(queueEntryDao);
        service.setVisitService(visitService);
        service.setAdministrationService(administrationService);
        service.setSortWeightGenerator(existingValueSortWeightGenerator);
        return service;
    }

    @Bean(name = "queue.QueueRoomService")
    QueueRoomService queueRoomService(QueueRoomDao queueRoomDao) {
        QueueRoomServiceImpl service = new QueueRoomServiceImpl();
        service.setDao(queueRoomDao);
        return service;
    }

    @Bean(name = "queue.RoomProviderMapService")
    RoomProviderMapService roomProviderMapService(RoomProviderMapDao roomProviderMapDao) {
        RoomProviderMapServiceImpl service = new RoomProviderMapServiceImpl();
        service.setDao(roomProviderMapDao);
        return service;
    }

    @Bean(name = "queue.QueueServicesWrapper")
    QueueServicesWrapper queueServicesWrapper(
            QueueService queueService,
            QueueEntryService queueEntryService,
            QueueRoomService queueRoomService,
            RoomProviderMapService roomProviderMapService,
            AdministrationService administrationService,
            ConceptService conceptService,
            LocationService locationService,
            PatientService patientService,
            VisitService visitService,
            ProviderService providerService) {
        return new QueueServicesWrapper(
                queueService,
                queueEntryService,
                queueRoomService,
                roomProviderMapService,
                administrationService,
                conceptService,
                locationService,
                patientService,
                visitService,
                providerService);
    }

    @Bean(name = "existingValueSortWeightGenerator")
    ExistingValueSortWeightGenerator existingValueSortWeightGenerator() {
        return new ExistingValueSortWeightGenerator();
    }

    @Bean(name = "basicPrioritySortWeightGenerator")
    BasicPrioritySortWeightGenerator basicPrioritySortWeightGenerator(QueueServicesWrapper queueServicesWrapper) {
        return new BasicPrioritySortWeightGenerator(queueServicesWrapper);
    }

    @Bean
    SmartInitializingSingleton queueServiceRegistrar(
            ServiceContext serviceContext,
            QueueService queueService,
            QueueEntryService queueEntryService,
            QueueRoomService queueRoomService,
            RoomProviderMapService roomProviderMapService) {
        return () -> {
            serviceContext.setService(QueueService.class, queueService);
            serviceContext.setService(QueueEntryService.class, queueEntryService);
            serviceContext.setService(QueueRoomService.class, queueRoomService);
            serviceContext.setService(RoomProviderMapService.class, roomProviderMapService);
        };
    }
}
