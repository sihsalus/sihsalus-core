package org.openmrs.module.emrapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EmrApiConceptMappingsTest {

	@Test
	void defaultsKeepLegacyEmrApiConceptMappings() {
		EmrApiConceptMappings mappings = EmrApiConceptMappings.defaults();

		assertEquals(EmrApiConstants.EMR_CONCEPT_SOURCE_NAME, mappings.getConceptSourceName());
		assertEquals(EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_CONCEPT_SET, mappings.getDiagnosisConceptSetCode());
		assertEquals(EmrApiConstants.CONCEPT_CODE_CODED_DIAGNOSIS, mappings.getCodedDiagnosisCode());
		assertEquals(EmrApiConstants.CONCEPT_CODE_DISPOSITION_CONCEPT_SET, mappings.getDispositionConceptSetCode());
		assertEquals(EmrApiConstants.CONCEPT_CODE_DISPOSITION, mappings.getDispositionCode());
		assertEquals(EmrApiConstants.CONCEPT_CODE_PATIENT_DIED, mappings.getPatientDiedCode());
	}

	@Test
	void globalPropertyValuesOverrideLegacyDefaults() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(EmrApiConstants.GP_EMR_CONCEPT_SOURCE_NAME, "sihsalus.concepts");
		properties.put(EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_CONCEPT_SET, "sihsalus-diagnosis-set");
		properties.put(EmrApiConstants.GP_CONCEPT_CODE_DISPOSITION, "sihsalus-disposition");
		properties.put(EmrApiConstants.GP_CONCEPT_CODE_PATIENT_DIED, "sihsalus-patient-died");

		EmrApiConceptMappings mappings = EmrApiConceptMappings
		        .from((propertyName, defaultValue) -> properties.getOrDefault(propertyName, defaultValue));

		assertEquals("sihsalus.concepts", mappings.getConceptSourceName());
		assertEquals("sihsalus-diagnosis-set", mappings.getDiagnosisConceptSetCode());
		assertEquals("sihsalus-disposition", mappings.getDispositionCode());
		assertEquals("sihsalus-patient-died", mappings.getPatientDiedCode());
		assertEquals(EmrApiConstants.CONCEPT_CODE_CODED_DIAGNOSIS, mappings.getCodedDiagnosisCode());
	}

	@Test
	void blankGlobalPropertyValuesFallBackToLegacyDefaults() {
		EmrApiConceptMappings mappings = EmrApiConceptMappings
		        .from((propertyName, defaultValue) -> EmrApiConstants.GP_CONCEPT_CODE_DISPOSITION.equals(propertyName) ? " "
		                : defaultValue);

		assertEquals(EmrApiConstants.CONCEPT_CODE_DISPOSITION, mappings.getDispositionCode());
	}
}
