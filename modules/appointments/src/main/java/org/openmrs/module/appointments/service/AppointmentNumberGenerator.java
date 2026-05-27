package org.openmrs.module.appointments.service;

import jakarta.validation.constraints.NotNull;
import org.openmrs.module.appointments.model.Appointment;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface AppointmentNumberGenerator {
  String generateAppointmentNumber(@NotNull Appointment appointment);
}
