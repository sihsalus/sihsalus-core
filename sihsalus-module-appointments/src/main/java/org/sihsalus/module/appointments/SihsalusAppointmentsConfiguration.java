package org.sihsalus.module.appointments;

import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.appointments.AppointmentsActivator;
import org.openmrs.module.appointments.conflicts.AppointmentConflict;
import org.openmrs.module.appointments.conflicts.impl.AppointmentServiceUnavailabilityConflict;
import org.openmrs.module.appointments.conflicts.impl.PatientDoubleBookingConflict;
import org.openmrs.module.appointments.dao.AppointmentAuditDao;
import org.openmrs.module.appointments.dao.AppointmentDao;
import org.openmrs.module.appointments.dao.AppointmentRecurringPatternDao;
import org.openmrs.module.appointments.dao.AppointmentServiceAttributeTypeDao;
import org.openmrs.module.appointments.dao.AppointmentServiceDao;
import org.openmrs.module.appointments.dao.SpecialityDao;
import org.openmrs.module.appointments.dao.impl.AppointmentAuditDaoImpl;
import org.openmrs.module.appointments.dao.impl.AppointmentDaoImpl;
import org.openmrs.module.appointments.dao.impl.AppointmentRecurringPatternDaoImpl;
import org.openmrs.module.appointments.dao.impl.AppointmentServiceAttributeTypeDaoImpl;
import org.openmrs.module.appointments.dao.impl.AppointmentServiceDaoImpl;
import org.openmrs.module.appointments.dao.impl.SpecialityDaoImpl;
import org.openmrs.module.appointments.events.advice.AppointmentEventsAdvice;
import org.openmrs.module.appointments.events.advice.RecurringAppointmentEventsAdvice;
import org.openmrs.module.appointments.helper.AppointmentServiceHelper;
import org.openmrs.module.appointments.notification.AppointmentEventNotifier;
import org.openmrs.module.appointments.notification.MailSender;
import org.openmrs.module.appointments.notification.impl.DefaultMailSender;
import org.openmrs.module.appointments.notification.impl.DefaultTCAppointmentPatientEmailNotifier;
import org.openmrs.module.appointments.service.AppointmentArgumentsMapper;
import org.openmrs.module.appointments.service.AppointmentNumberGenerator;
import org.openmrs.module.appointments.service.AppointmentNumberGeneratorLocator;
import org.openmrs.module.appointments.service.AppointmentRecurringPatternService;
import org.openmrs.module.appointments.service.AppointmentServiceAttributeTypeService;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.service.RecurringAppointmentNumberGenerator;
import org.openmrs.module.appointments.service.SpecialityService;
import org.openmrs.module.appointments.service.impl.AppointmentRecurringPatternServiceImpl;
import org.openmrs.module.appointments.service.impl.AppointmentArgumentsMapperImpl;
import org.openmrs.module.appointments.service.impl.AppointmentNumberGeneratorLocatorImpl;
import org.openmrs.module.appointments.service.impl.AppointmentServiceAttributeTypeServiceImpl;
import org.openmrs.module.appointments.service.impl.AppointmentServiceDefinitionServiceImpl;
import org.openmrs.module.appointments.service.impl.AppointmentsServiceImpl;
import org.openmrs.module.appointments.service.impl.DefaultAppointmentNumberGeneratorImpl;
import org.openmrs.module.appointments.service.impl.DefaultRecurringAppointmentNumberGeneratorImpl;
import org.openmrs.module.appointments.service.impl.PatientAppointmentNotifierService;
import org.openmrs.module.appointments.service.impl.SpecialityServiceImpl;
import org.openmrs.module.appointments.service.impl.TeleconsultationAppointmentService;
import org.openmrs.module.appointments.validator.AppointmentStatusChangeValidator;
import org.openmrs.module.appointments.validator.AppointmentValidator;
import org.openmrs.module.appointments.validator.impl.DefaultAppointmentStatusChangeValidator;
import org.openmrs.module.appointments.validator.impl.DefaultAppointmentValidator;
import org.openmrs.module.appointments.validator.impl.DefaultEditAppointmentValidator;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = AppointmentsActivator.class)
public class SihsalusAppointmentsConfiguration {

    @Bean
    HibernateMappingContributor appointmentsHibernateMappingContributor() {
        return () -> List.of(
                "Appointment.hbm.xml",
                "AppointmentAudit.hbm.xml",
                "AppointmentProvider.hbm.xml",
                "AppointmentReason.hbm.xml",
                "AppointmentRecurringPattern.hbm.xml",
                "AppointmentServiceAttribute.hbm.xml",
                "AppointmentServiceAttributeType.hbm.xml",
                "AppointmentServiceDefinition.hbm.xml",
                "AppointmentServiceType.hbm.xml",
                "ServiceWeeklyAvailability.hbm.xml",
                "Speciality.hbm.xml");
    }

    @Bean
    AppointmentDao appointmentDao(SessionFactory sessionFactory) {
        AppointmentDaoImpl dao = new AppointmentDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    AppointmentAuditDao appointmentAuditDao(SessionFactory sessionFactory) {
        AppointmentAuditDaoImpl dao = new AppointmentAuditDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    AppointmentServiceDao appointmentServiceDao(SessionFactory sessionFactory) {
        AppointmentServiceDaoImpl dao = new AppointmentServiceDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    AppointmentServiceAttributeTypeDao appointmentServiceAttributeTypeDao(SessionFactory sessionFactory) {
        AppointmentServiceAttributeTypeDaoImpl dao = new AppointmentServiceAttributeTypeDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    SpecialityDao specialityDao(SessionFactory sessionFactory) {
        SpecialityDaoImpl dao = new SpecialityDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    AppointmentRecurringPatternDao appointmentRecurringPatternDao(SessionFactory sessionFactory) {
        AppointmentRecurringPatternDaoImpl dao = new AppointmentRecurringPatternDaoImpl();
        dao.setSessionFactory(sessionFactory);
        return dao;
    }

    @Bean
    AppointmentServiceHelper appointmentServiceHelper() {
        return new AppointmentServiceHelper();
    }

    @Bean
    AppointmentValidator defaultAppointmentValidator() {
        return new DefaultAppointmentValidator();
    }

    @Bean
    AppointmentValidator defaultEditAppointmentValidator(AppointmentDao appointmentDao) {
        DefaultEditAppointmentValidator validator = new DefaultEditAppointmentValidator();
        validator.setAppointmentDao(appointmentDao);
        return validator;
    }

    @Bean
    AppointmentStatusChangeValidator defaultAppointmentStatusChangeValidator() {
        return new DefaultAppointmentStatusChangeValidator();
    }

    @Bean
    AppointmentConflict appointmentServiceUnavailabilityConflict() {
        return new AppointmentServiceUnavailabilityConflict();
    }

    @Bean
    AppointmentConflict patientDoubleBookingConflict(AppointmentDao appointmentDao) {
        PatientDoubleBookingConflict conflict = new PatientDoubleBookingConflict();
        conflict.setAppointmentDao(appointmentDao);
        return conflict;
    }

    @Bean
    MailSender defaultTCApptMailSender(AdministrationService administrationService) {
        return new DefaultMailSender(administrationService);
    }

    @Bean
    AppointmentEventNotifier defaultPatientEmailNotifier(MailSender defaultTCApptMailSender) {
        return new DefaultTCAppointmentPatientEmailNotifier(defaultTCApptMailSender);
    }

    @Bean
    PatientAppointmentNotifierService patientAppointmentNotifierService(List<AppointmentEventNotifier> eventNotifiers) {
        return new PatientAppointmentNotifierService(eventNotifiers);
    }

    @Bean
    AppointmentArgumentsMapper appointmentArgumentsMapper() {
        return new AppointmentArgumentsMapperImpl();
    }

    @Bean
    TeleconsultationAppointmentService teleconsultationAppointmentService(
            PatientService patientService, PatientAppointmentNotifierService patientAppointmentNotifierService) {
        TeleconsultationAppointmentService service = new TeleconsultationAppointmentService();
        service.setPatientService(patientService);
        service.setPatientAppointmentNotifierService(patientAppointmentNotifierService);
        return service;
    }

    @Bean
    AppointmentNumberGenerator appointmentNumberGenerator() {
        return new DefaultAppointmentNumberGeneratorImpl();
    }

    @Bean
    AppointmentNumberGeneratorLocator appointmentNumberGeneratorLocator(AppointmentNumberGenerator appointmentNumberGenerator) {
        AppointmentNumberGeneratorLocatorImpl locator = new AppointmentNumberGeneratorLocatorImpl(appointmentNumberGenerator);
        RecurringAppointmentNumberGenerator recurring = new DefaultRecurringAppointmentNumberGeneratorImpl(locator);
        locator.registerRecurringAppointmentNumberGenerator(recurring);
        return locator;
    }

    @Bean
    AppointmentsService appointmentsService(
            AppointmentDao appointmentDao,
            AppointmentAuditDao appointmentAuditDao,
            AppointmentServiceHelper appointmentServiceHelper,
            List<AppointmentValidator> appointmentValidators,
            List<AppointmentStatusChangeValidator> statusChangeValidators,
            List<AppointmentConflict> appointmentConflicts,
            TeleconsultationAppointmentService teleconsultationAppointmentService,
            PatientAppointmentNotifierService patientAppointmentNotifierService,
            AppointmentNumberGeneratorLocator appointmentNumberGeneratorLocator) {
        AppointmentsServiceImpl service = new AppointmentsServiceImpl();
        service.setAppointmentDao(appointmentDao);
        service.setAppointmentAuditDao(appointmentAuditDao);
        service.setAppointmentServiceHelper(appointmentServiceHelper);
        service.setAppointmentValidators(appointmentValidators);
        service.setEditAppointmentValidators(appointmentValidators);
        service.setStatusChangeValidators(statusChangeValidators);
        service.setAppointmentConflicts(appointmentConflicts);
        service.setTeleconsultationAppointmentService(teleconsultationAppointmentService);
        service.setAppointmentNotifierService(patientAppointmentNotifierService);
        service.setAppointmentNumberGeneratorLocator(appointmentNumberGeneratorLocator);
        return service;
    }

    @Bean
    AppointmentServiceDefinitionService appointmentServiceDefinitionService(
            AppointmentServiceDao appointmentServiceDao, AppointmentsService appointmentsService) {
        AppointmentServiceDefinitionServiceImpl service = new AppointmentServiceDefinitionServiceImpl();
        service.setAppointmentServiceDao(appointmentServiceDao);
        service.setAppointmentsService(appointmentsService);
        return service;
    }

    @Bean
    AppointmentServiceAttributeTypeService appointmentServiceAttributeTypeService(
            AppointmentServiceAttributeTypeDao appointmentServiceAttributeTypeDao) {
        AppointmentServiceAttributeTypeServiceImpl service = new AppointmentServiceAttributeTypeServiceImpl();
        service.setAppointmentServiceAttributeTypeDao(appointmentServiceAttributeTypeDao);
        return service;
    }

    @Bean
    SpecialityService specialityService(SpecialityDao specialityDao) {
        SpecialityServiceImpl service = new SpecialityServiceImpl();
        service.setSpecialityDao(specialityDao);
        return service;
    }

    @Bean
    AppointmentRecurringPatternService appointmentRecurringPatternService(
            AppointmentRecurringPatternDao appointmentRecurringPatternDao,
            AppointmentDao appointmentDao,
            AppointmentServiceHelper appointmentServiceHelper,
            List<AppointmentValidator> appointmentValidators,
            List<AppointmentStatusChangeValidator> statusChangeValidators,
            AppointmentNumberGeneratorLocator appointmentNumberGeneratorLocator) {
        AppointmentRecurringPatternServiceImpl service = new AppointmentRecurringPatternServiceImpl();
        service.setAppointmentRecurringPatternDao(appointmentRecurringPatternDao);
        service.setAppointmentDao(appointmentDao);
        service.setAppointmentServiceHelper(appointmentServiceHelper);
        service.setAppointmentValidators(appointmentValidators);
        service.setEditAppointmentValidators(appointmentValidators);
        service.setStatusChangeValidators(statusChangeValidators);
        service.setAppointmentNumberGeneratorLocator(appointmentNumberGeneratorLocator);
        return service;
    }

    @Bean
    SmartInitializingSingleton appointmentsServiceRegistrar(
            ServiceContext serviceContext,
            AppointmentsService appointmentsService,
            AppointmentServiceDefinitionService appointmentServiceDefinitionService,
            AppointmentServiceAttributeTypeService appointmentServiceAttributeTypeService,
            SpecialityService specialityService,
            AppointmentRecurringPatternService appointmentRecurringPatternService,
            AppointmentArgumentsMapper appointmentArgumentsMapper) {
        return () -> {
            serviceContext.setService(AppointmentsService.class, appointmentsService);
            serviceContext.setService(AppointmentServiceDefinitionService.class, appointmentServiceDefinitionService);
            serviceContext.setService(AppointmentServiceAttributeTypeService.class, appointmentServiceAttributeTypeService);
            serviceContext.setService(SpecialityService.class, specialityService);
            serviceContext.setService(AppointmentRecurringPatternService.class, appointmentRecurringPatternService);
            serviceContext.setService(AppointmentArgumentsMapper.class, appointmentArgumentsMapper);
            serviceContext.addAdvice(AppointmentsService.class, new AppointmentEventsAdvice());
            serviceContext.addAdvice(AppointmentRecurringPatternService.class, new RecurringAppointmentEventsAdvice());
        };
    }
}
