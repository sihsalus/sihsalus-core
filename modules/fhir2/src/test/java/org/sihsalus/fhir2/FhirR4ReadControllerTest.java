package org.sihsalus.fhir2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.openmrs.api.APIAuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
        new FhirR4ReadController(
            FhirContext.forR4Cached(), List.of(new UnauthorizedPatientProvider()));

    ResponseEntity<String> response = controller.read("Patient", "secured");

    assertEquals(401, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("FHIR read requires privileges"));
  }

  @Test
  void searchesResourcesFromR4Provider() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new PatientProvider()));

    ResponseEntity<String> response = controller.search("Patient", searchParameters("_count", "1"));

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"Bundle\""));
    assertTrue(response.getBody().contains("\"type\":\"searchset\""));
    assertTrue(response.getBody().contains("\"resourceType\":\"Patient\""));
    assertTrue(response.getBody().contains("\"id\":\"patient-search-uuid\""));
  }

  @Test
  void filtersSearchResourcesByMetaTag() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new LocationProvider()));
    LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("_summary", "data");
    parameters.add("_count", "50");
    parameters.add("_tag", "Login Location");

    ResponseEntity<String> response = controller.search("Location", parameters);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"Bundle\""));
    assertTrue(response.getBody().contains("\"total\":1"));
    assertTrue(response.getBody().contains("\"id\":\"login-location\""));
    assertTrue(response.getBody().contains("\"code\":\"Login Location\""));
    assertFalse(response.getBody().contains("\"id\":\"other-location\""));
  }

  @Test
  void rejectsUnsupportedSearchParametersInsteadOfIgnoringFilters() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new PatientProvider()));

    ResponseEntity<String> response =
        controller.search("Patient", searchParameters(Patient.SP_IDENTIFIER, "12345"));

    assertEquals(400, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("identifier"));
  }

  private static MultiValueMap<String, String> searchParameters(String name, String value) {
    LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add(name, value);
    return parameters;
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

    @Search
    @SuppressWarnings("unused")
    public IBundleProvider search() {
      Patient patient = new Patient();
      patient.setId("patient-search-uuid");
      return new SimpleBundleProvider(List.of(patient));
    }
  }

  private static final class LocationProvider implements IResourceProvider {

    @Override
    public Class<? extends IBaseResource> getResourceType() {
      return Location.class;
    }

    @Search
    @SuppressWarnings("unused")
    public IBundleProvider search() {
      Location loginLocation = new Location();
      loginLocation.setId("login-location");
      loginLocation.getMeta().addTag().setCode("Login Location");

      Location otherLocation = new Location();
      otherLocation.setId("other-location");
      otherLocation.getMeta().addTag().setCode("Other");

      return new SimpleBundleProvider(List.of(otherLocation, loginLocation));
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
