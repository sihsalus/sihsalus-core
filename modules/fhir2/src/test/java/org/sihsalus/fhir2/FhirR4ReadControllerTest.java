package org.sihsalus.fhir2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
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
  void readReturnsMethodNotSupportedWhenProviderLacksReadAnnotation() {
    FhirR4ReadController controller =
        new FhirR4ReadController(
            FhirContext.forR4Cached(), List.of(new SearchOnlyPatientProvider()));

    ResponseEntity<String> response = controller.read("Patient", "patient-uuid");

    assertEquals(405, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("read is not supported"));
  }

  @Test
  void searchReturnsMethodNotSupportedWhenProviderLacksSearchAnnotation() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new ReadOnlyPatientProvider()));

    ResponseEntity<String> response =
        controller.search("Patient", searchParameters("_count", "10"));

    assertEquals(405, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("search is not supported"));
  }

  @Test
  void readsNullResourceAsNotFound() {
    FhirR4ReadController controller =
        new FhirR4ReadController(
            FhirContext.forR4Cached(), List.of(new NullReturningPatientProvider()));

    ResponseEntity<String> response = controller.read("Patient", "patient-uuid");

    assertEquals(404, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("resource was not found: Patient/patient-uuid"));
  }

  @Test
  void propagatesRuntimeReadExceptions() {
    FhirR4ReadController controller =
        new FhirR4ReadController(
            FhirContext.forR4Cached(), List.of(new ExplodingPatientProvider()));

    assertThrows(IllegalStateException.class, () -> controller.read("Patient", "patient-uuid"));
  }

  @Test
  void rejectsNegativeSearchCount() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new PatientProvider()));

    ResponseEntity<String> response =
        controller.search("Patient", searchParameters("_count", "-1"));

    assertEquals(400, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("_count must be zero or greater"));
  }

  @Test
  void rejectsMalformedSearchCount() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new PatientProvider()));

    ResponseEntity<String> response =
        controller.search("Patient", searchParameters("_count", "abc"));

    assertEquals(400, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("_count must be a whole number"));
  }

  @Test
  void rejectsDuplicateSearchCountParameter() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new PatientProvider()));
    LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("_count", "10");
    parameters.add("_count", "20");

    ResponseEntity<String> response = controller.search("Patient", parameters);

    assertEquals(400, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("_count must be specified only once"));
  }

  @Test
  void rejectsBlankSortParameterName() {
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(new ObservationProvider()));
    LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("_sort", ",date");

    ResponseEntity<String> response = controller.search("Observation", parameters);

    assertEquals(400, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));
    assertTrue(response.getBody().contains("_sort must not contain blank values"));
  }

  @Test
  void propagatesRuntimeSearchExceptions() {
    FhirR4ReadController controller =
        new FhirR4ReadController(
            FhirContext.forR4Cached(), List.of(new ExplodingSearchPatientProvider()));

    assertThrows(
        IllegalStateException.class,
        () -> controller.search("Patient", searchParameters("_count", "10")));
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
  void passesAnnotatedSearchParametersToProvider() {
    ObservationProvider provider = new ObservationProvider();
    FhirR4ReadController controller =
        new FhirR4ReadController(FhirContext.forR4Cached(), List.of(provider));
    LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("subject:Patient", "patient-uuid");
    parameters.add("code", "LOINC|1234-5");
    parameters.add("_sort", "-date");
    parameters.add("_summary", "data");
    parameters.add("_count", "100");

    ResponseEntity<String> response = controller.search("Observation", parameters);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(provider.patientReference);
    assertTrue(provider.patientReference.toString().contains("patient-uuid"));
    assertNotNull(provider.code);
    assertTrue(provider.code.toString().contains("LOINC"));
    assertTrue(provider.code.toString().contains("1234-5"));
    assertNotNull(provider.sort);
    assertEquals("date", provider.sort.getParamName());
    assertEquals(SortOrderEnum.DESC, provider.sort.getOrder());
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

  private static final class SearchOnlyPatientProvider implements IResourceProvider {

    @Override
    public Class<? extends IBaseResource> getResourceType() {
      return Patient.class;
    }

    @Search
    @SuppressWarnings("unused")
    public IBundleProvider search() {
      Patient patient = new Patient();
      patient.setId("patient-search-uuid");
      return new SimpleBundleProvider(List.of(patient));
    }
  }

  private static final class ReadOnlyPatientProvider implements IResourceProvider {

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

  private static final class NullReturningPatientProvider extends PatientProvider {

    @Override
    @Read
    public Patient read(@IdParam IdType id) {
      return null;
    }
  }

  private static final class ExplodingPatientProvider extends PatientProvider {

    @Override
    @Read
    public Patient read(@IdParam IdType id) {
      throw new IllegalStateException("boom");
    }
  }

  private static final class ExplodingSearchPatientProvider extends PatientProvider {

    @Override
    @Search
    public IBundleProvider search() {
      throw new IllegalStateException("boom");
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

  private static final class ObservationProvider implements IResourceProvider {

    private ReferenceAndListParam patientReference;

    private TokenAndListParam code;

    private SortSpec sort;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
      return Observation.class;
    }

    @Search
    @SuppressWarnings("unused")
    public IBundleProvider search(
        @OptionalParam(name = Observation.SP_SUBJECT) ReferenceAndListParam patientReference,
        @OptionalParam(name = Observation.SP_CODE) TokenAndListParam code,
        @Sort SortSpec sort) {
      this.patientReference = patientReference;
      this.code = code;
      this.sort = sort;

      Observation observation = new Observation();
      observation.setId("observation-search-uuid");
      return new SimpleBundleProvider(List.of(observation));
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
