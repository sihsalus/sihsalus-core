package org.openmrs.module.appointments.dao;

import java.util.List;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.springframework.transaction.annotation.Transactional;

public interface AppointmentRecurringPatternDao {
  @Transactional
  void save(AppointmentRecurringPattern appointmentRecurringPattern);

  List<AppointmentRecurringPattern> getAllAppointmentRecurringPatterns();
}
