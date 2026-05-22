package org.openmrs.module.appointments.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.AdhocTeleconsultationResponse;
import org.openmrs.module.appointments.notification.NotificationResult;
import org.bahmni.module.teleconsultation.api.TeleconsultationService;

import java.util.List;
import java.security.SecureRandom;

public class TeleconsultationAppointmentService {

    private final static String ADHOC_TC_ID = "bahmni.adhoc.teleConsultation.id";
    private final static String CREATE_TELECONSULTATION_PRIVILEGE = "Create Teleconsultation";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private Log log = LogFactory.getLog(this.getClass());

    private PatientService patientService;
    private PatientAppointmentNotifierService patientAppointmentNotifierService;

    public void setPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    public void setPatientAppointmentNotifierService(PatientAppointmentNotifierService patientAppointmentNotifierService) {
        this.patientAppointmentNotifierService = patientAppointmentNotifierService;
    }

    public String generateTeleconsultationLink(String uuid) {
        return Context.getService(TeleconsultationService.class).generateTeleconsultationLink(uuid);
    }

    public AdhocTeleconsultationResponse generateAdhocTeleconsultationLink(String patientUuid, String provider) {
        Context.requirePrivilege(CREATE_TELECONSULTATION_PRIVILEGE);
        if (StringUtils.isBlank(patientUuid)) {
            throw new IllegalArgumentException("patientUuid is required");
        }
        if (StringUtils.isBlank(provider)) {
            throw new IllegalArgumentException("provider is required");
        }
        String identifierType = Context.getAdministrationService().getGlobalProperty(ADHOC_TC_ID);
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient == null) {
            throw new IllegalArgumentException("Patient does not exist");
        }
        PatientIdentifier identifier = null;
        if (StringUtils.isNotBlank(identifierType)) {
            identifier = patient.getIdentifiers().stream().filter(pi ->
                            identifierType.equals(pi.getIdentifierType().getName()))
                    .findAny()
                    .orElse(null);
        }
        String teleConsultationId = (identifier != null) ? identifier.getIdentifier() : generateRandomID();
        String link = generateTeleconsultationLink(teleConsultationId);
        AdhocTeleconsultationResponse response = new AdhocTeleconsultationResponse();
        response.setUuid(teleConsultationId);
        response.setLink(link);
        notifyUpdates(response, patient, provider, link);
        return response;
    }

    private String generateRandomID() {
        return Long.toHexString(SECURE_RANDOM.nextLong()) + Long.toHexString(System.currentTimeMillis());
    }

    private void notifyUpdates(AdhocTeleconsultationResponse response, Patient patient, String provider, String link) {
        List<NotificationResult> notificationResults = patientAppointmentNotifierService.notifyAll(patient, provider, link);
        if (!notificationResults.isEmpty()) {
            notificationResults.stream().forEach(nr -> {
                String notificationMsg = String.format("Appointment Notification Result - medium: %s, uuid: %s, status: %d, message: %s",
                        nr.getMedium(), nr.getUuid(), nr.getStatus(), nr.getMessage());
                log.info(notificationMsg);
            });
            response.setNotificationResults(notificationResults);
        }
    }
}
