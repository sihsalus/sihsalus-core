package org.openmrs.module.appointments.validator.impl;

import java.util.List;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.validator.AppointmentValidator;

public class DefaultAppointmentValidator implements AppointmentValidator {

  @Override
  public void validate(Appointment appointment, List<String> errors) {
    if (appointment.getPatient() == null)
      errors.add("Appointment cannot be created without Patient");
    if (appointment.getService() == null)
      errors.add("Appointment cannot be created without Service");
  }
}
