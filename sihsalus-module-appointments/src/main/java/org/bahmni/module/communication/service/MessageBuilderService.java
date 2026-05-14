package org.bahmni.module.communication.service;

import java.util.List;
import java.util.Map;
import org.openmrs.api.OpenmrsService;

public interface MessageBuilderService extends OpenmrsService {

    String getAppointmentBookingMessage(Map<String, String> arguments, List<String> providersName);

    String getRecurringAppointmentBookingMessage(Map<String, String> arguments, List<String> providersName);

    String getAppointmentReminderMessage(Map<String, String> arguments, List<String> providersName);
}
