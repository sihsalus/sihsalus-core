package org.bahmni.module.teleconsultation.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;


@RunWith(SpringJUnit4ClassRunner.class)
@org.springframework.test.context.ContextConfiguration(locations = {
		"classpath:TestingApplicationContext.xml" }, inheritLocations = true)
@PrepareForTest({Context.class})
public class TeleconsultationServiceTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	TeleconsultationService teleconsultationService;

	@Before
	public void init() {
		executeDataSet("userRolesAndPrivileges.xml");
	}

	@Test
	public void shouldPassCreateTeleconsultationLinkIfTheUserHasCreateTeleconsultationPrivileges() {
		Context.authenticate("userWithPrivilege", "P@ssw0rd");
		UUID uuid = UUID.randomUUID();
		String link = teleconsultationService.generateTeleconsultationLink(uuid.toString());
		assertNotNull("https://meet.jit.si/" + uuid, link);
	}

	@Test(expected = APIAuthenticationException.class)
	public void shouldThrowAuthenticationExceptionIfUserDoesNotHaveSufficientPrivileges() {
		Context.authenticate("userWithoutPrivilege", "P@ssw0rd");
		TeleconsultationService service = Context.getService(TeleconsultationService.class);
		service.generateTeleconsultationLink("uuid");
	}
}
