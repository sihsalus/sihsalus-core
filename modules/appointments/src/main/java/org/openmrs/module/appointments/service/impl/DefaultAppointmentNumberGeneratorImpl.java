package org.openmrs.module.appointments.service.impl;

import jakarta.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.service.AppointmentNumberGenerator;

public class DefaultAppointmentNumberGeneratorImpl implements AppointmentNumberGenerator {
  @Override
  public String generateAppointmentNumber(@NotNull Appointment appointment) {
    return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
  }
}
