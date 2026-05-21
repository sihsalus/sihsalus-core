package org.sihsalus.core.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.UserSessionListener;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
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
import org.openmrs.module.attachments.AttachmentsConstants;
import org.openmrs.module.attachments.AttachmentsService;
import org.openmrs.module.attachments.obs.DefaultAttachmentHandler;
import org.openmrs.module.attachments.obs.ImageAttachmentHandler;
import org.openmrs.module.attachments.rest.AttachmentBytesResource;
import org.openmrs.module.attachments.rest.AttachmentResource;
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
import org.openmrs.module.appointments.service.impl.AppointmentRecurringPatternServiceImpl;
import org.openmrs.module.appointments.service.impl.AppointmentsServiceImpl;
import org.openmrs.module.appointments.service.impl.PatientAppointmentNotifierService;
import org.openmrs.module.appointments.validator.impl.DefaultAppointmentValidator;
import org.openmrs.module.appointments.validator.impl.DefaultEditAppointmentValidator;
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
import org.openmrs.module.billing.api.BillExemptionService;
import org.openmrs.module.billing.api.BillLineItemService;
import org.openmrs.module.billing.api.CashierItemPriceService;
import org.openmrs.module.billing.api.ITimesheetService;
import org.openmrs.module.billing.api.PaymentModeService;
import org.openmrs.module.billing.api.billing.BillingEventListener;
import org.openmrs.module.billing.api.handler.BillReceiptNumberHandler;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillDiscount;
import org.openmrs.module.billing.api.model.BillRefund;
import org.openmrs.module.billing.validator.BillDiscountValidator;
import org.openmrs.module.billing.validator.BillRefundValidator;
import org.openmrs.module.billing.validator.BillValidator;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.cohort.definition.CohortDefinitionHandler;
import org.openmrs.module.cohort.definition.handler.DefaultCohortDefinitionHandler;
import org.openmrs.module.cohort.validators.CohortAttributeTypeValidator;
import org.openmrs.module.cohort.validators.CohortMValidator;
import org.openmrs.module.cohort.validators.CohortTypeValidator;
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
import org.openmrs.module.htmlwidgets.web.WidgetConfig;
import org.openmrs.module.htmlwidgets.web.handler.WidgetHandler;
import org.openmrs.module.htmlwidgets.web.html.Attribute;
import org.openmrs.module.htmlwidgets.web.html.HtmlUtil;
import org.openmrs.module.htmlwidgets.web.html.Option;
import org.openmrs.module.htmlwidgets.web.html.OptionGroup;
import org.openmrs.module.htmlwidgets.web.html.SelectWidget;
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
import org.openmrs.module.imaging.api.client.OrthancHttpClient;
import org.openmrs.module.initializer.api.InitializerService;
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
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.module.openconceptlab.Subscription;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.module.openconceptlab.scheduler.UpdateScheduler;
import org.openmrs.module.ordertemplates.OrderTemplatesConstants;
import org.openmrs.module.ordertemplates.api.OrderTemplatesService;
import org.openmrs.module.patientflags.aop.ConditionServiceAdvice;
import org.openmrs.module.patientflags.aop.EncounterServiceAdvice;
import org.openmrs.module.patientflags.aop.ObsServiceAdvice;
import org.openmrs.module.patientflags.aop.OrderServiceAdvice;
import org.openmrs.module.patientflags.aop.PatientServiceAdvice;
import org.openmrs.module.patientflags.aop.ProgramWorkflowServiceAdvice;
import org.openmrs.module.patientflags.Flag;
import org.openmrs.module.patientflags.PatientFlagsConstants;
import org.openmrs.module.patientflags.api.FlagService;
import org.openmrs.module.patientflags.task.PatientFlagTask;
import org.openmrs.module.patientflags.web.RefAppConfiguration;
import org.openmrs.module.patientflags.web.validators.FlagValidator;
import org.openmrs.module.patientflags.web.validators.PatientFlagsPropertiesValidator;
import org.openmrs.module.patientflags.web.validators.PriorityValidator;
import org.openmrs.module.patientflags.web.validators.TagValidator;
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
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeDTO;
import org.openmrs.module.stockmanagement.api.reporting.Report;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.util.HandlerUtil;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.core.api.StaticModuleTaskRunner;
import org.sihsalus.initializer.InitializerBoundary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SihsalusCoreApplicationTest {

    private static final String TEST_PATIENT_UUID = "2e29f6cc-14e4-44f5-bf57-c5cf0d7659f3";

    private static final String TEST_IDENTIFIER = "SIH-REST-FHIR-001";

    private static final String TEST_IDENTIFIER_TYPE_UUID = "f7c1c7d2-cf2d-45fd-9660-e81975cf50da";

    private static final String TEST_REQUIRED_IDENTIFIER =
            new LuhnMod30IdentifierValidator().getValidIdentifier("HC000001");

    private static final String REQUIRED_IDENTIFIER_TYPE_UUID = "05a29f94-c0ed-11e2-94be-8c13b969e334";

    private static final String TEST_ADMIN_USERNAME = "admin";

    private static final String TEST_ADMIN_PASSWORD = "Admin123";

    private static final String ADMIN_BASIC_AUTH = basicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);

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
    void requestTraceHeaderIsReturned() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "test-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-1"));
    }

    @Test
    void fhirMetadataResponds() throws Exception {
        mockMvc.perform(get("/api/fhir/metadata").header("Authorization", ADMIN_BASIC_AUTH))
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
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Basic")));
        mockMvc.perform(get("/api/system/info").header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dynamicOmodLoading").value(false));
    }

    @Test
    void adminAndLegacyModuleEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/static-modules"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Basic")));
        mockMvc.perform(get("/api/admin/static-modules").header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'webservices-rest')]").exists());
        mockMvc.perform(get("/module/htmlwidgets/patientSearch.form").param("q", "a"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sihsalusContentPackageIsLoadedIntoStaticBootDatabase() {
        assumeTrue(
                sihsalusContentConfigurationAvailable(),
                "reference-sources/sihsalus-content is a local reference clone and is not present in this checkout");

        assertSihsalusContentPackageLoaded();

        assertDoesNotThrow(() -> applicationContext.getBean(InitializerService.class).loadUnsafe(true, true));

        assertSihsalusContentPackageLoaded();
    }

    private void assertSihsalusContentPackageLoaded() {
        assertEquals(
                1,
                countRows("select count(*) from location where uuid = ?", "35d2234e-129a-4c40-abb2-1ae0b72c1602"));
        assertEquals(1, countRows("select count(*) from role where role = ?", "Organizational: Doctor"));
        assertEquals(1, countRows("select count(*) from privilege where privilege = ?", "O3 Implementer Tools"));
        assertEquals(
                "SIH SALUS",
                jdbcTemplate.queryForObject(
                        "select property_value from global_property where property = ?",
                        String.class,
                        "application.name"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from patient_identifier_type where uuid = ?",
                        "05a29f94-c0ed-11e2-94be-8c13b969e334"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from relationship_type where uuid = ?",
                        "057de23f-3d9c-4314-9391-4452970739c6"));
        assertEquals(
                1,
                countRows("select count(*) from visit_type where uuid = ?", "b1f0e8a1-9c5d-4f0e-8892-81f3140fbc09"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from encounter_type where uuid = ?",
                        "186c1e78-a99f-4cd0-86de-b8c4ee27a2b5"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from encounter_role where uuid = ?",
                        "240b26f9-dd88-4172-823d-4a8bfeb7841f"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from concept_class where uuid = ?",
                        "b4535251-9183-4175-959e-9ee67dc71e78"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from concept_reference_source where uuid = ?",
                        "1ADDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"));
        assertEquals(
                1,
                countRows("select count(*) from order_type where uuid = ?", "f9c5d0b8-8b5a-11e5-8e9b-12345678a01a"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from visit_attribute_type where uuid = ?",
                        "3a988e33-a6c0-4b76-b924-01abb998944b"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from provider_attribute_type where uuid = ?",
                        "0da4d3db-4385-40de-a4b0-fd8d89c4ec10"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from person_attribute_type where uuid = ?",
                        "14d4f066-15f5-102d-96e4-000c29c2a5d7"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from cashier_payment_mode where uuid = ?",
                        "526bf278-ba81-4436-b867-c2f6641d060a"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from cashier_payment_mode_attribute_type where name = ? and required = true",
                        "Maximum"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from cashier_cash_point where uuid = ?",
                        "422bbe3e-2742-4640-8ec9-4ee4fbf23374"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from cashier_billable_service where uuid = ?",
                        "5689a516-f9e4-4a88-b068-2ac0d132953e"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from appointment_speciality where uuid = ? and name = ?",
                        "9f2a8cd0-32c6-4844-8df7-1ac9c4d79943",
                        "Medicina General"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from appointment_service where uuid = ? and location_id is not null",
                        "7ba3aa21-cc56-47ca-bb4d-a60549f666c0"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from cohort_type where uuid = ? and name = ?",
                        "eee9970e-7ca0-4e8c-a280-c33e9d5f6a04",
                        "System List"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from cohort_attribute_type where uuid = ? and datatype = ?",
                        "70d8be3c-3a3c-4c64-b394-cee994f60cbe",
                        "org.openmrs.customdatatype.datatype.FreeTextDatatype"));
        assertEquals(
                "https://cielterminology.org",
                jdbcTemplate.queryForObject(
                        "select url from fhir_concept_source where uuid = ?",
                        String.class,
                        "2b3c1ff8-768a-102f-83f4-12313b04a615"));
        assertEquals(
                "https://santaclotilde.salud.gob.pe/fhir/sid/historia-clinica",
                jdbcTemplate.queryForObject(
                        "select url from fhir_patient_identifier_system where uuid = ?",
                        String.class,
                        "6000e616-4af9-4631-841a-f96a441875bd"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from idgen_identifier_source src "
                                + "join idgen_seq_id_gen seq on seq.id = src.id "
                                + "where src.uuid = ? and src.name = ? and seq.first_identifier_base = ? "
                                + "and seq.min_length = ?",
                        "8549f706-7e85-4c1d-9424-217d50a2988b",
                        "Generator for SIHSALUS",
                        "100000",
                        7));
        assertEquals(
                1,
                countRows(
                        "select count(*) from idgen_auto_generation_option "
                                + "where uuid = ? and manual_entry_enabled = false "
                                + "and automatic_generation_enabled = true",
                        "2be74f07-a4a0-4bfc-a943-8555d0074a74"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from metadatamapping_metadata_source where uuid = ? and name = ?",
                        "33bed25b-2ff7-49ec-ba52-bd066199d8b8",
                        "org.openmrs.module.emrapi"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from metadatamapping_metadata_set where uuid = ? and name = ?",
                        "f0ebcb99-7618-41b7-b0bf-8ff93de67b9e",
                        "Extra Patient Identifiers Set"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from metadatamapping_metadata_term_mapping mapping "
                                + "join metadatamapping_metadata_source source "
                                + "on source.metadata_source_id = mapping.metadata_source_id "
                                + "where source.name = ? and mapping.code = ? and mapping.metadata_uuid = ?",
                        "org.openmrs.module.emrapi",
                        "emr.primaryIdentifierType",
                        "05a29f94-c0ed-11e2-94be-8c13b969e334"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from concept_set conceptSet "
                                + "join concept parentConcept on parentConcept.concept_id = conceptSet.concept_set "
                                + "join concept memberConcept on memberConcept.concept_id = conceptSet.concept_id "
                                + "where parentConcept.uuid = ? and memberConcept.uuid = ? and conceptSet.sort_weight = ?",
                        "4bf3f465-ac91-44fa-9b1f-173daf0c89a0",
                        "7ba3aa21-cc56-47ca-bb4d-a60549f666c0",
                        1.0));
        assertEquals(
                1,
                countRows(
                        "select count(*) from order_frequency where uuid = ? and frequency_per_day = ?",
                        "136ebdb7-e989-47cf-8ec2-4e8b2ffe0ab3",
                        1.0));
        assertEquals(
                1,
                countRows(
                        "select count(*) from drug where uuid = ? and concept_id is not null "
                                + "and dosage_form is not null and strength = ?",
                        "0ee4c038-56b7-4ffe-808b-5a9ec5a00004",
                        "300 mg"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from program where uuid = ? and concept_id is not null and retired = false",
                        "b9db5c39-2855-4c61-9f25-9a7ec2d564bc"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from program_workflow where uuid = ? and program_id is not null "
                                + "and concept_id is not null",
                        "41dc3091-cf1c-4a54-bbc6-98b762c87f28"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from program_workflow_state where uuid = ? and initial = true "
                                + "and terminal = false",
                        "4470c9d2-8893-4dbf-bdcf-894104a9f9cb"));
        assertEquals(
                1,
                countRows(
                        "select count(*) from queue where uuid = ? and service is not null "
                                + "and status_concept_set is not null and priority_concept_set is not null",
                        "c3d4e5f6-a7b8-49c0-d1e2-f3a4b5c6d7e8"));
        assertEquals(7, countRows("select count(*) from address_hierarchy_level"));
        assertEquals(
                "País",
                jdbcTemplate.queryForObject(
                        "select name from address_hierarchy_level where address_field = ?",
                        String.class,
                        "COUNTRY"));
        assertTrue(
                countRows("select count(*) from address_hierarchy_entry") > 90000,
                "Peru address hierarchy entries should be loaded from sihsalus-content");
        assertEquals(
                1,
                countRows(
                        "select count(*) from address_hierarchy_entry where name = ? and user_generated_id = ?",
                        "CACLIC",
                        "0101010002"));
        assertTrue(
                jdbcTemplate.queryForObject(
                                "select property_value from global_property where property = ?",
                                String.class,
                                "layout.address.format")
                        .contains("Centro Poblado"));
        InitializerService initializerService = applicationContext.getBean(InitializerService.class);
        assertEquals("50mm", initializerService.getValueFromKey("report.patientIdSticker.size.height"));
        assertTrue(initializerService.getBooleanFromKey("report.patientIdSticker.barcode"));
        assertEquals(
                "Visitas Activas",
                Context.getMessageSourceService()
                        .getMessage("coreapps.app.activeVisits.label", null, Locale.forLanguageTag("es")));
        assertTrue(
                jdbcTemplate.queryForObject(
                                "select character_maximum_length from information_schema.columns where lower(table_name) = 'concept_name' and lower(column_name) = 'name'",
                                Integer.class)
                        >= 500);
    }

    private boolean sihsalusContentConfigurationAvailable() {
        Path sourceLayout = Paths.get(InitializerBoundary.sourceLayout());
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            Path configRoot = candidate.resolve(sourceLayout).resolve("configuration/backend_configuration").normalize();
            if (Files.isDirectory(configRoot)) {
                return true;
            }
        }
        return false;
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
        mockMvc.perform(get("/rest/v1/not-a-resource/not-a-real-uuid").header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.rawMessage").value("Unknown resource: v1/not-a-resource"));
    }

    @Test
    void queueEntryNumberLegacyEndpointRespondsSafelyWithoutParameters() throws Exception {
        assertTrue(QueueTimerTask.isEnabled());

        mockMvc.perform(get("/rest/v1/queue-entry-number").header("Authorization", ADMIN_BASIC_AUTH))
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
        assertOpenmrsValidatorRegistered(Queue.class, QueueValidator.class);
        assertOpenmrsValidatorRegistered(QueueEntry.class, QueueEntryValidator.class);
        assertOpenmrsValidatorRegistered(QueueRoom.class, QueueRoomValidation.class);
        assertOpenmrsValidatorRegistered(RoomProviderMap.class, RoomProviderMapValidator.class);
        assertOpenmrsValidatorRegistered(Visit.class, VisitWithQueueEntriesValidator.class);

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
        mockMvc.perform(get("/rest/v1/patientdocuments/patientIdSticker").header("Authorization", ADMIN_BASIC_AUTH))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/rest/v1/patientdocuments/patientIdSticker")
                        .param("patientUuid", TEST_PATIENT_UUID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/rest/v1/patientdocuments/encounters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + TEST_PATIENT_UUID + "\"]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cohortIsWiredAsStaticInternalModule() throws Exception {
        assertNotNull(Context.getService(CohortService.class));
        assertNotNull(Context.getService(CohortMemberService.class));
        assertNotNull(Context.getService(CohortTypeService.class));
        RestService restService = Context.getService(RestService.class);
        assertNotNull(restService.getResourceByName("v1/cohortm/cohort"));
        assertNotNull(restService.getResourceByName("v1/cohortm/cohortmember"));
        assertNotNull(restService.getResourceByName("v1/cohortm/cohorttype"));
        assertNotNull(restService.getResourceByName("v1/cohortm/cohortattributetype"));
        assertNotNull(restService.getResourceByName("v1/cohortm/cohort-member-attribute-type"));
        assertOpenmrsValidatorRegistered(CohortM.class, CohortMValidator.class);
        assertOpenmrsValidatorRegistered(Cohort.class, CohortMValidator.class);
        assertOpenmrsValidatorRegistered(CohortType.class, CohortTypeValidator.class);
        assertOpenmrsValidatorRegistered(CohortAttributeType.class, CohortAttributeTypeValidator.class);
        assertTrue(Context.getRegisteredComponents(CohortDefinitionHandler.class).stream()
                .anyMatch(DefaultCohortDefinitionHandler.class::isInstance));
        assertTrue(new CohortM().getDefinitionHandler() instanceof DefaultCohortDefinitionHandler);
        CohortM cohortWithInvalidHandler = new CohortM();
        cohortWithInvalidHandler.setDefinitionHandlerClassname(String.class.getName());
        assertThrows(APIException.class, cohortWithInvalidHandler::getDefinitionHandler);
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from cohort_type", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from cohort_attribute_type", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from cohort_member_attribute_type", Integer.class));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, () -> Context.getService(CohortService.class).findAll());
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(CohortMemberService.class).findAllCohortMembers());
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(CohortTypeService.class).findAllCohortTypes());
            mockMvc.perform(get("/rest/v1/cohortm/cohort/not-a-real-cohort"))
                    .andExpect(status().isUnauthorized());
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
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
    void htmlWidgetsIsWiredAsStaticInternalModule() throws Exception {
        HtmlWidgetsService htmlWidgetsService = Context.getService(HtmlWidgetsService.class);
        assertNotNull(htmlWidgetsService);
        assertNotNull(Context.getRegisteredComponents(WidgetHandler.class).stream()
                .findFirst()
                .orElse(null));

        StringWriter tagWriter = new StringWriter();
        HtmlUtil.renderSimpleTag(
                tagWriter,
                "input",
                List.of(new Attribute("value", "\"><script>alert(1)</script>", null, null)));
        assertEquals("<input value=\"&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;\"/>", tagWriter.toString());

        assertEquals("\\u003C/script\\u003E\\u0026\\\"\\'", HtmlUtil.escapeJavaScriptString("</script>&\"'"));

        SelectWidget selectWidget = new SelectWidget();
        OptionGroup group = new OptionGroup("Group \"<x>", null);
        selectWidget.getOptions().add(new Option("a\"<", "<b>A</b>", null, "a", group));
        selectWidget.getOptions().add(new Option("b", "B", null, "b"));
        WidgetConfig config = new WidgetConfig();
        config.setFixedAttribute("name", "choice");
        config.setDefaultValue("a");
        StringWriter selectWriter = new StringWriter();
        selectWidget.render(config, selectWriter);
        assertTrue(selectWriter.toString().contains("<optgroup label=\"Group &quot;&lt;x&gt;\">"));
        assertTrue(selectWriter.toString()
                .contains("<option value=\"a&quot;&lt;\" selected=\"true\">&lt;b&gt;A&lt;/b&gt;</option>"));
        assertTrue(selectWriter.toString().contains("</optgroup><option value=\"b\">B</option>"));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, () -> htmlWidgetsService.getAllObjectsByType(User.class));
            assertThrows(APIAuthenticationException.class, () -> htmlWidgetsService.getUserNamesById("admin", List.of()));
            assertThrows(APIAuthenticationException.class, () -> htmlWidgetsService.getPersonNamesById("admin", List.of()));
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
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
    void stockManagementVoidUserRoleScopesUsesAuthenticatedUser() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeTest = Context.isAuthenticated();
        if (!authenticatedBeforeTest) {
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }

        try {
            Integer adminUserId = jdbcTemplate.queryForObject(
                    "select user_id from users where username = ?",
                    Integer.class,
                    TEST_ADMIN_USERNAME);
            String role = jdbcTemplate.queryForObject("select role from role order by role limit 1", String.class);
            String scopeUuid = UUID.randomUUID().toString();

            jdbcTemplate.update(
                    "insert into stockmgmt_user_role_scope "
                            + "(user_id, role, is_permanent, enabled, creator, date_created, voided, uuid) "
                            + "values (?, ?, true, true, ?, current_timestamp, false, ?)",
                    adminUserId,
                    role,
                    adminUserId,
                    scopeUuid);

            StockManagementService stockManagementService = Context.getService(StockManagementService.class);
            assertDoesNotThrow(() -> stockManagementService.voidUserRoleScopes(List.of(scopeUuid), "test void"));

            assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
                    "select voided from stockmgmt_user_role_scope where uuid = ?",
                    Boolean.class,
                    scopeUuid));
            assertEquals(adminUserId, jdbcTemplate.queryForObject(
                    "select voided_by from stockmgmt_user_role_scope where uuid = ?",
                    Integer.class,
                    scopeUuid));
        } finally {
            if (!authenticatedBeforeTest && Context.isSessionOpen()) {
                Context.logout();
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void stockManagementSaveUserRoleScopeAcceptsDtoPayloads() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeTest = Context.isAuthenticated();
        if (!authenticatedBeforeTest) {
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }

        try {
            List<String> userUuids = jdbcTemplate.queryForList(
                    "select uuid from users where username <> ? order by user_id limit 1",
                    String.class,
                    TEST_ADMIN_USERNAME);
            assumeTrue(!userUuids.isEmpty());

            String role = jdbcTemplate.queryForObject("select role from role order by role limit 1", String.class);
            Integer existingScopes = jdbcTemplate.queryForObject(
                    "select count(*) from stockmgmt_user_role_scope urs "
                            + "join users u on u.user_id = urs.user_id "
                            + "where u.uuid = ? and urs.role = ? and urs.voided = false",
                    Integer.class,
                    userUuids.get(0),
                    role);
            UserRoleScopeDTO dto = new UserRoleScopeDTO();
            dto.setUserUuid(userUuids.get(0));
            dto.setRole(role);
            dto.setPermanent(true);
            dto.setEnabled(true);
            dto.setLocations(Collections.emptyList());
            dto.setOperationTypes(Collections.emptyList());

            StockManagementService stockManagementService = Context.getService(StockManagementService.class);
            assertDoesNotThrow(() -> stockManagementService.saveUserRoleScope(dto));

            assertEquals(existingScopes + 1, jdbcTemplate.queryForObject(
                    "select count(*) from stockmgmt_user_role_scope urs "
                            + "join users u on u.user_id = urs.user_id "
                            + "where u.uuid = ? and urs.role = ? and urs.voided = false",
                    Integer.class,
                    userUuids.get(0),
                    role));
        } finally {
            if (!authenticatedBeforeTest && Context.isSessionOpen()) {
                Context.logout();
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void stockManagementPackagingUomLookupPrefersNonVoidedRows() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeTest = Context.isAuthenticated();
        if (!authenticatedBeforeTest) {
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }

        try {
            Integer adminUserId = jdbcTemplate.queryForObject(
                    "select user_id from users where username = ?",
                    Integer.class,
                    TEST_ADMIN_USERNAME);
            Integer conceptId = jdbcTemplate.queryForObject(
                    "select concept_id from concept where retired = false order by concept_id limit 1",
                    Integer.class);
            String stockItemUuid = UUID.randomUUID().toString();

            jdbcTemplate.update(
                    "insert into stockmgmt_stock_item "
                            + "(concept_id, has_expiration, common_name, is_drug, creator, date_created, voided, uuid) "
                            + "values (?, false, ?, false, ?, current_timestamp, false, ?)",
                    conceptId,
                    "Test stock item " + stockItemUuid,
                    adminUserId,
                    stockItemUuid);
            Integer stockItemId = jdbcTemplate.queryForObject(
                    "select stock_item_id from stockmgmt_stock_item where uuid = ?",
                    Integer.class,
                    stockItemUuid);

            String voidedUomUuid = UUID.randomUUID().toString();
            jdbcTemplate.update(
                    "insert into stockmgmt_stock_item_packaging_uom "
                            + "(stock_item_id, packaging_uom_id, factor, creator, date_created, voided, uuid) "
                            + "values (?, ?, 1, ?, current_timestamp, true, ?)",
                    stockItemId,
                    conceptId,
                    adminUserId,
                    voidedUomUuid);

            String activeUomUuid = UUID.randomUUID().toString();
            jdbcTemplate.update(
                    "insert into stockmgmt_stock_item_packaging_uom "
                            + "(stock_item_id, packaging_uom_id, factor, creator, date_created, voided, uuid) "
                            + "values (?, ?, 1, ?, current_timestamp, false, ?)",
                    stockItemId,
                    conceptId,
                    adminUserId,
                    activeUomUuid);
            Integer activeUomId = jdbcTemplate.queryForObject(
                    "select stock_item_packaging_uom_id from stockmgmt_stock_item_packaging_uom where uuid = ?",
                    Integer.class,
                    activeUomUuid);

            StockManagementService stockManagementService = Context.getService(StockManagementService.class);
            assertEquals(activeUomId,
                    stockManagementService.getStockItemPackagingUOMByConcept(stockItemId, conceptId).getId());
        } finally {
            if (!authenticatedBeforeTest && Context.isSessionOpen()) {
                Context.logout();
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void stockManagementReportsExposeStaticCatalog() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeTest = Context.isAuthenticated();
        if (!authenticatedBeforeTest) {
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }

        try {
            StockManagementService stockManagementService = Context.getService(StockManagementService.class);
            List<Report> reports = stockManagementService.getReports();

            assertEquals(Report.getAllReports().size(), reports.size());
            assertTrue(reports.stream().anyMatch(report -> "STOCK_STATUS_REPORT".equals(report.getSystemName())));
            assertTrue(reports.stream().anyMatch(report -> "STOCK_EXPIRY_REPORT".equals(report.getSystemName())));
        } finally {
            if (!authenticatedBeforeTest && Context.isSessionOpen()) {
                Context.logout();
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void stockManagementStockItemReferencesReturnsEmptyListForMissingItem() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeTest = Context.isAuthenticated();
        if (!authenticatedBeforeTest) {
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }

        try {
            StockManagementService stockManagementService = Context.getService(StockManagementService.class);
            assertTrue(stockManagementService.getStockItemReferenceByStockItem(UUID.randomUUID().toString()).isEmpty());
        } finally {
            if (!authenticatedBeforeTest && Context.isSessionOpen()) {
                Context.logout();
            }
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
                .andExpect(status().isUnauthorized());
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
    void appointmentsIsWiredAsStaticInternalModule() throws Exception {
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
        assertValidatorList(
                getAdvisedTarget(appointmentsService, AppointmentsServiceImpl.class),
                "appointmentValidators",
                DefaultAppointmentValidator.class);
        assertValidatorList(
                getAdvisedTarget(appointmentsService, AppointmentsServiceImpl.class),
                "editAppointmentValidators",
                DefaultEditAppointmentValidator.class);
        assertValidatorList(
                getAdvisedTarget(recurringPatternService, AppointmentRecurringPatternServiceImpl.class),
                "appointmentValidators",
                DefaultAppointmentValidator.class);
        assertValidatorList(
                getAdvisedTarget(recurringPatternService, AppointmentRecurringPatternServiceImpl.class),
                "editAppointmentValidators",
                DefaultEditAppointmentValidator.class);
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
    void bedManagementIsWiredAsStaticInternalModule() throws Exception {
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
            assertThrows(APIAuthenticationException.class, () -> bedManagementService.getBedById(1));
            mockMvc.perform(get("/rest/v1/bedPatientAssignment/not-a-real-assignment"))
                    .andExpect(status().isUnauthorized());
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void billingIsWiredAsStaticInternalModule() throws Exception {
        assertNotNull(Context.getService(BillService.class));
        assertNotNull(Context.getService(PaymentModeService.class));
        assertNotNull(Context.getService(CashierItemPriceService.class));
        assertNotNull(Context.getService(BillExemptionService.class));
        assertNotNull(Context.getService(BillLineItemService.class));
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
        assertOpenmrsValidatorRegistered(Bill.class, BillValidator.class);
        assertOpenmrsValidatorRegistered(BillDiscount.class, BillDiscountValidator.class);
        assertOpenmrsValidatorRegistered(BillRefund.class, BillRefundValidator.class);
        List<SaveHandler> billSaveHandlers = HandlerUtil.getHandlersForType(SaveHandler.class, Bill.class);
        assertTrue(
                billSaveHandlers.stream().anyMatch(BillReceiptNumberHandler.class::isInstance),
                () -> "Bill SaveHandlers: " + billSaveHandlers.stream()
                        .map(handler -> handler.getClass().getName())
                        .toList());

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

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, () -> Context.getService(BillService.class).getBill(1));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(PaymentModeService.class).getPaymentModes(false));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(CashierItemPriceService.class).getCashierItemPrices(false));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(BillExemptionService.class).getBillingExemptionById(1));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(BillLineItemService.class).getBillLineItemByUuid("not-a-real-line-item"));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(ITimesheetService.class).getCurrentTimesheet(null));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(ITimesheetService.class).getTimesheetsByDate(null, new Date()));
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(ITimesheetService.class).closeOpenTimesheets());
            mockMvc.perform(get("/rest/v1/billing/bill/not-a-real-bill"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/rest/v2/billing/timesheet/not-a-real-timesheet"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/rest/v2/billing/timesheet")
                    .param("date", "01/01/2026"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(post("/rest/v1/billing/bill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/rest/v1/billing/bill")
                    .header("Authorization", ADMIN_BASIC_AUTH)
                    .param("status", "not-a-real-status"))
                    .andExpect(status().isBadRequest());
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
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
        OrthancHttpClient orthancHttpClient = applicationContext.getBean(OrthancHttpClient.class);
        assertNotNull(orthancHttpClient);
        assertEquals("study%2Fid%20with%20space", OrthancHttpClient.encodePathSegment("study/id with space"));
        assertThrows(IOException.class,
                () -> orthancHttpClient.createConnection("GET", "file:///tmp/orthanc", "/system", "", ""));
        assertThrows(IOException.class,
                () -> orthancHttpClient.createConnection("GET", "https://user:pass@example.org", "/system", "", ""));
        assertThrows(IOException.class,
                () -> orthancHttpClient.createConnection("GET", "https://example.org", "//evil.example/system", "", ""));
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
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(DicomStudyService.class).getAllStudies());
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(RequestProcedureService.class).getAllRequestProcedures());
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(RequestProcedureStepService.class).getProcedureStep(1));
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
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }
        InteropQueueItem queuedItem = null;
        try {
            queuedItem = dyakuSenderService.queueMessage(
                    "FHIR_BUNDLE",
                    "{\"resourceType\":\"Bundle\",\"type\":\"transaction\"}",
                    "http://request-controlled.example/fhir");
            String configuredEndpoint = jdbcTemplate.queryForObject(
                    "select property_value from global_property where property = ?",
                    String.class,
                    "sihsalusinterop.renhice.endpoint");
            assertEquals(configuredEndpoint, queuedItem.getTargetEndpoint());
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
                    .andExpect(status().isUnauthorized());
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
        AttachmentsService attachmentsService = Context.getService(AttachmentsService.class);
        assertNotNull(attachmentsService);
        assertNotNull(Context.getObsService().getHandler(DefaultAttachmentHandler.class.getSimpleName()));
        assertNotNull(Context.getObsService().getHandler(ImageAttachmentHandler.class.getSimpleName()));
        AttachmentResource attachmentResource =
                (AttachmentResource) Context.getService(RestService.class).getResourceByName("v1/attachment");
        assertNotNull(attachmentResource);
        AttachmentBytesResource attachmentBytesResource = applicationContext.getBean(AttachmentBytesResource.class);
        assertNotNull(attachmentBytesResource);
        assertEquals(AttachmentsConstants.VIEW_ATTACHMENTS, attachmentResource.getRequiredGetPrivilege());
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (?, ?)",
                Integer.class,
                AttachmentsConstants.CREATE_ATTACHMENTS,
                AttachmentsConstants.VIEW_ATTACHMENTS));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from global_property where property = 'attachments.defaultConceptComplexUuid'",
                Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from concept_name where uuid in (?, ?)",
                Integer.class,
                "8f3a26f8-7e8f-4a53-84c7-c3ff48bde417",
                "8f3a26f8-7e8f-4a53-84c7-c3ff48bde419"));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        UserContext originalUserContext = openedSession ? null : Context.getUserContext();
        try {
            Context.setUserContext(new UserContext(Context.getAuthenticationScheme()));
            assertThrows(APIAuthenticationException.class, () -> attachmentsService.getAttachments((Patient) null, false));
            assertThrows(ContextAuthenticationException.class, () -> attachmentResource.getByUniqueId("not-a-real-uuid"));
            assertThrows(ContextAuthenticationException.class,
                    () -> attachmentBytesResource.getFile("not-a-real-uuid", null, new MockHttpServletResponse()));
            assertThrows(ContextAuthenticationException.class,
                    () -> attachmentResource.upload(
                            new MockMultipartFile(
                                    "file",
                                    "../unsafe|name.txt",
                                    "text/plain",
                                    "hello".getBytes(StandardCharsets.UTF_8)),
                            new RequestContext()));
            mockMvc.perform(get("/rest/v1/attachment/not-a-real-uuid"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/rest/v1/attachment/not-a-real-uuid/bytes"))
                    .andExpect(status().isUnauthorized());
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
    void patientFlagsIsWiredAsStaticInternalModule() throws Exception {
        FlagService flagService = Context.getService(FlagService.class);
        assertNotNull(flagService);
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/flag"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/tag"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/priority"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/displaypoint"));
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/patientflags/patientflag"));
        assertNotNull(applicationContext.getBean(FlagValidator.class));
        assertNotNull(applicationContext.getBean(TagValidator.class));
        assertNotNull(applicationContext.getBean(PriorityValidator.class));
        assertNotNull(applicationContext.getBean(PatientFlagsPropertiesValidator.class));
        assertNotNull(applicationContext.getBean(RefAppConfiguration.class));

        assertEquals(5, jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (?, ?, ?, ?, ?)",
                Integer.class,
                PatientFlagsConstants.PRIV_MANAGE_FLAGS,
                PatientFlagsConstants.PRIV_VIEW_FLAGS,
                PatientFlagsConstants.PRIV_MANAGE_PATIENT_FLAGS,
                PatientFlagsConstants.PRIV_VIEW_PATIENT_FLAGS,
                PatientFlagsConstants.PRIV_TEST_FLAGS));
        assertEquals(3, jdbcTemplate.queryForObject(
                "select count(*) from global_property where property in (?, ?, ?)",
                Integer.class,
                "patientflags.patientHeaderDisplay",
                "patientflags.patientOverviewDisplay",
                "patientflags.username"));

        assertAdviceRegistered(Context.getEncounterService(), EncounterServiceAdvice.class);
        assertAdviceRegistered(Context.getObsService(), ObsServiceAdvice.class);
        assertAdviceRegistered(Context.getOrderService(), OrderServiceAdvice.class);
        assertAdviceRegistered(Context.getPatientService(), PatientServiceAdvice.class);
        assertAdviceRegistered(Context.getConditionService(), ConditionServiceAdvice.class);
        assertAdviceRegistered(Context.getProgramWorkflowService(), ProgramWorkflowServiceAdvice.class);

        Flag invalidEvaluatorFlag = new Flag("Invalid evaluator", "select p.patient_id from patient p", "message");
        invalidEvaluatorFlag.setEvaluator(String.class.getName());
        assertThrows(APIException.class, invalidEvaluatorFlag::instantiateEvaluator);

        PatientFlagTask.setDaemonToken(null);
        PatientFlagTask.evaluateAllFlags().run();

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class, flagService::getAllFlags);
            mockMvc.perform(get("/rest/v1/patientflags/flag"))
                    .andExpect(status().isUnauthorized());
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
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
    void orderTemplatesIsWiredAsStaticInternalModule() throws Exception {
        OrderTemplatesService orderTemplatesService = Context.getService(OrderTemplatesService.class);
        assertNotNull(orderTemplatesService);
        assertNotNull(Context.getService(RestService.class).getResourceByName("v1/ordertemplates/orderTemplate"));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from order_template", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from privilege where privilege in (?, ?)",
                Integer.class,
                OrderTemplatesConstants.VIEW_ORDER_TEMPLATES,
                OrderTemplatesConstants.MANAGE_ORDER_TEMPLATES));

        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        try {
            Context.logout();
            assertThrows(APIAuthenticationException.class,
                    () -> Context.getService(OrderTemplatesService.class).getAllOrderTemplates(false));
            mockMvc.perform(get("/rest/v1/ordertemplates/orderTemplate/not-a-real-template"))
                    .andExpect(status().isUnauthorized());
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    @Test
    void openConceptLabIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(ImportService.class));
        assertNotNull(Context.getService(OclConceptService.class));
        assertNotNull(Context.getRegisteredComponent("openconceptlab.importer", Importer.class));
        assertNotNull(Context.getRegisteredComponent("openconceptlab.updateScheduler", UpdateScheduler.class));
        assertTrue(ModuleFactory.getExtensions("org.openmrs.admin.list", Extension.MEDIA_TYPE.html).stream()
                .anyMatch(org.openmrs.module.openconceptlab.extension.html.AdminList.class::isInstance));
        assertTrue(ModuleFactory.getExtensions("org.openmrs.dictionary.conceptHeader", Extension.MEDIA_TYPE.html).stream()
                .anyMatch(org.openmrs.module.openconceptlab.extension.html.HighlightSubscribedConcept.class::isInstance));

        RestService restService = Context.getService(RestService.class);
        assertNotNull(restService.getResourceByName("v1/openconceptlab/import"));
        assertNotNull(restService.getResourceByName("v1/openconceptlab/importaction"));
        assertNotNull(restService.getResourceByName("v1/openconceptlab/subscription"));
        assertEquals(4, jdbcTemplate.queryForObject(
                "select count(*) from global_property where property in (?, ?, ?, ?)",
                Integer.class,
                OpenConceptLabConstants.GP_SUBSCRIPTION_URL,
                OpenConceptLabConstants.GP_SCHEDULED_DAYS,
                OpenConceptLabConstants.GP_SCHEDULED_TIME,
                OpenConceptLabConstants.GP_TOKEN));
        Subscription subscription = new Subscription();
        subscription.setUrl(null);
        subscription.setToken("super-secret-token");
        assertNull(subscription.getUrl());
        assertFalse(subscription.toString().contains("super-secret-token"));

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

            User systemUser = new User(1);
            ensureTestIdentifierTypeExists();
            PatientIdentifierType identifierType =
                    Context.getPatientService().getPatientIdentifierTypeByUuid(TEST_IDENTIFIER_TYPE_UUID);
            assertNotNull(identifierType);
            PatientIdentifierType requiredIdentifierType =
                    Context.getPatientService().getPatientIdentifierTypeByUuid(REQUIRED_IDENTIFIER_TYPE_UUID);

            java.util.Date now = new java.util.Date();
            Patient existing = Context.getPatientService().getPatientByUuid(TEST_PATIENT_UUID);
            if (existing != null) {
                boolean changed = addIdentifierIfMissing(existing, TEST_IDENTIFIER, identifierType, true, systemUser, now);
                if (requiredIdentifierType != null) {
                    changed |= addIdentifierIfMissing(
                            existing, TEST_REQUIRED_IDENTIFIER, requiredIdentifierType, false, systemUser, now);
                }
                if (changed) {
                    return Context.getPatientService().savePatient(existing).getUuid();
                }
                return existing.getUuid();
            }

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

            addIdentifierIfMissing(patient, TEST_IDENTIFIER, identifierType, true, systemUser, now);
            if (requiredIdentifierType != null) {
                addIdentifierIfMissing(patient, TEST_REQUIRED_IDENTIFIER, requiredIdentifierType, false, systemUser, now);
            }

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

    private boolean addIdentifierIfMissing(
            Patient patient,
            String identifierValue,
            PatientIdentifierType identifierType,
            boolean preferred,
            User creator,
            java.util.Date now) {
        boolean hasIdentifier = patient.getActiveIdentifiers().stream()
                .anyMatch(identifier -> identifierValue.equals(identifier.getIdentifier())
                        && identifier.getIdentifierType() != null
                        && identifierType.getUuid().equals(identifier.getIdentifierType().getUuid()));
        if (hasIdentifier) {
            return false;
        }

        PatientIdentifier identifier = new PatientIdentifier(identifierValue, identifierType, null);
        identifier.setPreferred(preferred);
        identifier.setCreator(creator);
        identifier.setDateCreated(now);
        patient.addIdentifier(identifier);
        return true;
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

    private void assertOpenmrsValidatorRegistered(Class<?> supportedClass, Class<? extends Validator> validatorClass) {
        List<Validator> validators = HandlerUtil.getHandlersForType(Validator.class, supportedClass);
        assertTrue(
                validators.stream().anyMatch(validatorClass::isInstance),
                () -> supportedClass.getSimpleName() + " Validators: " + validators.stream()
                        .map(validator -> validator.getClass().getName())
                        .toList());
    }

    private int countRows(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
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

    private <T> T getAdvisedTarget(Object service, Class<T> targetClass) throws Exception {
        assertTrue(service instanceof Advised);
        Object target = ((Advised) service).getTargetSource().getTarget();
        assertTrue(targetClass.isInstance(target));
        return targetClass.cast(target);
    }

    private void assertValidatorList(Object target, String fieldName, Class<?> validatorClass) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        List<?> validators = (List<?>) field.get(target);
        assertEquals(1, validators.size());
        assertTrue(validatorClass.isInstance(validators.get(0)));
    }
}
