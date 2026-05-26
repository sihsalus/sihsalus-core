package org.openmrs.module.appointments.validator;

import java.util.List;
import org.openmrs.module.appointments.model.Appointment;

public interface AppointmentValidator {

  void validate(Appointment appointment, List<String> errors);
}
