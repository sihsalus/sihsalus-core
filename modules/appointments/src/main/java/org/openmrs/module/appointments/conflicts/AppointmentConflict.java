package org.openmrs.module.appointments.conflicts;

import java.util.List;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentConflictType;

public interface AppointmentConflict {

  AppointmentConflictType getType();

  List<Appointment> getConflicts(List<Appointment> appointment);
}
