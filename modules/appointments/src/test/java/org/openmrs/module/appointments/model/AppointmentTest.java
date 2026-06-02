package org.openmrs.module.appointments.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AppointmentTest {

  @Test
  void getProvidersWithResponseReturnsProvidersWhoAccepted() {
    Appointment appointment = appointmentWithProviderResponses();

    Set<AppointmentProvider> providersWithResponse =
        appointment.getProvidersWithResponse(AppointmentProviderResponse.ACCEPTED);

    assertEquals(2, providersWithResponse.size());
    assertTrue(
        providersWithResponse.stream()
            .anyMatch(provider -> "1DummyUuidJustForTest".equals(provider.getUuid())));
    assertTrue(
        providersWithResponse.stream()
            .anyMatch(provider -> "2DummyUuidJustForTest".equals(provider.getUuid())));
  }

  @Test
  void getProvidersWithResponseReturnsProvidersWhoRejected() {
    Appointment appointment = appointmentWithProviderResponses();

    Set<AppointmentProvider> providersWithResponse =
        appointment.getProvidersWithResponse(AppointmentProviderResponse.REJECTED);

    assertEquals(1, providersWithResponse.size());
    assertTrue(
        providersWithResponse.stream()
            .anyMatch(provider -> "3DummyUuidJustForTest".equals(provider.getUuid())));
  }

  @Test
  void getProvidersWithResponseReturnsEmptySetWhenProvidersAreMissing() {
    Appointment appointment = new Appointment();

    Set<AppointmentProvider> providersWithResponse =
        appointment.getProvidersWithResponse(AppointmentProviderResponse.ACCEPTED);

    assertTrue(providersWithResponse.isEmpty());
  }

  private Appointment appointmentWithProviderResponses() {
    Appointment appointment = new Appointment();
    Set<AppointmentProvider> providers = new HashSet<>();
    providers.add(
        appointmentProvider("1DummyUuidJustForTest", AppointmentProviderResponse.ACCEPTED));
    providers.add(
        appointmentProvider("2DummyUuidJustForTest", AppointmentProviderResponse.ACCEPTED));
    providers.add(
        appointmentProvider("3DummyUuidJustForTest", AppointmentProviderResponse.REJECTED));
    appointment.setProviders(providers);
    return appointment;
  }

  private AppointmentProvider appointmentProvider(
      String uuid, AppointmentProviderResponse response) {
    AppointmentProvider provider = new AppointmentProvider();
    provider.setUuid(uuid);
    provider.setResponse(response);
    return provider;
  }
}
