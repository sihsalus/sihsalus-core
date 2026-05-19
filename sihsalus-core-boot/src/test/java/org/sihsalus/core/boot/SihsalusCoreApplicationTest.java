package org.sihsalus.core.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.UserSessionListener;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.hibernate.HibernateSessionFactoryBean;
import org.openmrs.api.handler.SaveHandler;
import org.openmrs.calculation.api.CalculationRegistrationService;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.event.Event;
import org.openmrs.event.EventActivator;
import org.openmrs.event.EventListener;
import org.openmrs.event.JmsEventPublisher;
import org.openmrs.event.api.db.hibernate.HibernateEventInterceptor;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.attachments.AttachmentsService;
import org.openmrs.module.attachments.obs.DefaultAttachmentHandler;
import org.openmrs.module.attachments.obs.ImageAttachmentHandler;
import org.openmrs.module.authentication.AuthenticationUserSessionListener;
import org.openmrs.module.authentication.DelegatingAuthenticationScheme;
import org.openmrs.module.appointments.events.advice.AppointmentEventsAdvice;
import org.openmrs.module.appointments.events.advice.RecurringAppointmentEventsAdvice;
import org.openmrs.module.appointments.events.eventListener.AppointmentSMSEventListener;
import org.openmrs.module.appointments.events.publisher.AppointmentEventPublisher;
import org.openmrs.module.appointments.notification.impl.DefaultTCAppointmentPatientEmailNotifier;
import org.openmrs.module.appointments.service.AppointmentArgumentsMapper;
import org.openmrs.module.appointments.service.AppointmentRecurringPatternService;
import org.openmrs.module.appointments.service.AppointmentServiceAttributeTypeService;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.service.SpecialityService;
import org.openmrs.module.appointments.service.impl.PatientAppointmentNotifierService;
import org.openmrs.module.Extension;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.bedmanagement.BedLayout;
import org.openmrs.module.bedmanagement.aop.BedPatientAssignmentValidator;
import org.openmrs.module.bedmanagement.aop.EncounterWithBedPatientAssignmentSaveHandler;
import org.openmrs.module.bedmanagement.aop.VisitWithBedPatientAssignmentSaveHandler;
import org.openmrs.module.bedmanagement.aop.VisitWithBedPatientAssignmentValidator;
import org.openmrs.module.bedmanagement.constants.BedManagementProperties;
import org.openmrs.module.bedmanagement.entity.BedPatientAssignment;
import org.openmrs.module.bedmanagement.extension.html.AdminList;
import org.openmrs.module.bedmanagement.service.BedManagementService;
import org.openmrs.module.bedmanagement.service.BedTagMapService;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.PaymentModeService;
import org.openmrs.module.billing.api.billing.BillingEventListener;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.concept.EmrConceptService;
import org.openmrs.module.emrapi.patient.EmrPatientService;
import org.openmrs.module.emrapi.procedure.ProcedureService;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.api.FuaEstadoService;
import org.openmrs.module.fua.api.FuaEstadoVersionService;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.fua.api.FuaVersionService;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsService;
import org.openmrs.module.htmlwidgets.web.handler.WidgetHandler;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.idgen.validator.LuhnMod10IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod25IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod30IdentifierValidator;
import org.openmrs.module.imaging.ImagingConstants;
import org.openmrs.module.imaging.ImagingProperties;
import org.openmrs.module.imaging.api.DicomStudyService;
import org.openmrs.module.imaging.api.OrthancConfigurationService;
import org.openmrs.module.imaging.api.RequestProcedureService;
import org.openmrs.module.imaging.api.RequestProcedureStepService;
import org.openmrs.module.sihsalusinterop.api.DyakuSenderService;
import org.openmrs.module.sihsalusinterop.api.advice.EncounterSavedAdvice;
import org.openmrs.module.sihsalusinterop.api.model.InteropQueueItem;
import org.openmrs.module.sihsalusinterop.api.service.BundleBuilderService;
import org.openmrs.module.legacyui.api.LegacyUIService;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.OAuth2UserInfoAuthenticationScheme;
import org.openmrs.module.o3forms.api.O3FormsService;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.OclConceptService;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.ordertemplates.api.OrderTemplatesService;
import org.openmrs.module.patientflags.aop.ConditionServiceAdvice;
import org.openmrs.module.patientflags.aop.EncounterServiceAdvice;
import org.openmrs.module.patientflags.aop.ObsServiceAdvice;
import org.openmrs.module.patientflags.aop.OrderServiceAdvice;
import org.openmrs.module.patientflags.aop.PatientServiceAdvice;
import org.openmrs.module.patientflags.aop.ProgramWorkflowServiceAdvice;
import org.openmrs.module.patientflags.api.FlagService;
import org.openmrs.module.patientflags.task.PatientFlagTask;
import org.openmrs.module.patientdocuments.common.PatientDocumentsPrivilegeConstants;
import org.openmrs.module.patientdocuments.reports.EncounterPdfReport;
import org.openmrs.module.patientdocuments.reports.PatientIdStickerPdfReport;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.QueueRoomService;
import org.openmrs.module.queue.api.QueueService;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.RoomProviderMapService;
import org.openmrs.module.queue.api.VisitWithQueueEntriesSaveHandler;
import org.openmrs.module.queue.api.sort.BasicPrioritySortWeightGenerator;
import org.openmrs.module.queue.api.sort.ExistingValueSortWeightGenerator;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.model.QueueRoom;
import org.openmrs.module.queue.model.RoomProviderMap;
import org.openmrs.module.queue.tasks.QueueTimerTask;
import org.openmrs.module.queue.tasks.QueueTaskExecutor;
import org.openmrs.module.queue.validators.QueueEntryValidator;
import org.openmrs.module.queue.validators.QueueRoomValidation;
import org.openmrs.module.queue.validators.QueueValidator;
import org.openmrs.module.queue.validators.RoomProviderMapValidator;
import org.openmrs.module.queue.validators.VisitWithQueueEntriesValidator;
import org.openmrs.module.reporting.cohort.definition.AllPatientsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.dataset.DataSetMetaData;
import org.openmrs.module.reporting.dataset.definition.service.DataSetDefinitionService;
import org.openmrs.module.reporting.definition.evaluator.DefinitionEvaluator;
import org.openmrs.module.reporting.definition.service.SerializedDefinitionService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.report.task.ReportingTimerTask;
import org.openmrs.module.reporting.serializer.ReportingSerializer;
import org.openmrs.module.reportingrest.adhoc.AdHocExportManager;
import org.openmrs.module.serialization.xstream.XStreamSerializer;
import org.openmrs.module.serialization.xstream.XStreamShortSerializer;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.util.HandlerUtil;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.core.api.StaticModuleTaskRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SihsalusCoreApplicationTest {

    private static final String TEST_PATIENT_UUID = "2e29f6cc-14e4-44f5-bf57-c5cf0d7659f3";

    private static final String TEST_IDENTIFIER = "SIH-REST-FHIR-001";

    private static final String TEST_IDENTIFIER_TYPE_UUID = "f7c1c7d2-cf2d-45fd-9660-e81975cf50da";

    private static final String ADMIN_BASIC_AUTH = basicAuth("admin", "test");

    private static final List<String> UNPORTED_STOCKMANAGEMENT_SCHEDULER_JOBS = Arrays.asList(
            "org.openmrs.module.stockmanagement.api.jobs.StockRuleEvaluationJob",
            "org.openmrs.module.stockmanagement.api.jobs.StockBatchExpiryJob",
            "org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob");

    @Autowired private MockMvc mockMvc;

    @Autowired private ApplicationContext applicationContext;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void healthcheckResponds() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void fhirMetadataResponds() throws Exception {
        mockMvc.perform(get("/api/fhir/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("CapabilityStatement"))
                .andExpect(jsonPath("$.rest[0].resource[?(@.type == 'Patient')]").exists());
    }

    @Test
    void fhirR4ReadEndpointInvokesImportedProvider() throws Exception {
        mockMvc.perform(get("/api/fhir/r4/Patient/not-a-real-patient").header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("not-found"));
    }

    @Test
    void reportsStaticModulesWithoutOmodRuntime() throws Exception {
        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dynamicOmodLoading").value(false));
    }

    @Test
    void staticModuleBackgroundTasksUseSchedulerUserWithoutDaemonToken() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            StaticModuleTaskRunner.runAndWait(
                    null, () -> {
                        assertTrue(Context.isAuthenticated());
                        assertNotNull(Context.getAuthenticatedUser());
                    });
            assertTrue(!Context.isAuthenticated());
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void restV1ControllerIsWiredWithoutOmodRuntime() throws Exception {
        mockMvc.perform(get("/rest/v1/not-a-resource/not-a-real-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.rawMessage").value("Unknown resource: v1/not-a-resource"));
    }

    @Test
    void queueEntryNumberLegacyEndpointRespondsSafelyWithoutParameters() throws Exception {
        assertTrue(QueueTimerTask.isEnabled());

        mockMvc.perform(get("/rest/v1/queue-entry-number"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceType").value(""))
                .andExpect(jsonPath("$.visitQueueNumber").value(""));
    }

    @Test
    void queueTicketAssignmentRejectsIncompletePayload() throws Exception {
        mockMvc.perform(post("/rest/v1/queueutil/assignticket")
                        .header("Authorization", ADMIN_BASIC_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketNumber\":\"A-001\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queueLegacyStateEndpointsRequirePrivileges() throws Exception {
        mockMvc.perform(post("/rest/v1/queueutil/assignticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicePointName\":\"Room 1\",\"ticketNumber\":\"A-001\",\"status\":\"called\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rest/v1/queueutil/active-tickets"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rest/v1/queue-entry-number")
                        .param("visitAttributeType", "not-a-real-vat"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void queueIsWiredAsStaticInternalModule() {
        QueueService queueService = Context.getService(QueueService.class);
        QueueEntryService queueEntryService = Context.getService(QueueEntryService.class);
        assertNotNull(queueService);
        assertNotNull(queueEntryService);
        assertNotNull(Context.getService(QueueRoomService.class));
        assertNotNull(Context.getService(RoomProviderMapService.class));
        assertNotNull(Context.getRegisteredComponent("queue.QueueServicesWrapper", QueueServicesWrapper.class));
        assertNotNull(Context.getRegisteredComponent("existingValueSortWeightGenerator", ExistingValueSortWeightGenerator.class));
        assertNotNull(Context.getRegisteredComponent("basicPrioritySortWeightGenerator", BasicPrioritySortWeightGenerator.class));
        assertNotNull(applicationContext.getBean("&queueTaskExecutor", QueueTaskExecutor.class));
        assertTrue(QueueTimerTask.isEnabled());

        RestService restService = Context.getService(RestService.class);
        for (String resourceName : List.of(
                "v1/queue",
                "v1/queue-entry",
                "v1/queue-room",
                "v1/queue-room-provider")) {
            assertNotNull(restService.getResourceByName(resourceName));
        }

        List<SaveHandler> visitSaveHandlers = HandlerUtil.getHandlersForType(SaveHandler.class, Visit.class);
        assertTrue(
                visitSaveHandlers.stream().anyMatch(VisitWithQueueEntriesSaveHandler.class::isInstance),
                () -> "Visit SaveHandlers: " + visitSaveHandlers.stream()
                        .map(handler -> handler.getClass().getName())
                        .toList());
        assertQueueValidatorRegistered(Queue.class, QueueValidator.class);
        assertQueueValidatorRegistered(QueueEntry.class, QueueEntryValidator.class);
        assertQueueValidatorRegistered(QueueRoom.class, QueueRoomValidation.class);
        assertQueueValidatorRegistered(RoomProviderMap.class, RoomProviderMapValidator.class);
        assertQueueValidatorRegistered(Visit.class, VisitWithQueueEntriesValidator.class);

        List<String> queuePrivileges = List.of(
                "Get Queues",
                "Get Queue Entries",
                "Get Queue Rooms",
                "Manage Queues",
                "Manage Queue Entries",
                "Manage Queue Rooms",
                "Purge Queues",
                "Purge Queue Entries",
                "Purge Queue Rooms");
        String privilegePlaceholders = String.join(",", Collections.nCopies(queuePrivileges.size(), "?"));
        assertEquals(queuePrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + privilegePlaceholders + ")",
                Integer.class,
                queuePrivileges.toArray()));

        List<String> queueGlobalProperties = List.of(
                "queue.statusConceptSetName",
                "queue.priorityConceptSetName",
                "queue.serviceConceptSetName",
                "queue.sortWeightGenerator");
        String globalPropertyPlaceholders = String.join(",", Collections.nCopies(queueGlobalProperties.size(), "?"));
        assertEquals(queueGlobalProperties.size(), jdbcTemplate.queryForObject(
                "select count(*) from global_property where property in (" + globalPropertyPlaceholders + ")",
                Integer.class,
                queueGlobalProperties.toArray()));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from queue_entry", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from queue_room", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from room_provider_map", Integer.class));

        jdbcTemplate.update(
                "update global_property set property_value = ? where property = ?",
                "basicPrioritySortWeightGenerator",
                "queue.sortWeightGenerator");
        queueEntryService.setSortWeightGenerator(null);
        try {
            StaticModuleTaskRunner.runAndWait(
                    null,
                    () -> assertTrue(
                            queueEntryService.getSortWeightGenerator() instanceof BasicPrioritySortWeightGenerator));
        } finally {
            jdbcTemplate.update(
                    "update global_property set property_value = ? where property = ?",
                    "",
                    "queue.sortWeightGenerator");
            queueEntryService.setSortWeightGenerator(null);
        }
        StaticModuleTaskRunner.runAndWait(
                null,
                () -> assertTrue(queueEntryService.getSortWeightGenerator() instanceof ExistingValueSortWeightGenerator));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, queueService::getAllQueues);
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void patientRegistryPatientIsReadableThroughRestAndFhir() throws Exception {
        String patientUuid = ensureTestPatient();

        mockMvc.perform(get("/rest/v1/patient/{uuid}", patientUuid).header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(patientUuid))
                .andExpect(jsonPath("$.identifier").value(TEST_IDENTIFIER))
                .andExpect(jsonPath("$.givenName").value("Sihsalus"))
                .andExpect(jsonPath("$.familyName").value("Paciente"));

        assertNotNull(jdbcTemplate.queryForObject("select count(*) from fhir_patient_identifier_system", Integer.class));

        mockMvc.perform(get("/api/fhir/r4/Patient/{uuid}", patientUuid).header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value(patientUuid))
                .andExpect(jsonPath("$.identifier[0].value").value(TEST_IDENTIFIER));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void idgenIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(IdentifierSourceService.class));
        assertNotNull(Context.getPatientService().getIdentifierValidator((Class) LuhnMod10IdentifierValidator.class));
        assertNotNull(Context.getPatientService().getIdentifierValidator((Class) LuhnMod25IdentifierValidator.class));
        assertNotNull(Context.getPatientService().getIdentifierValidator((Class) LuhnMod30IdentifierValidator.class));
        assertNotNull(
                jdbcTemplate.queryForObject("select count(*) from idgen_identifier_source", Integer.class));
    }

    @Test
    void staticModuleServicesPreserveOpenmrsAuthorizationInterceptors() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            IdentifierSourceService identifierSourceService = Context.getService(IdentifierSourceService.class);

            assertThrows(APIAuthenticationException.class, identifierSourceService::getIdentifierSourceTypes);
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void authenticationIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getAuthenticationScheme());
        assertNotNull(DelegatingAuthenticationScheme.class.cast(Context.getAuthenticationScheme()));
        assertNotNull(Context.getRegisteredComponents(UserSessionListener.class).stream()
                .filter(AuthenticationUserSessionListener.class::isInstance)
                .findFirst()
                .orElse(null));
    }

    @Test
    void oauth2LoginIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getRegisteredComponent(
                OAuth2LoginConstants.AUTH_SCHEME_COMPONENT, OAuth2UserInfoAuthenticationScheme.class));
        assertNotNull(OAuth2UserInfoAuthenticationScheme.class.cast(
                AuthenticationConfig.getAuthenticationScheme(OAuth2LoginConstants.OAUTH2_SCHEME_ID)));
        assertNotNull(OAuth2UserInfoAuthenticationScheme.class.cast(
                AuthenticationConfig.getAuthenticationScheme(OAuth2TokenCredentials.SCHEME_NAME)));
    }

    @Test
    void addressHierarchyIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(AddressHierarchyService.class));
        assertNotNull(
                jdbcTemplate.queryForObject("select count(*) from address_hierarchy_level", Integer.class));
    }

    @Test
    void metadataMappingIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(MetadataMappingService.class));
        assertNotNull(jdbcTemplate.queryForObject(
                "select count(*) from metadatamapping_metadata_source", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject(
                "select count(*) from metadatamapping_metadata_term_mapping", Integer.class));
    }

    @Test
    void emrApiIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(EmrConceptService.class));
        assertNotNull(Context.getService(EmrPatientService.class));
        assertNotNull(Context.getService(AdtService.class));
        assertNotNull(Context.getService(ProcedureService.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from emrapi_procedure_type", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from emrapi_procedure", Integer.class));
    }

    @Test
    void o3FormsIsWiredAsStaticInternalModule() throws Exception {
        O3FormsService service = Context.getService(O3FormsService.class);
        assertNotNull(service);
        assertTrue(service instanceof Advised);
        mockMvc.perform(get("/rest/v1/o3/forms/not-a-real-form"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rest/v1/o3/forms/not-a-real-form").header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isNotFound());
    }

    @Test
    void patientDocumentsIsWiredAsStaticInternalModule() throws Exception {
        assertNotNull(Context.getRegisteredComponent("encounterPdfReport", EncounterPdfReport.class));
        assertNotNull(Context.getRegisteredComponent("patientIdStickerPdfReport", PatientIdStickerPdfReport.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (?, ?)",
                Integer.class,
                PatientDocumentsPrivilegeConstants.VIEW_PATIENT_ID_STICKER,
                PatientDocumentsPrivilegeConstants.PRINT_ENCOUNTER_FORMS_PRIVILEGE));
        mockMvc.perform(get("/rest/v1/patientdocuments/patientIdSticker")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/rest/v1/patientdocuments/patientIdSticker")
                        .param("patientUuid", TEST_PATIENT_UUID))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/v1/patientdocuments/encounters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + TEST_PATIENT_UUID + "\"]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cohortIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(CohortService.class));
        assertNotNull(Context.getService(CohortMemberService.class));
        assertNotNull(Context.getService(CohortTypeService.class));
        RestService restService = Context.getService(RestService.class);
        assertNotNull(restService.getResourceByName("v1/cohortm/cohort"));
        assertNotNull(restService.getResourceByName("v1/cohortm/cohortmember"));
        assertNotNull(restService.getResourceByName("v1/cohortm/cohorttype"));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from cohort_type", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from cohort_attribute_type", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from cohort_member_attribute_type", Integer.class));
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyUiIsWiredAsStaticInternalModule() {
        LegacyUIService legacyUIService = Context.getService(LegacyUIService.class);
        assertNotNull(legacyUIService);

        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from global_property where property in (?, ?)",
                Integer.class,
                "legacyui.enableExitFromCare",
                "dashboard.formEntry.maximumNumberEncountersToShow"));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, () -> legacyUIService.exitFromCare(null, null, null));
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void eventIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getRegisteredComponent("eventActivator", EventActivator.class));
        assertNotNull(Context.getRegisteredComponents(HibernateEventInterceptor.class).stream()
                .findFirst()
                .orElse(null));
        assertNotNull(Context.getRegisteredComponents(JmsEventPublisher.class).stream()
                .findFirst()
                .orElse(null));

        HibernateSessionFactoryBean sessionFactoryBean =
                applicationContext.getBean("&sessionFactory", HibernateSessionFactoryBean.class);
        assertTrue(sessionFactoryBean.interceptors.values().stream()
                .anyMatch(HibernateEventInterceptor.class::isInstance));

        AtomicInteger received = new AtomicInteger();
        EventListener listener = message -> received.incrementAndGet();
        Event.subscribe(Patient.class, Event.Action.CREATED.name(), listener);
        try {
            Patient patient = new Patient();
            patient.setUuid("event-smoke-patient");
            Event.fireAction(Event.Action.CREATED.name(), patient);
            assertEquals(1, received.get());
        } finally {
            Event.unsubscribe(Patient.class, Event.Action.CREATED, listener);
        }
    }

    @Test
    void calculationIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(PatientCalculationService.class));
        assertNotNull(Context.getService(CalculationRegistrationService.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from calculation_registration", Integer.class));
    }

    @Test
    void htmlWidgetsIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(HtmlWidgetsService.class));
        assertNotNull(Context.getRegisteredComponents(WidgetHandler.class).stream()
                .findFirst()
                .orElse(null));
    }

    @Test
    void stockManagementIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(StockManagementService.class));
        assertNotNull(
                jdbcTemplate.queryForObject("select count(*) from stockmgmt_stock_item", Integer.class));
        String privilegePlaceholders = String.join(",", Collections.nCopies(Privileges.ALL.size(), "?"));
        assertEquals(Privileges.ALL.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + privilegePlaceholders + ")",
                Integer.class,
                Privileges.ALL.toArray()));

        String jobPlaceholders = String.join(
                ",", Collections.nCopies(UNPORTED_STOCKMANAGEMENT_SCHEDULER_JOBS.size(), "?"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from scheduler_task_config where schedulable_class in (" + jobPlaceholders
                        + ") and (started = true or start_on_startup = true)",
                Integer.class,
                UNPORTED_STOCKMANAGEMENT_SCHEDULER_JOBS.toArray()));
    }

    @Test
    void stockManagementProxyPreservesOpenmrsAuthorizationInterceptors() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            StockManagementService stockManagementService = Context.getService(StockManagementService.class);

            assertThrows(APIAuthenticationException.class,
                    () -> stockManagementService.getStockOperationByUuid("not-a-real-stock-operation"));
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void teleconsultationIsWiredAsStaticInternalModule() throws Exception {
        TeleconsultationService teleconsultationService = Context.getService(TeleconsultationService.class);
        assertNotNull(teleconsultationService);
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege = ?",
                Integer.class,
                "Create Teleconsultation"));
        assertEquals("https://meet.jit.si/{0}", jdbcTemplate.queryForObject(
                "select property_value from global_property where property = ?",
                String.class,
                "bahmni.appointment.teleConsultation.serverUrlPattern"));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class,
                    () -> teleconsultationService.generateTeleconsultationLink("not-a-real-appointment"));
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }

        mockMvc.perform(get("/rest/v1/teleconsultation/generateLink").param("uuid", "not-a-real-appointment"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/v1/teleconsultation/generateLink")
                        .header("Authorization", ADMIN_BASIC_AUTH)
                        .param("uuid", "room 123"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://meet.jit.si/room%20123"));
        mockMvc.perform(get("/rest/v1/teleconsultation/generateLink")
                        .header("Authorization", ADMIN_BASIC_AUTH)
                        .param("uuid", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appointmentsIsWiredAsStaticInternalModule() {
        AppointmentsService appointmentsService = Context.getService(AppointmentsService.class);
        AppointmentRecurringPatternService recurringPatternService =
                Context.getService(AppointmentRecurringPatternService.class);
        assertNotNull(appointmentsService);
        assertNotNull(Context.getService(AppointmentServiceDefinitionService.class));
        assertNotNull(Context.getService(AppointmentServiceAttributeTypeService.class));
        assertNotNull(Context.getService(SpecialityService.class));
        assertNotNull(recurringPatternService);
        assertNotNull(Context.getService(AppointmentArgumentsMapper.class));

        assertAdviceRegistered(appointmentsService, AppointmentEventsAdvice.class);
        assertAdviceRegistered(recurringPatternService, RecurringAppointmentEventsAdvice.class);
        assertNotNull(Context.getRegisteredComponent("appointmentEventPublisher", AppointmentEventPublisher.class));
        assertNotNull(Context.getRegisteredComponent("AppointmentsAsyncThreadExecutor", Executor.class));
        assertNotNull(Context.getRegisteredComponents(AppointmentSMSEventListener.class).stream()
                .findFirst()
                .orElse(null));

        PatientAppointmentNotifierService notifierService =
                Context.getRegisteredComponent("patientAppointmentNotifierService", PatientAppointmentNotifierService.class);
        assertTrue(notifierService.getEventNotifiers().stream()
                .anyMatch(DefaultTCAppointmentPatientEmailNotifier.class::isInstance));

        List<String> appointmentPrivileges = List.of(
                "View Appointments",
                "Manage Appointments",
                "View Appointment Services",
                "Manage Appointment Services",
                "Manage Appointment Specialities",
                "Manage Own Appointments",
                "Reset Appointment Status",
                "Appointments: Invite Providers",
                "app:appointments:manageServiceAvailability",
                "app:appointments:manageServices");
        String privilegePlaceholders = String.join(",", Collections.nCopies(appointmentPrivileges.size(), "?"));
        assertEquals(appointmentPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + privilegePlaceholders + ")",
                Integer.class,
                appointmentPrivileges.toArray()));

        List<String> appointmentGlobalProperties = List.of(
                "disableDefaultAppointmentValidations",
                "SchedulerMarksComplete",
                "SchedulerMarksMissed",
                "SchedulerReminderBeforeHours",
                "bahmni.appointment.teleConsultation.patientEmailNotificationSubject",
                "bahmni.appointment.teleConsultation.patientEmailNotificationTemplate",
                "bahmni.appointment.teleConsultation.serverUrlPattern",
                "bahmni.appointment.teleConsultation.sendEmail",
                "bahmni.appointment.adhocTeleConsultation.patientEmailNotificationSubject",
                "bahmni.appointment.adhocTeleConsultation.patientEmailNotificationTemplate",
                "bahmni.appointment.adhocTeleConsultation.bccEmails",
                "bahmni.adhoc.teleConsultation.id",
                "sms.enableAppointmentBookingSMSAlert",
                "sms.enableAppointmentReminderSMSAlert",
                "sms.timezone",
                "sms.dateformat");
        String globalPropertyPlaceholders =
                String.join(",", Collections.nCopies(appointmentGlobalProperties.size(), "?"));
        assertEquals(appointmentGlobalProperties.size(), jdbcTemplate.queryForObject(
                "select count(*) from global_property where property in (" + globalPropertyPlaceholders + ")",
                Integer.class,
                appointmentGlobalProperties.toArray()));

        assertEquals(3, jdbcTemplate.queryForObject(
                "select count(*) from scheduler_task_config where name in (?, ?, ?)",
                Integer.class,
                "Mark Appointment As Missed Task",
                "Mark Appointment As Complete Task",
                "Reminder of scheduled appointment"));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from patient_appointment", Integer.class));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, () -> appointmentsService.getAllAppointments(null));
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void bedManagementIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(BedManagementService.class));
        assertNotNull(Context.getService(BedTagMapService.class));

        RestService restService = Context.getService(RestService.class);
        for (String resourceName : List.of(
                "v1/admissionLocation",
                "v1/bed",
                "v1/beds",
                "v1/bedPatientAssignment",
                "v1/bedTag",
                "v1/bedTagMap",
                "v1/bedtype")) {
            assertNotNull(restService.getResourceByName(resourceName));
        }
        assertNotNull(HandlerUtil.getPreferredHandler(Converter.class, BedLayout.class));

        List<SaveHandler> encounterSaveHandlers = HandlerUtil.getHandlersForType(SaveHandler.class, Encounter.class);
        assertTrue(
                encounterSaveHandlers.stream().anyMatch(EncounterWithBedPatientAssignmentSaveHandler.class::isInstance),
                () -> "Encounter SaveHandlers: " + encounterSaveHandlers.stream()
                        .map(handler -> handler.getClass().getName())
                        .toList());
        List<SaveHandler> visitSaveHandlers = HandlerUtil.getHandlersForType(SaveHandler.class, Visit.class);
        assertTrue(
                visitSaveHandlers.stream().anyMatch(VisitWithBedPatientAssignmentSaveHandler.class::isInstance),
                () -> "Visit SaveHandlers: " + visitSaveHandlers.stream()
                        .map(handler -> handler.getClass().getName())
                        .toList());
        List<Validator> bedPatientAssignmentValidators =
                HandlerUtil.getHandlersForType(Validator.class, BedPatientAssignment.class);
        assertTrue(
                bedPatientAssignmentValidators.stream().anyMatch(BedPatientAssignmentValidator.class::isInstance),
                () -> "BedPatientAssignment Validators: " + bedPatientAssignmentValidators.stream()
                        .map(handler -> handler.getClass().getName())
                        .toList());
        List<Validator> visitValidators = HandlerUtil.getHandlersForType(Validator.class, Visit.class);
        assertTrue(
                visitValidators.stream().anyMatch(VisitWithBedPatientAssignmentValidator.class::isInstance),
                () -> "Visit Validators: " + visitValidators.stream()
                        .map(handler -> handler.getClass().getName())
                        .toList());

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from global_property where property = ? and property_value = ?",
                Integer.class,
                "bedmanagement.owa.enableManagingLocations",
                "true"));
        List<String> bedManagementPrivileges = List.of(
                "app:adt",
                "Get Beds",
                "Assign Beds",
                "Get Admission Locations",
                "Edit Admission Locations",
                "Edit Beds",
                "Get Bed Type",
                "Edit Bed Type",
                "Get Bed Tags",
                "Edit Bed Tags");
        String placeholders = String.join(",", Collections.nCopies(bedManagementPrivileges.size(), "?"));
        assertEquals(bedManagementPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + placeholders + ")",
                Integer.class,
                bedManagementPrivileges.toArray()));

        assertNotNull(jdbcTemplate.queryForObject("select count(*) from bed", Integer.class));
        assertEquals("/owa", BedManagementProperties.getProperty("appBaseUrl"));
        assertTrue(ModuleFactory.getExtensions("org.openmrs.admin.list", Extension.MEDIA_TYPE.html).stream()
                .anyMatch(AdminList.class::isInstance));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            BedManagementService bedManagementService = Context.getService(BedManagementService.class);

            assertThrows(APIAuthenticationException.class, bedManagementService::getAdmissionLocations);
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void billingIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(BillService.class));
        assertNotNull(Context.getService(PaymentModeService.class));
        assertNotNull(Context.getRegisteredComponents(BillingEventListener.class).stream()
                .findFirst()
                .orElse(null));

        RestService restService = Context.getService(RestService.class);
        for (String resourceName : List.of(
                "v1/billing/attributetype",
                "v1/billing/bill",
                "v1/billing/billDiscount",
                "v1/billing/billExemption",
                "v1/billing/billLineItem",
                "v1/billing/billRefund",
                "v1/billing/billableService",
                "v1/billing/cashPoint",
                "v1/billing/cashierItemPrice",
                "v1/billing/paymentAttribute",
                "v1/billing/paymentMode",
                "v1/billing/paymentModeAttributeType",
                "v2/billing/timesheet")) {
            assertNotNull(restService.getResourceByName(resourceName));
        }

        List<String> billingPrivileges = List.of(
                "Manage Cashier Bills",
                "Adjust Cashier Bills",
                "View Cashier Bills",
                "Delete Cashier Bills",
                "Purge Cashier Bills",
                "Refund Money",
                "Reprint Receipt",
                "Manage Bill Discounts",
                "Approve Bill Discounts",
                "View Bill Discounts",
                "Request Bill Refunds",
                "Approve Bill Refunds",
                "Complete Bill Refunds",
                "View Bill Refunds",
                "Manage Cashier Timesheets",
                "View Cashier Timesheets",
                "Purge Cashier Timesheets",
                "Manage Cashier Metadata",
                "View Cashier Metadata",
                "Purge Cashier Metadata",
                "App: View Cashier App",
                "App: Access Cashier Tasks",
                "Task: Create new bill",
                "Task: Adjust Cashier Bills",
                "Task: Cashier Timesheets",
                "Task: Manage Cashier Module",
                "Task: Manage Cashier Metadata",
                "Task: View Cashier Reports");
        String placeholders = String.join(",", Collections.nCopies(billingPrivileges.size(), "?"));
        assertEquals(billingPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + placeholders + ")",
                Integer.class,
                billingPrivileges.toArray()));
    }

    @Test
    void fuaIsWiredAsStaticInternalModule() {
        FuaService fuaService = Context.getService(FuaService.class);
        assertNotNull(fuaService);
        assertTrue(fuaService instanceof Advised);
        assertNotNull(Context.getService(FuaEstadoService.class));
        assertNotNull(Context.getService(FuaVersionService.class));
        assertNotNull(Context.getService(FuaEstadoVersionService.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from fua", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from fua_estado", Integer.class));

        List<String> fuaPrivileges = Arrays.asList(
                FuaConfig.MODULE_PRIVILEGE,
                FuaConfig.READ_FUA_PRIVILEGE,
                FuaConfig.MANAGE_FUA_PRIVILEGE,
                FuaConfig.DELETE_FUA_PRIVILEGE,
                FuaConfig.UPDATE_FUA_PRIVILEGE);
        String privilegePlaceholders = String.join(",", Collections.nCopies(fuaPrivileges.size(), "?"));
        assertEquals(fuaPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + privilegePlaceholders + ")",
                Integer.class,
                fuaPrivileges.toArray()));
        assertEquals(FuaConfig.FUA_GENERATOR_URL_DEFAULT, jdbcTemplate.queryForObject(
                "select property_value from global_property where property = ?",
                String.class,
                FuaConfig.FUA_GENERATOR_URL_GP));
        assertEquals("", jdbcTemplate.queryForObject(
                "select property_value from global_property where property = ?",
                String.class,
                FuaConfig.FUA_GENERATOR_IDENTIFIER));
    }

    @Test
    void fuaProxyPreservesOpenmrsAuthorizationInterceptors() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            FuaService fuaService = Context.getService(FuaService.class);

            assertThrows(APIAuthenticationException.class, fuaService::getAllFuas);
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void imagingIsWiredAsStaticInternalModule() {
        OrthancConfigurationService orthancConfigurationService = Context.getService(OrthancConfigurationService.class);
        DicomStudyService dicomStudyService = Context.getService(DicomStudyService.class);
        RequestProcedureService requestProcedureService = Context.getService(RequestProcedureService.class);
        RequestProcedureStepService requestProcedureStepService = Context.getService(RequestProcedureStepService.class);
        assertNotNull(orthancConfigurationService);
        assertNotNull(dicomStudyService);
        assertNotNull(requestProcedureService);
        assertNotNull(requestProcedureStepService);
        assertTrue(orthancConfigurationService instanceof Advised);
        assertTrue(dicomStudyService instanceof Advised);
        assertTrue(requestProcedureService instanceof Advised);
        assertTrue(requestProcedureStepService instanceof Advised);
        assertNotNull(Context.getRegisteredComponent("imagingProperties", ImagingProperties.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_orthancconfiguration", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_dicomstudy", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_requestprocedure", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_requestprocedurestep", Integer.class));

        List<String> imagingPrivileges = Arrays.asList(
                ImagingConstants.TASK_MANAGER_ORTHANC_CONFIGURATION,
                ImagingConstants.PRIVILEGE_MODIFY_IMAGE_DATA,
                ImagingConstants.PRIVILEGE_VIEW_IMAGE_DATA,
                ImagingConstants.PRIVILEGE_UPLOAD_IMAGE_DATA,
                ImagingConstants.PRIVILEGE_DELETE_IMAGE_DATA,
                ImagingConstants.PRIVILEGE_LINK_IMAGE_STUDIES,
                ImagingConstants.PRIVILEGE_EDIT_WORKLIST,
                ImagingConstants.PRIVILEGE_RECEIVE_ORTHANC_UPDATES);
        String privilegePlaceholders = String.join(",", Collections.nCopies(imagingPrivileges.size(), "?"));
        assertEquals(imagingPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + privilegePlaceholders + ")",
                Integer.class,
                imagingPrivileges.toArray()));
        assertEquals("200000000", jdbcTemplate.queryForObject(
                "select property_value from global_property where property = ?",
                String.class,
                ImagingConstants.GP_MAX_UPLOAD_IMAGEDATA_SIZE));
        assertEquals(imagingPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from role_privilege where role = 'Imaging Manager' and privilege in ("
                        + privilegePlaceholders + ")",
                Integer.class,
                imagingPrivileges.toArray()));
    }

    @Test
    void imagingProxyPreservesOpenmrsAuthorizationInterceptors() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        UserContext originalUserContext = openedSession ? null : Context.getUserContext();
        try {
            Context.setUserContext(new UserContext(Context.getAuthenticationScheme()));
            OrthancConfigurationService orthancConfigurationService =
                    Context.getService(OrthancConfigurationService.class);

            assertThrows(APIAuthenticationException.class, orthancConfigurationService::getAllOrthancConfigurations);
        } finally {
            if (originalUserContext != null) {
                Context.setUserContext(originalUserContext);
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void sihsalusInteropIsWiredAsStaticInternalModule() throws Exception {
        DyakuSenderService dyakuSenderService = Context.getService(DyakuSenderService.class);
        assertNotNull(dyakuSenderService);
        assertTrue(dyakuSenderService instanceof Advised);
        assertNotNull(Context.getRegisteredComponent("sihsalusinterop.BundleBuilderService", BundleBuilderService.class));
        assertAdviceRegistered(Context.getEncounterService(), EncounterSavedAdvice.class);
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from sihsalus_interop_queue", Integer.class));

        List<String> interopPrivileges = List.of(
                "SIH SALUS Interop Privilege",
                "Manage Interop Queue",
                "Send FHIR Messages",
                "Generate FUA",
                "View Interop Logs",
                "View Interop Queue");
        String privilegePlaceholders = String.join(",", Collections.nCopies(interopPrivileges.size(), "?"));
        assertEquals(interopPrivileges.size(), jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (" + privilegePlaceholders + ")",
                Integer.class,
                interopPrivileges.toArray()));

        List<String> interopGlobalProperties = List.of(
                "sihsalusinterop.renhice.endpoint",
                "sihsalusinterop.renhice.enabled",
                "sihsalusinterop.queue.maxRetries",
                "sihsalusinterop.queue.retryInterval");
        String globalPropertyPlaceholders = String.join(",", Collections.nCopies(interopGlobalProperties.size(), "?"));
        assertEquals(interopGlobalProperties.size(), jdbcTemplate.queryForObject(
                "select count(*) from global_property where property in (" + globalPropertyPlaceholders + ")",
                Integer.class,
                interopGlobalProperties.toArray()));

        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from scheduler_task_config "
                        + "where name = ? and schedulable_class = ? and repeat_interval = ?",
                Integer.class,
                "SIH SALUS Interop Queue Processor",
                "org.openmrs.module.sihsalusinterop.api.tasks.QueueProcessorTask",
                300L));

        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from scheduler_task_config "
                        + "where name = ? and (started = true or start_on_startup = true)",
                Integer.class,
                "SIH SALUS Interop Queue Processor"));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeQueueSmoke = Context.isAuthenticated();
        if (!authenticatedBeforeQueueSmoke) {
            Context.authenticate("admin", "test");
        }
        InteropQueueItem queuedItem = null;
        try {
            queuedItem = dyakuSenderService.queueMessage(
                    "FHIR_BUNDLE",
                    "{\"resourceType\":\"Bundle\",\"type\":\"transaction\"}",
                    "http://request-controlled.example/fhir");
            assertEquals("http://hapi-fhir-server:8080/fhir", queuedItem.getTargetEndpoint());
        } finally {
            try {
                if (queuedItem != null) {
                    dyakuSenderService.deleteQueueItem(queuedItem.getQueueId());
                }
            } finally {
                if (!authenticatedBeforeQueueSmoke && Context.isSessionOpen()) {
                    Context.logout();
                }
                if (openedSession && Context.isSessionOpen()) {
                    Context.closeSession();
                }
            }
        }

        boolean openedRequestSession = !Context.isSessionOpen();
        if (openedRequestSession) {
            Context.openSession();
        }
        UserContext originalUserContext = Context.getUserContext();
        Context.setUserContext(new UserContext(Context.getAuthenticationScheme()));
        try {
            mockMvc.perform(get("/module/sihsalusinterop/api/queue/items"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        } finally {
            Context.setUserContext(originalUserContext);
            if (openedRequestSession && Context.isSessionOpen()) {
                Context.closeSession();
            }
        }
    }

    @Test
    void sihsalusInteropProxyPreservesOpenmrsAuthorizationInterceptors() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        UserContext originalUserContext = openedSession ? null : Context.getUserContext();
        try {
            Context.setUserContext(new UserContext(Context.getAuthenticationScheme()));
            DyakuSenderService dyakuSenderService = Context.getService(DyakuSenderService.class);

            assertThrows(APIAuthenticationException.class, dyakuSenderService::getAllQueueItems);
        } finally {
            if (originalUserContext != null) {
                Context.setUserContext(originalUserContext);
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void attachmentsIsWiredAsStaticInternalModule() throws Exception {
        assertNotNull(Context.getService(AttachmentsService.class));
        assertNotNull(Context.getObsService().getHandler(DefaultAttachmentHandler.class.getSimpleName()));
        assertNotNull(Context.getObsService().getHandler(ImageAttachmentHandler.class.getSimpleName()));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/attachment"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from global_property where property = 'attachments.defaultConceptComplexUuid'",
                Integer.class));
    }

    @Test
    void patientFlagsIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(FlagService.class));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/flag"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/tag"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/priority"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/displaypoint"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/patientflag"));

        assertEquals(5, jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in ('Manage Flags', 'View Flags', "
                        + "'Manage Patient Flags', 'View Patient Flags', 'Test Flags')",
                Integer.class));

        assertAdviceRegistered(Context.getEncounterService(), EncounterServiceAdvice.class);
        assertAdviceRegistered(Context.getObsService(), ObsServiceAdvice.class);
        assertAdviceRegistered(Context.getOrderService(), OrderServiceAdvice.class);
        assertAdviceRegistered(Context.getPatientService(), PatientServiceAdvice.class);
        assertAdviceRegistered(Context.getConditionService(), ConditionServiceAdvice.class);
        assertAdviceRegistered(Context.getProgramWorkflowService(), ProgramWorkflowServiceAdvice.class);

        PatientFlagTask.setDaemonToken(null);
        PatientFlagTask.evaluateAllFlags().run();
    }

    @Test
    void webservicesRestServiceIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(RestService.class));
    }

    @Test
    void serializationXstreamIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getSerializationService().getSerializer(XStreamSerializer.class));
        assertNotNull(Context.getSerializationService().getSerializer(XStreamShortSerializer.class));
    }

    @Test
    void orderTemplatesIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(OrderTemplatesService.class));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/ordertemplates/orderTemplate"));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from order_template", Integer.class));
    }

    @Test
    void openConceptLabIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(ImportService.class));
        assertNotNull(Context.getService(OclConceptService.class));
        assertNotNull(Context.getRegisteredComponent("openconceptlab.importer", Importer.class));
        assertNotNull(Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class));

        RestService restService = Context.getService(RestService.class);
        assertNotNull(restService.getResourceByName("v1/openconceptlab/import"));
        assertNotNull(restService.getResourceByName("v1/openconceptlab/importaction"));
        assertNotNull(restService.getResourceByName("v1/openconceptlab/subscription"));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            ImportService importService = Context.getService(ImportService.class);

            assertThrows(APIAuthenticationException.class, importService::getSubscription);
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void reportingRestIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(SerializedDefinitionService.class));
        assertNotNull(Context.getService(EvaluationService.class));
        assertNotNull(Context.getService(ReportDefinitionService.class));
        assertNotNull(Context.getService(ReportService.class));
        assertNotNull(Context.getService(CohortDefinitionService.class));
        assertNotNull(Context.getService(DataSetDefinitionService.class));
        assertNotNull(Context.getSerializationService().getSerializer(ReportingSerializer.class));
        assertNotNull(HandlerUtil.getPreferredHandler(DefinitionEvaluator.class, AllPatientsCohortDefinition.class));
        assertTrue(ReportingTimerTask.isEnabled());

        assertNotNull(Context.getRegisteredComponents(AdHocExportManager.class).stream()
                .findFirst()
                .orElse(null));
        assertNotNull(HandlerUtil.getPreferredHandler(Converter.class, DataSetMetaData.class));
        assertNotNull(HandlerUtil.getPreferredHandler(Converter.class, EvaluationContext.class));
        assertNotNull(HandlerUtil.getPreferredHandler(Converter.class, Mapped.class));
        assertNotNull(HandlerUtil.getPreferredHandler(Converter.class, Parameter.class));
        assertNotNull(HandlerUtil.getPreferredHandler(Converter.class, RenderingMode.class));

        RestService restService = Context.getService(RestService.class);
        for (String resourceName : List.of(
                "v1/reportingrest/adhocdataset",
                "v1/reportingrest/adhocquery",
                "v1/reportingrest/cohort",
                "v1/reportingrest/cohortDefinition",
                "v1/reportingrest/dataSet",
                "v1/reportingrest/dataSetDefinition",
                "v1/reportingrest/definitionlibrary",
                "v1/reportingrest/reportdata",
                "v1/reportingrest/reportDefinition",
                "v1/reportingrest/reportDefinitionsWithScheduledRequests",
                "v1/reportingrest/reportDesign",
                "v1/reportingrest/reportRequest")) {
            assertNotNull(restService.getResourceByName(resourceName));
        }

        assertNotNull(jdbcTemplate.queryForObject("select count(*) from reporting_report_design", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from reporting_report_request", Integer.class));
    }

    private String ensureTestPatient() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        Context.addProxyPrivilege(
                PrivilegeConstants.ADD_PATIENTS,
                PrivilegeConstants.EDIT_PATIENTS,
                PrivilegeConstants.GET_PATIENTS,
                PrivilegeConstants.ADD_PATIENT_IDENTIFIERS,
                PrivilegeConstants.GET_IDENTIFIER_TYPES,
                PrivilegeConstants.MANAGE_IDENTIFIER_TYPES);
        try {
            restartOpenmrsIdentityColumnsForH2();

            Patient existing = Context.getPatientService().getPatientByUuid(TEST_PATIENT_UUID);
            if (existing != null) {
                return existing.getUuid();
            }

            User systemUser = new User(1);
            ensureTestIdentifierTypeExists();
            PatientIdentifierType identifierType =
                    Context.getPatientService().getPatientIdentifierTypeByUuid(TEST_IDENTIFIER_TYPE_UUID);
            assertNotNull(identifierType);

            java.util.Date now = new java.util.Date();
            Patient patient = new Patient();
            patient.setUuid(TEST_PATIENT_UUID);
            patient.setGender("M");
            patient.setBirthdate(java.sql.Date.valueOf("1990-01-02"));
            patient.setCreator(systemUser);
            patient.setDateCreated(now);
            patient.setPersonCreator(systemUser);
            patient.setPersonDateCreated(now);
            patient.setPersonVoided(false);

            PersonName name = new PersonName("Sihsalus", null, "Paciente");
            name.setCreator(systemUser);
            name.setDateCreated(now);
            patient.addName(name);

            PatientIdentifier identifier = new PatientIdentifier(TEST_IDENTIFIER, identifierType, null);
            identifier.setPreferred(true);
            identifier.setCreator(systemUser);
            identifier.setDateCreated(now);
            patient.addIdentifier(identifier);

            return Context.getPatientService().savePatient(patient).getUuid();
        } finally {
            Context.removeProxyPrivilege(
                    PrivilegeConstants.ADD_PATIENTS,
                    PrivilegeConstants.EDIT_PATIENTS,
                    PrivilegeConstants.GET_PATIENTS,
                    PrivilegeConstants.ADD_PATIENT_IDENTIFIERS,
                    PrivilegeConstants.GET_IDENTIFIER_TYPES,
                    PrivilegeConstants.MANAGE_IDENTIFIER_TYPES);
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    private void restartOpenmrsIdentityColumnsForH2() {
        jdbcTemplate.execute("alter table person_attribute add column if not exists value varchar(50)");
        restartIdentityAfterSeedData("patient_identifier_type", "patient_identifier_type_id");
        restartIdentityAfterSeedData("person", "person_id");
        restartIdentityAfterSeedData("person_name", "person_name_id");
        restartIdentityAfterSeedData("patient_identifier", "patient_identifier_id");
    }

    private void ensureTestIdentifierTypeExists() {
        Integer existingCount = jdbcTemplate.queryForObject(
                "select count(*) from patient_identifier_type where uuid = ?",
                Integer.class,
                TEST_IDENTIFIER_TYPE_UUID);
        if (existingCount != null && existingCount > 0) {
            return;
        }

        Integer nextIdentifierTypeId = jdbcTemplate.queryForObject(
                "select coalesce(max(patient_identifier_type_id), 0) + 1 from patient_identifier_type",
                Integer.class);
        jdbcTemplate.update(
                "insert into patient_identifier_type "
                        + "(patient_identifier_type_id, name, description, format, check_digit, creator, "
                        + "date_created, required, format_description, validator, location_behavior, "
                        + "retired, uuid, uniqueness_behavior) "
                        + "values (?, ?, ?, null, false, ?, current_timestamp, false, null, null, ?, false, ?, ?)",
                nextIdentifierTypeId,
                "SIH Salus test identifier",
                "Identifier type used by REST/FHIR boot smoke tests",
                1,
                PatientIdentifierType.LocationBehavior.NOT_USED.name(),
                TEST_IDENTIFIER_TYPE_UUID,
                PatientIdentifierType.UniquenessBehavior.UNIQUE.name());
    }

    private static String basicAuth(String username, String password) {
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private void assertQueueValidatorRegistered(Class<?> supportedClass, Class<? extends Validator> validatorClass) {
        List<Validator> validators = HandlerUtil.getHandlersForType(Validator.class, supportedClass);
        assertTrue(
                validators.stream().anyMatch(validatorClass::isInstance),
                () -> supportedClass.getSimpleName() + " Validators: " + validators.stream()
                        .map(validator -> validator.getClass().getName())
                        .toList());
    }

    private void restartIdentityAfterSeedData(String tableName, String columnName) {
        Integer nextValue = jdbcTemplate.queryForObject(
                "select coalesce(max(" + columnName + "), 0) + 1000 from " + tableName, Integer.class);
        jdbcTemplate.execute("alter table " + tableName + " alter column " + columnName + " restart with " + nextValue);
    }

    private void assertAdviceRegistered(Object service, Class<?> adviceClass) {
        assertTrue(service instanceof Advised);
        assertTrue(Arrays.stream(((Advised) service).getAdvisors())
                .anyMatch(advisor -> adviceClass.isInstance(advisor.getAdvice())));
    }
}
