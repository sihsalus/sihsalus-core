package org.openmrs.module.appointments.dao;

import java.util.List;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentAudit;

public interface AppointmentAuditDao {

  void save(AppointmentAudit appointmentAuditEvent);

  List<AppointmentAudit> getAppointmentHistoryForAppointment(Appointment appointment);

  AppointmentAudit getPriorStatusChangeEvent(Appointment appointment);
}
