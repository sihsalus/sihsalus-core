package org.openmrs.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openmrs.api.APIException;
import org.openmrs.api.RefByUuid;
import org.openmrs.hl7.impl.HL7ServiceImpl;

class RefByUuidUnsupportedTypeTest {

	private final List<RefByUuid> services = List.of(new AdministrationServiceImpl(), new CohortServiceImpl(),
	    new ConceptServiceImpl(), new ConditionServiceImpl(), new DatatypeServiceImpl(), new DiagnosisServiceImpl(),
	    new EncounterServiceImpl(), new FormServiceImpl(), new HL7ServiceImpl(), new LocationServiceImpl(),
	    new MedicationDispenseServiceImpl(), new ObsServiceImpl(), new OrderServiceImpl(), new OrderSetServiceImpl(),
	    new PatientServiceImpl(), new PersonServiceImpl(), new ProgramWorkflowServiceImpl(), new ProviderServiceImpl(),
	    new UserServiceImpl(), new VisitServiceImpl());

	@Test
	void getRefByUuidReportsNullUnsupportedType() {
		for (RefByUuid service : services) {
			APIException exception = assertThrows(APIException.class, () -> service.getRefByUuid(null, "uuid"));

			assertEquals("Unsupported type for getRefByUuid: null", exception.getMessage(), service.getClass().getName());
		}
	}

	@Test
	void getRefByUuidReportsUnsupportedTypeName() {
		for (RefByUuid service : services) {
			APIException exception = assertThrows(APIException.class, () -> service.getRefByUuid(String.class, "uuid"));

			assertEquals("Unsupported type for getRefByUuid: java.lang.String", exception.getMessage(),
			    service.getClass().getName());
		}
	}
}
