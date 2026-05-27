package org.openmrs.module.openconceptlab.web.rest;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.util.PrivilegeConstants;

public final class OpenConceptLabRestPrivileges {

  private OpenConceptLabRestPrivileges() {}

  public static void requireManageConcepts() {
    try {
      Context.requirePrivilege(PrivilegeConstants.MANAGE_CONCEPTS);
    } catch (ContextAuthenticationException e) {
      throw new APIAuthenticationException(e.getMessage(), e);
    }
  }
}
