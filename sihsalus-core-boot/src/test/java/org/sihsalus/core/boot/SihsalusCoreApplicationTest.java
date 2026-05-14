package org.sihsalus.core.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.UserSessionListener;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.api.CalculationRegistrationService;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.attachments.AttachmentsService;
import org.openmrs.module.attachments.obs.DefaultAttachmentHandler;
import org.openmrs.module.attachments.obs.ImageAttachmentHandler;
import org.openmrs.module.authentication.AuthenticationUserSessionListener;
import org.openmrs.module.authentication.DelegatingAuthenticationScheme;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.concept.EmrConceptService;
import org.openmrs.module.emrapi.patient.EmrPatientService;
import org.openmrs.module.emrapi.procedure.ProcedureService;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsService;
import org.openmrs.module.htmlwidgets.web.handler.WidgetHandler;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.idgen.validator.LuhnMod10IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod25IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod30IdentifierValidator;
import org.openmrs.module.imaging.api.DicomStudyService;
import org.openmrs.module.imaging.api.OrthancConfigurationService;
import org.openmrs.module.imaging.api.RequestProcedureService;
import org.openmrs.module.imaging.api.RequestProcedureStepService;
import org.openmrs.module.legacyui.api.LegacyUIService;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.OAuth2UserInfoAuthenticationScheme;
import org.openmrs.module.o3forms.api.O3FormsService;
import org.openmrs.module.ordertemplates.api.OrderTemplatesService;
import org.openmrs.module.patientdocuments.reports.PatientIdStickerPdfReport;
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
import org.openmrs.module.reporting.serializer.ReportingSerializer;
import org.openmrs.module.reportingrest.adhoc.AdHocExportManager;
import org.openmrs.module.serialization.xstream.XStreamSerializer;
import org.openmrs.module.serialization.xstream.XStreamShortSerializer;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.util.HandlerUtil;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SihsalusCoreApplicationTest {

    private static final String TEST_PATIENT_UUID = "2e29f6cc-14e4-44f5-bf57-c5cf0d7659f3";

    private static final String TEST_IDENTIFIER = "SIH-REST-FHIR-001";

    private static final String TEST_IDENTIFIER_TYPE_UUID = "f7c1c7d2-cf2d-45fd-9660-e81975cf50da";

    @Autowired private MockMvc mockMvc;

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
        mockMvc.perform(get("/api/fhir/r4/Patient/not-a-real-patient"))
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
    void restV1ControllerIsWiredWithoutOmodRuntime() throws Exception {
        mockMvc.perform(get("/rest/v1/not-a-resource/not-a-real-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.rawMessage").value("Unknown resource: v1/not-a-resource"));
    }

    @Test
    void patientRegistryPatientIsReadableThroughRestAndFhir() throws Exception {
        String patientUuid = ensureTestPatient();

        mockMvc.perform(get("/rest/v1/patient/{uuid}", patientUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(patientUuid))
                .andExpect(jsonPath("$.identifier").value(TEST_IDENTIFIER))
                .andExpect(jsonPath("$.givenName").value("Sihsalus"))
                .andExpect(jsonPath("$.familyName").value("Paciente"));

        assertNotNull(jdbcTemplate.queryForObject("select count(*) from fhir_patient_identifier_system", Integer.class));

        mockMvc.perform(get("/api/fhir/r4/Patient/{uuid}", patientUuid))
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
        assertNotNull(Context.getService(O3FormsService.class));
        mockMvc.perform(get("/rest/v1/o3/forms/not-a-real-form")).andExpect(status().isNotFound());
    }

    @Test
    void patientDocumentsIsWiredAsStaticInternalModule() throws Exception {
        assertNotNull(Context.getRegisteredComponent("patientIdStickerPdfReport", PatientIdStickerPdfReport.class));
        mockMvc.perform(get("/rest/v1/patientdocuments/patientIdSticker")).andExpect(status().isBadRequest());
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
    void legacyUiIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(LegacyUIService.class));
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
    }

    @Test
    void fuaIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(FuaService.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from fua", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from fua_estado", Integer.class));
    }

    @Test
    void imagingIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(OrthancConfigurationService.class));
        assertNotNull(Context.getService(DicomStudyService.class));
        assertNotNull(Context.getService(RequestProcedureService.class));
        assertNotNull(Context.getService(RequestProcedureStepService.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_orthancconfiguration", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_dicomstudy", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_requestprocedure", Integer.class));
        assertNotNull(jdbcTemplate.queryForObject("select count(*) from imaging_requestprocedurestep", Integer.class));
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

    private void restartIdentityAfterSeedData(String tableName, String columnName) {
        Integer nextValue = jdbcTemplate.queryForObject(
                "select coalesce(max(" + columnName + "), 0) + 1000 from " + tableName, Integer.class);
        jdbcTemplate.execute("alter table " + tableName + " alter column " + columnName + " restart with " + nextValue);
    }
}
