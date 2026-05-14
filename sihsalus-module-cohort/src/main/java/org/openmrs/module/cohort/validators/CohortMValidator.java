/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cohort.validators;

import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.api.CohortService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
@Handler(supports = { CohortM.class, Cohort.class }, order = 50)
@Qualifier("cohort.cohortMValidator")
public class CohortMValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return clazz.equals(CohortM.class) || clazz.equals(Cohort.class);
	}
	
	@Override
	public void validate(Object command, Errors errors) {
		if (command instanceof Cohort) {
			errors.reject("A standard cohort should not be created while the cohort module is active");
			return;
		}
		
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "Cohort Name Required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "definitionHandlerClassname",
		    "Cohort definitionHandlerClassname is required");
		
		CohortM cohort = (CohortM) command;
		
		// Cohort should have a unique name
		CohortM cohortByName = Context.getService(CohortService.class).getCohortM(cohort.getName());
		if (cohortByName != null && cohortByName.getId() != cohort.getId()) {
			errors.rejectValue("name", "A cohort with this name already exists");
		}
		
		// EndDate should less than startDate
		if (cohort.getEndDate() != null && cohort.getEndDate().before(cohort.getStartDate())) {
			errors.rejectValue("startDate", "Start date should be before the end date");
		}
	}
}
