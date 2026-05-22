package org.sihsalus.fhir2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.openmrs.api.APIAuthenticationException;
import org.springframework.http.ResponseEntity;

class FhirR4ReadControllerTest {

    @Test
    void readsResourceFromR4Provider() {
        FhirR4ReadController controller =
                new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new PatientProvider()));

        ResponseEntity<String> response = controller.read("Patient", "patient-uuid");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"resourceType\":\"Patient\""));
        assertTrue(response.getBody().contains("\"id\":\"patient-uuid\""));
    }

    @Test
    void convertsProviderExceptionToOperationOutcome() {
        FhirR4ReadController controller =
                new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new MissingPatientProvider()));

        ResponseEntity<String> response = controller.read("Patient", "missing");

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
        assertTrue(response.getBody().contains("Could not find patient with Id missing"));
    }

    @Test
    void convertsAnonymousAuthorizationExceptionToOperationOutcome() {
        FhirR4ReadController controller =
                new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new UnauthorizedPatientProvider()));

        ResponseEntity<String> response = controller.read("Patient", "secured");

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
        assertTrue(response.getBody().contains("FHIR read requires privileges"));
    }

    private static class PatientProvider implements IResourceProvider {

        @Override
        public Class<? extends IBaseResource> getResourceType() {
            return Patient.class;
        }

        @Read
        @SuppressWarnings("unused")
        public Patient read(@IdParam IdType id) {
            Patient patient = new Patient();
            patient.setId(id.getIdPart());
            return patient;
        }
    }

    private static final class MissingPatientProvider extends PatientProvider {

        @Override
        @Read
        public Patient read(@IdParam IdType id) {
            throw new ResourceNotFoundException("Could not find patient with Id " + id.getIdPart());
        }
    }

    private static final class UnauthorizedPatientProvider extends PatientProvider {

        @Override
        @Read
        public Patient read(@IdParam IdType id) {
            throw new APIAuthenticationException("FHIR read requires privileges");
        }
    }
}
