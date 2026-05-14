package org.sihsalus.core.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openmrs.UserSessionListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.authentication.DelegatingAuthenticationScheme;
import org.openmrs.module.authentication.AuthenticationUserSessionListener;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.idgen.validator.LuhnMod10IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod25IdentifierValidator;
import org.openmrs.module.idgen.validator.LuhnMod30IdentifierValidator;
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
    void addressHierarchyIsWiredAsStaticInternalModule() {
        assertNotNull(Context.getService(AddressHierarchyService.class));
        assertNotNull(
                jdbcTemplate.queryForObject("select count(*) from address_hierarchy_level", Integer.class));
    }
}
