/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.patientflags.web.validators;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientflags.Flag;
import org.openmrs.module.patientflags.FlagValidationResult;
import org.openmrs.module.patientflags.api.FlagService;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/** Validator for the Flag class */
public class FlagValidator implements Validator {

  @SuppressWarnings("rawtypes")
  public boolean supports(Class clazz) {
    return Flag.class.isAssignableFrom(clazz);
  }

  public void validate(Object target, Errors errors) {

    Flag flagToValidate = (Flag) target;
    String name = flagToValidate.getName();
    String criteria = flagToValidate.getCriteria();
    String flagMessage = flagToValidate.getMessage();
    String evaluator = flagToValidate.getEvaluator();

    // name cannot be empty
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "patientflags.errors.noName");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "criteria", "patientflags.errors.noCriteria");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "message", "patientflags.errors.noMessage");

    // make sure that the string fields aren't too large
    if (name != null && name.length() > 255)
      errors.rejectValue("name", "patientflags.errors.nameTooLong");

    if (StringUtils.isNotBlank(name) && isFlagNameDuplicated(flagToValidate)) {
      errors.rejectValue("name", "patientflags.errors.noUniqueName");
    }

    if (criteria != null && criteria.length() > 5000)
      errors.rejectValue("criteria", "patientflags.errors.criteriaTooLong");

    if (flagMessage != null && flagMessage.length() > 255)
      errors.rejectValue("message", "patientflags.errors.messageTooLong");

    if (StringUtils.isBlank(evaluator)) {
      errors.rejectValue("evaluator", "patientflags.errors.noEvaluator");
    } else if (StringUtils.isNotBlank(criteria)) {
      // run the target Flag's validate method to see if the criteria is well-formed
      FlagValidationResult result;
      try {
        result = flagToValidate.validate();
      } catch (APIException e) {
        errors.rejectValue("evaluator", "patientflags.errors.invalidCriteria");
        return;
      }

      if (!result.getResult()) {
        String message = result.getLocalizedMessage();
        errors.rejectValue(
            "criteria",
            Context.getMessageSourceService().getMessage("patientflags.errors.invalidCriteria")
                + (message != null ? ": " + message : ""));
      }
    }
  }

  private boolean isFlagNameDuplicated(Flag flagToValidate) {
    return Context.getService(FlagService.class).isFlagNameDuplicated(flagToValidate);
  }
}
