package org.bahmni.module.communication.service;

import org.openmrs.api.OpenmrsService;

public interface CommunicationService extends OpenmrsService {

    void sendSMS(String phoneNumber, String message);
}
