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

import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.validator.BaseAttributeTypeValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@Handler(supports = { CohortAttributeType.class }, order = 50)
@Qualifier("cohort.cohortAttributeTypeValidator")
public class CohortAttributeTypeValidator extends BaseAttributeTypeValidator<CohortAttributeType> implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return clazz.equals(CohortAttributeType.class);
	}
	
	@Override
	public void validate(Object command, Errors errors) {
		super.validate(command, errors);
		
		CohortAttributeType cohortAttributeType = (CohortAttributeType) command;
		CohortAttributeType attributeType = Context.getService(CohortService.class)
		        .getCohortAttributeTypeByName(cohortAttributeType.getName());
		
		if (attributeType != null) {
			errors.rejectValue("name", " A cohort attribute type with the same name already exists");
		}
	}
}
