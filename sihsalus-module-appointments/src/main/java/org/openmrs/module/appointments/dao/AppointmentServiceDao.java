package org.openmrs.module.appointments.dao;

import java.util.List;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceSearchParams;
import org.openmrs.module.appointments.model.AppointmentServiceType;

public interface AppointmentServiceDao {

  List<AppointmentServiceDefinition> getAllAppointmentServices(boolean includeVoided);

  AppointmentServiceDefinition save(AppointmentServiceDefinition appointmentServiceDefinition);

  AppointmentServiceDefinition getAppointmentServiceByUuid(String uuid);

  AppointmentServiceDefinition getNonVoidedAppointmentServiceByName(String serviceName);

  AppointmentServiceType getAppointmentServiceTypeByUuid(String uuid);

  List<AppointmentServiceDefinition> search(AppointmentServiceSearchParams searchParams);
}
