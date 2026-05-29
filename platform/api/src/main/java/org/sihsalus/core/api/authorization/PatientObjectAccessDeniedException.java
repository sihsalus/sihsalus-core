package org.sihsalus.core.api.authorization;

import org.openmrs.api.APIAuthenticationException;

public class PatientObjectAccessDeniedException extends APIAuthenticationException {

  public PatientObjectAccessDeniedException(String patientUuid) {
    super("Patient access denied: " + patientUuid);
  }
}
