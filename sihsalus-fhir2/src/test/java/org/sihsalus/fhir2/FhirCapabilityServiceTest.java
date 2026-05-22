package org.sihsalus.fhir2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;

class FhirCapabilityServiceTest {

    @Test
    void producesR4CapabilityStatement() {
        CapabilityStatement statement =
                new FhirCapabilityService(FhirContext.forR4Cached(), List.of(new PatientProvider())).capabilityStatement();

        assertEquals("4.0.1", statement.getFhirVersion().toCode());
        assertEquals("Patient", statement.getRestFirstRep().getResourceFirstRep().getType());
        assertTrue(statement.getRestFirstRep().getResourceFirstRep().getInteraction().stream()
                .anyMatch(interaction -> "read".equals(interaction.getCode().toCode())));
        assertFalse(statement.getRestFirstRep().getResourceFirstRep().getInteraction().stream()
                .anyMatch(interaction -> "create".equals(interaction.getCode().toCode())));
    }

    private static final class PatientProvider implements IResourceProvider {

        @Override
        public Class<? extends IBaseResource> getResourceType() {
            return Patient.class;
        }

        @Read
        @SuppressWarnings("unused")
        public Patient read(@IdParam IdType id) {
            return new Patient();
        }

        @Create
        @SuppressWarnings("unused")
        public Patient create(Patient patient) {
            return patient;
        }
    }
}
