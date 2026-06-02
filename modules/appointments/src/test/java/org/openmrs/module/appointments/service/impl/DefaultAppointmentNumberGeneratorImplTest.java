package org.openmrs.module.appointments.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.openmrs.module.appointments.model.Appointment;

class DefaultAppointmentNumberGeneratorImplTest {

  @Test
  void generateAppointmentNumberStartsWithCurrentTimestampPrefix() {
    DefaultAppointmentNumberGeneratorImpl appointmentNumberGenerator =
        new DefaultAppointmentNumberGeneratorImpl();
    String expectedPrefix = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());

    String generated = appointmentNumberGenerator.generateAppointmentNumber(new Appointment());

    assertTrue(generated.startsWith(expectedPrefix));
  }
}
