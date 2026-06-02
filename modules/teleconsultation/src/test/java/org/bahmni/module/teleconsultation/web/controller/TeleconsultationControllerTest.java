package org.bahmni.module.teleconsultation.web.controller;

import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TeleconsultationControllerTest {
	
	@Mock
	private TeleconsultationService teleconsultationService;
	
	@InjectMocks
	private TeleconsultationController teleconsultationController;
	
	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}
	
	@Test
	public void shouldGenerateTeleconsultationLink() throws Exception {
		String patientUuid = "patientUuid";
		ResponseEntity<String> adhocTeleconsultationResponse = teleconsultationController
		        .generateTeleconsultationLink(patientUuid);
		verify(teleconsultationService, times(1)).generateTeleconsultationLink(eq(patientUuid));
	}
}
