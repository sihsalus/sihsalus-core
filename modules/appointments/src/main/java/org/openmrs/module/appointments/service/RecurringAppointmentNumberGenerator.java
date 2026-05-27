package org.openmrs.module.appointments.service;

import java.util.List;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface RecurringAppointmentNumberGenerator {
  void setAppointmentNumbers(List<Appointment> appointments, AppointmentRecurringPattern pattern);
}
