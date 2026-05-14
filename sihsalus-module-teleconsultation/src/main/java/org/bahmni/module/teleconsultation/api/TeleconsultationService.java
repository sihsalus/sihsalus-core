package org.bahmni.module.teleconsultation.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;

public interface TeleconsultationService extends OpenmrsService {

	@Authorized(value = {"Create Teleconsultation"})
	String generateTeleconsultationLink(String uuid);
}
