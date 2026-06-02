package org.openmrs.module.appointments.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AppointmentRecurringPatternTest {

  @Test
  void getActiveAppointmentsReturnsAllAppointmentsWhenThereIsNoVoidedAppointment() {
    AppointmentRecurringPattern pattern = new AppointmentRecurringPattern();
    Appointment appointment1 = appointment(false);
    Appointment appointment2 = appointment(false);
    Appointment appointment3 = appointment(false);
    Set<Appointment> appointments = appointments(appointment1, appointment2, appointment3);
    pattern.setAppointments(appointments);

    Set<Appointment> activeAppointments = pattern.getActiveAppointments();

    assertEquals(3, activeAppointments.size());
    assertEquals(appointments, activeAppointments);
  }

  @Test
  void getActiveAppointmentsReturnsOnlyNonVoidedAppointments() {
    AppointmentRecurringPattern pattern = new AppointmentRecurringPattern();
    Appointment voidedAppointment = appointment(true);
    Appointment activeAppointment1 = appointment(false);
    Appointment activeAppointment2 = appointment(false);
    Set<Appointment> appointments =
        appointments(voidedAppointment, activeAppointment1, activeAppointment2);
    pattern.setAppointments(appointments);

    Set<Appointment> activeAppointments = pattern.getActiveAppointments();

    assertEquals(3, appointments.size());
    assertEquals(2, activeAppointments.size());
    assertTrue(activeAppointments.contains(activeAppointment1));
    assertTrue(activeAppointments.contains(activeAppointment2));
    assertFalse(activeAppointments.contains(voidedAppointment));
  }

  @Test
  void getActiveAppointmentsReturnsEmptySetWhenAllAppointmentsAreVoided() {
    AppointmentRecurringPattern pattern = new AppointmentRecurringPattern();
    Appointment appointment1 = appointment(true);
    Appointment appointment2 = appointment(true);
    Appointment appointment3 = appointment(true);
    pattern.setAppointments(appointments(appointment1, appointment2, appointment3));

    Set<Appointment> activeAppointments = pattern.getActiveAppointments();

    assertTrue(activeAppointments.isEmpty());
  }

  private Appointment appointment(boolean voided) {
    Appointment appointment = new Appointment();
    appointment.setVoided(voided);
    return appointment;
  }

  private Set<Appointment> appointments(Appointment... appointments) {
    return new LinkedHashSet<>(Arrays.asList(appointments));
  }
}
