package org.sihsalus.core.api.authorization;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;

public interface PatientObjectAuthorizationService {

  String BEAN_NAME = "sihsalusPatientObjectAuthorizationService";

  PatientObjectAuthorizationService PERMIT_ALL = patientUuid -> true;

  boolean canReadPatient(String patientUuid);

  default void requireCanReadPatient(String patientUuid) {
    if (!canReadPatient(patientUuid)) {
      throw new PatientObjectAccessDeniedException(patientUuid);
    }
  }

  static PatientObjectAuthorizationService permitAll() {
    return PERMIT_ALL;
  }

  static PatientObjectAuthorizationService current() {
    try {
      return Context.getRegisteredComponent(BEAN_NAME, PatientObjectAuthorizationService.class);
    } catch (APIException | IllegalStateException exception) {
      return permitAll();
    }
  }
}
