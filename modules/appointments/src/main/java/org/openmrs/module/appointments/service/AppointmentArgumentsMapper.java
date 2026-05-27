package org.openmrs.module.appointments.service;

import java.util.List;
import java.util.Map;
import org.openmrs.module.appointments.model.Appointment;

public interface AppointmentArgumentsMapper {
  Map<String, String> createArgumentsMapForRecurringAppointmentBooking(Appointment appointment);

  List<String> getProvidersNameInString(Appointment appointment);

  Map<String, String> createArgumentsMapForAppointmentBooking(Appointment appointment);
}
