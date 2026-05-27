package org.openmrs.module.appointments.service;

import static org.openmrs.module.appointments.constants.PrivilegeConstants.MANAGE_APPOINTMENTS;
import static org.openmrs.module.appointments.constants.PrivilegeConstants.MANAGE_OWN_APPOINTMENTS;

import java.util.List;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.springframework.transaction.annotation.Transactional;

public interface AppointmentRecurringPatternService {

  @Transactional
  @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
  AppointmentRecurringPattern validateAndSave(
      AppointmentRecurringPattern appointmentRecurringPattern);

  @Transactional
  @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
  Appointment update(
      AppointmentRecurringPattern appointmentRecurringPattern,
      List<Appointment> updatedAppointments);

  @Transactional
  @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
  List<Appointment> changeStatus(Appointment appointment, String toStatus, String clientTimeZone);

  @Transactional
  @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
  AppointmentRecurringPattern update(
      AppointmentRecurringPattern appointmentRecurringPattern, Appointment editedAppointment);
}
