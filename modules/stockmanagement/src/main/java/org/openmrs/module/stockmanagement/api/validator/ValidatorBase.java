package org.openmrs.module.stockmanagement.api.validator;

import java.util.Date;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public abstract class ValidatorBase implements Validator {

  /**
   * Rejects a date if it is in the future.
   *
   * @param errors the error object
   * @param date the date to check
   * @param dateField the name of the field
   */
  private void rejectIfFutureDate(Errors errors, Date date, String dateField) {
    if (OpenmrsUtil.compare(date, new Date()) > 0) {
      errors.rejectValue(dateField, "error.date.future");
    }
  }
}
