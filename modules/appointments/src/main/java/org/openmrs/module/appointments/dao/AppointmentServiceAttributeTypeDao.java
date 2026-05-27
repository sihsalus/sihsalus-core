package org.openmrs.module.appointments.dao;

import java.util.List;
import org.openmrs.module.appointments.model.AppointmentServiceAttributeType;

public interface AppointmentServiceAttributeTypeDao {
  List<AppointmentServiceAttributeType> getAllAttributeTypes(boolean includeRetired);

  AppointmentServiceAttributeType getAttributeTypeByUuid(String uuid);

  AppointmentServiceAttributeType getAttributeTypeById(Integer id);

  AppointmentServiceAttributeType save(AppointmentServiceAttributeType attributeType);
}
