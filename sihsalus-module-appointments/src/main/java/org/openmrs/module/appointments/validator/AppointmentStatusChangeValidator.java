package org.openmrs.module.appointments.validator;

import java.util.List;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;

public interface AppointmentStatusChangeValidator {
  void validate(Appointment appointment, AppointmentStatus status, List<String> errors);
}
