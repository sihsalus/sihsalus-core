/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cohort.api;

import org.openmrs.module.cohort.CohortAttribute;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.CohortMemberAttribute;
import org.openmrs.module.cohort.CohortMemberAttributeType;
import org.openmrs.module.cohort.CohortType;

public class TestDataUtils {
	
	public static CohortType COHORT_TYPE() {
		CohortType cohortType = new CohortType();
		cohortType.setId(1);
		cohortType.setCohortTypeId(100);
		cohortType.setName("TestCohortType");
		cohortType.setDescription("Test cohort description");
		return cohortType;
	}
	
	public static CohortAttributeType COHORT_ATTRIBUTE_TYPE() {
		CohortAttributeType cohortAttributeType = new CohortAttributeType();
		cohortAttributeType.setUuid("32816782-d578-401c-8475-8ccbb26ce001");
		cohortAttributeType.setName("cohortAttributeType");
		cohortAttributeType.setDescription("test cohort attribute type");
		cohortAttributeType.setDatatypeClassname("java.lang.String");
		cohortAttributeType.setCohortAttributeTypeId(400);
		return cohortAttributeType;
	}
	
	public static CohortAttribute COHORT_ATTRIBUTE() {
		CohortAttribute cohortAttribute = new CohortAttribute();
		cohortAttribute.setId(1);
		cohortAttribute.setUuid("");
		cohortAttribute.setCohortAttributeId(200);
		cohortAttribute.setValue("cohortAttribute");
		cohortAttribute.setAttributeType(COHORT_ATTRIBUTE_TYPE());
		return cohortAttribute;
	}
	
	public static CohortMemberAttributeType COHORT_MEMBER_ATTRIBUTE_TYPE() {
		CohortMemberAttributeType cohortMemberAttributeType = new CohortMemberAttributeType();
		cohortMemberAttributeType.setId(1);
		cohortMemberAttributeType.setId(103);
		cohortMemberAttributeType.setName("cohort member attributeType Name");
		cohortMemberAttributeType.setDescription("test cohort member attribute type");
		cohortMemberAttributeType.setDatatypeClassname("java.lang.String");
		return cohortMemberAttributeType;
	}
	
	public static CohortMemberAttribute COHORT_MEMBER_ATTRIBUTE() {
		CohortMemberAttribute cohortMemberAttribute = new CohortMemberAttribute();
		cohortMemberAttribute.setId(1);
		cohortMemberAttribute.setUuid("32816782-d578-401c-8475-8ccbb26ce001");
		cohortMemberAttribute.setAttributeType(COHORT_MEMBER_ATTRIBUTE_TYPE());
		cohortMemberAttribute.setValue("cohortMemberAttribute");
		cohortMemberAttribute.setId(100);
		return cohortMemberAttribute;
	}
}
