package org.openmrs.module.appointments.web.contract;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.openmrs.module.appointments.model.AppointmentStatus;

public class AppointmentStatusChangeRequest {

  @NotNull(message = "List of appointment UUIDs is required")
  @Size(min = 1, max = 200, message = "Between 1 and 200 appointment UUIDs must be specified")
  private List<String> appointmentUuids;

  @NotNull(message = "toStatus is required")
  private AppointmentStatus toStatus;

  public List<String> getAppointmentUuids() {
    return appointmentUuids;
  }

  public void setAppointmentUuids(List<String> appointmentUuids) {
    this.appointmentUuids = appointmentUuids;
  }

  public AppointmentStatus getToStatus() {
    return toStatus;
  }

  public void setToStatus(AppointmentStatus toStatus) {
    this.toStatus = toStatus;
  }
}
