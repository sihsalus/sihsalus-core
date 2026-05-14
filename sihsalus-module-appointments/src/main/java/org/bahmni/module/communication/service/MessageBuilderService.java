package org.bahmni.module.communication.service;

import java.util.List;
import java.util.Map;
import org.openmrs.api.OpenmrsService;

public interface MessageBuilderService extends OpenmrsService {

    String getAppointmentBookingMessage(Map<String, Object> arguments, List<String> providersName);

    String getRecurringAppointmentBookingMessage(Map<String, Object> arguments, List<String> providersName);

    String getAppointmentReminderMessage(Map<String, Object> arguments, List<String> providersName);
}
