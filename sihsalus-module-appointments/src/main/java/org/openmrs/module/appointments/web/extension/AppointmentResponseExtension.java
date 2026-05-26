package org.openmrs.module.appointments.web.extension;

import java.util.Map;
import org.openmrs.module.appointments.model.Appointment;

public interface AppointmentResponseExtension {
  Map<String, String> run(Appointment appointment);
}
