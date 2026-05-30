package org.sihsalus.core.boot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.DrugOrder;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.metadatamapping.MetadataTermMapping;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.openmrs.module.metadatamapping.api.MetadataTermMappingSearchCriteriaBuilder;
import org.sihsalus.core.api.authorization.PatientObjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PatientChartCompatibilityController {

  private final PatientObjectAuthorizationService patientAuthorization;

  PatientChartCompatibilityController(PatientObjectAuthorizationService patientAuthorization) {
    this.patientAuthorization = patientAuthorization;
  }

  @GetMapping("/rest/v1/programenrollment")
  Map<String, Object> programEnrollments(@RequestParam("patient") String patientUuid) {
    Patient patient = requiredPatient(patientUuid);
    List<Map<String, Object>> results =
        Context.getProgramWorkflowService()
            .getPatientPrograms(patient, null, null, null, null, null, false)
            .stream()
            .map(this::patientProgram)
            .toList();

    return results(results);
  }

  @GetMapping("/rest/v1/obs")
  Map<String, Object> observations(
      @RequestParam("patient") String patientUuid,
      @RequestParam(value = "concept", required = false) String conceptIdentifier) {
    Patient patient = requiredPatient(patientUuid);
    Concept concept = findConcept(conceptIdentifier);
    if (conceptIdentifier != null && concept == null) {
      return results(List.of());
    }

    List<Obs> observations =
        concept == null
            ? Context.getObsService().getObservationsByPerson(patient)
            : Context.getObsService().getObservationsByPersonAndConcept(patient, concept);

    return results(observations.stream().map(this::observation).toList());
  }

  @GetMapping("/rest/v1/order")
  Map<String, Object> orders(
      @RequestParam("patient") String patientUuid,
      @RequestParam(value = "careSetting", required = false) String careSettingUuid,
      @RequestParam(value = "orderTypes", required = false) String orderTypeUuids) {
    Patient patient = requiredPatient(patientUuid);
    CareSetting careSetting = findCareSetting(careSettingUuid);
    List<OrderType> orderTypes = findOrderTypes(orderTypeUuids);
    List<Order> orders =
        Context.getOrderService().getAllOrdersByPatient(patient).stream()
            .filter(order -> !Boolean.TRUE.equals(order.getVoided()))
            .filter(order -> careSetting == null || careSetting.equals(order.getCareSetting()))
            .filter(order -> orderTypes.isEmpty() || orderTypes.contains(order.getOrderType()))
            .toList();

    return results(orders.stream().map(this::order).toList());
  }

  @GetMapping("/rest/v1/metadatamapping/termmapping")
  Map<String, Object> metadataTermMappings(@RequestParam("code") String code) {
    MetadataMappingService service = Context.getService(MetadataMappingService.class);
    List<Map<String, Object>> results =
        service
            .getMetadataTermMappings(
                new MetadataTermMappingSearchCriteriaBuilder()
                    .setMetadataTermCode(code)
                    .setIncludeAll(false)
                    .build())
            .stream()
            .map(this::metadataTermMapping)
            .toList();

    return results(results);
  }

  @GetMapping("/rest/v1/concept/{uuid}")
  Map<String, Object> concept(@PathVariable("uuid") String uuid) {
    Concept concept = Context.getConceptService().getConceptByUuid(uuid);
    if (concept == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept not found: " + uuid);
    }
    return concept(concept);
  }

  private Patient requiredPatient(String patientUuid) {
    if (!patientAuthorization.canReadPatient(patientUuid)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Patient access denied: " + patientUuid);
    }
    Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
    if (patient == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found: " + patientUuid);
    }
    return patient;
  }

  private Concept findConcept(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return null;
    }

    ConceptService conceptService = Context.getConceptService();
    Concept concept = conceptService.getConceptByUuid(identifier);
    if (concept != null) {
      return concept;
    }

    try {
      return conceptService.getConcept(Integer.valueOf(identifier));
    } catch (NumberFormatException ignored) {
      return conceptService.getConcept(identifier);
    }
  }

  private CareSetting findCareSetting(String uuid) {
    if (uuid == null || uuid.isBlank()) {
      return null;
    }
    return Context.getOrderService().getCareSettingByUuid(uuid);
  }

  private List<OrderType> findOrderTypes(String commaSeparatedUuids) {
    if (commaSeparatedUuids == null || commaSeparatedUuids.isBlank()) {
      return List.of();
    }

    List<OrderType> orderTypes = new ArrayList<>();
    for (String uuid : commaSeparatedUuids.split(",")) {
      OrderType orderType = Context.getOrderService().getOrderTypeByUuid(uuid.trim());
      if (orderType != null) {
        orderTypes.add(orderType);
      }
    }
    return orderTypes;
  }

  private Map<String, Object> patientProgram(PatientProgram patientProgram) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", patientProgram.getUuid());
    response.put("display", display(patientProgram));
    response.put("program", reference(patientProgram.getProgram()));
    response.put("dateEnrolled", patientProgram.getDateEnrolled());
    response.put("dateCompleted", patientProgram.getDateCompleted());
    response.put("location", reference(patientProgram.getLocation()));
    return response;
  }

  private Map<String, Object> observation(Obs obs) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", obs.getUuid());
    response.put("display", display(obs));
    response.put("concept", reference(obs.getConcept()));
    response.put("person", reference(obs.getPerson()));
    response.put("obsDatetime", obs.getObsDatetime());
    response.put("value", value(obs));
    response.put("valueNumeric", obs.getValueNumeric());
    response.put("valueText", obs.getValueText());
    response.put("valueCoded", reference(obs.getValueCoded()));
    response.put("valueDatetime", obs.getValueDatetime());
    response.put("comment", obs.getComment());
    Set<Obs> groupMembers = obs.getGroupMembers();
    response.put(
        "groupMembers",
        groupMembers == null ? List.of() : groupMembers.stream().map(this::observation).toList());
    return response;
  }

  private Map<String, Object> order(Order order) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", order.getUuid());
    response.put("display", display(order));
    response.put("concept", reference(order.getConcept()));
    response.put("careSetting", reference(order.getCareSetting()));
    response.put("orderType", reference(order.getOrderType()));
    response.put("action", order.getAction());
    response.put("orderNumber", order.getOrderNumber());
    response.put("dateActivated", order.getDateActivated());
    response.put("autoExpireDate", order.getAutoExpireDate());
    response.put("dateStopped", order.getDateStopped());
    response.put("previousOrder", reference(order.getPreviousOrder()));
    response.put("orderer", reference(order.getOrderer()));

    if (order instanceof DrugOrder drugOrder) {
      response.put("dosingType", dosingType(drugOrder));
      response.put("duration", drugOrder.getDuration());
      response.put("durationUnits", reference(drugOrder.getDurationUnits()));
      response.put("route", reference(drugOrder.getRoute()));
      response.put("dose", drugOrder.getDose());
      response.put("doseUnits", reference(drugOrder.getDoseUnits()));
      response.put("frequency", reference(drugOrder.getFrequency()));
      response.put("drug", reference(drugOrder.getDrug()));
    }
    return response;
  }

  private Map<String, Object> metadataTermMapping(MetadataTermMapping mapping) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", mapping.getUuid());
    response.put("display", mapping.getCode());
    response.put("metadataUuid", mapping.getMetadataUuid());
    response.put("metadataClass", mapping.getMetadataClass());
    response.put("code", mapping.getCode());
    response.put("source", reference(mapping.getMetadataSource()));
    return response;
  }

  private Map<String, Object> concept(Concept concept) {
    Map<String, Object> response = conceptMember(concept);
    response.put("conceptClass", reference(concept.getConceptClass()));
    response.put("datatype", reference(concept.getDatatype()));
    response.put("set", Boolean.TRUE.equals(concept.getSet()));
    response.put(
        "setMembers", concept.getSetMembers(false).stream().map(this::conceptMember).toList());
    return response;
  }

  private Map<String, Object> conceptMember(Concept concept) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", concept.getUuid());
    response.put("display", display(concept));

    ConceptNumeric numeric = numericConcept(concept);
    response.put("hiNormal", numeric == null ? null : numeric.getHiNormal());
    response.put("hiAbsolute", numeric == null ? null : numeric.getHiAbsolute());
    response.put("hiCritical", numeric == null ? null : numeric.getHiCritical());
    response.put("lowNormal", numeric == null ? null : numeric.getLowNormal());
    response.put("lowAbsolute", numeric == null ? null : numeric.getLowAbsolute());
    response.put("lowCritical", numeric == null ? null : numeric.getLowCritical());
    response.put("units", numeric == null ? null : numeric.getUnits());
    return response;
  }

  private ConceptNumeric numericConcept(Concept concept) {
    if (concept instanceof ConceptNumeric numeric) {
      return numeric;
    }
    try {
      return Context.getConceptService().getConceptNumeric(concept.getConceptId());
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private Map<String, Object> reference(OpenmrsObject object) {
    if (object == null) {
      return null;
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", object.getUuid());
    response.put("display", display(object));
    return response;
  }

  private String display(OpenmrsObject object) {
    if (object == null) {
      return null;
    }
    if (object instanceof Concept concept) {
      return concept.getDisplayString();
    }
    if (object instanceof Location location) {
      return location.getDisplayString();
    }
    if (object instanceof Program program && program.getConcept() != null) {
      return program.getConcept().getDisplayString();
    }
    if (object instanceof OpenmrsMetadata metadata) {
      return metadata.getName();
    }
    return object.toString();
  }

  private String display(Obs obs) {
    String concept = display(obs.getConcept());
    String value = value(obs);
    if (value == null || value.isBlank()) {
      return concept;
    }
    return concept + ": " + value;
  }

  private String display(PatientProgram patientProgram) {
    return display(patientProgram.getProgram());
  }

  private String display(Order order) {
    return display(order.getConcept());
  }

  private String value(Obs obs) {
    return obs.getValueAsString(Locale.ENGLISH);
  }

  private String dosingType(DrugOrder drugOrder) {
    Class<?> dosingType = drugOrder.getDosingType();
    return dosingType == null ? null : dosingType.getName();
  }

  private Map<String, Object> results(List<Map<String, Object>> results) {
    return Map.of("results", results);
  }
}
