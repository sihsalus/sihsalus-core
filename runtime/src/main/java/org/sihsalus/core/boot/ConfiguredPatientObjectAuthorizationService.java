package org.sihsalus.core.boot;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.openmrs.api.context.Context;
import org.sihsalus.core.api.authorization.PatientObjectAuthorizationService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component(PatientObjectAuthorizationService.BEAN_NAME)
final class ConfiguredPatientObjectAuthorizationService
    implements PatientObjectAuthorizationService {

  static final String DENIED_PATIENT_UUIDS_PROPERTY = "sihsalus.authorization.patient.deniedUuids";

  private final JdbcTemplate jdbcTemplate;

  ConfiguredPatientObjectAuthorizationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean canReadPatient(String patientUuid) {
    if (patientUuid == null || patientUuid.isBlank()) {
      return true;
    }
    if (!Context.isSessionOpen() || !Context.isAuthenticated()) {
      return false;
    }
    Set<String> deniedUuids = deniedPatientUuids();
    return !deniedUuids.contains("*") && !deniedUuids.contains(patientUuid);
  }

  private Set<String> deniedPatientUuids() {
    String value;
    try {
      value =
          jdbcTemplate.query(
              "select property_value from global_property where property = ?",
              resultSet -> resultSet.next() ? resultSet.getString(1) : "",
              DENIED_PATIENT_UUIDS_PROPERTY);
    } catch (DataAccessException exception) {
      return Set.of();
    }
    if (value == null || value.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(value.split("[,\\s]+"))
        .map(String::trim)
        .filter(token -> !token.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }
}
