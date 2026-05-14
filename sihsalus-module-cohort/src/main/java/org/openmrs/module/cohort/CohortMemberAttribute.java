/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cohort;

import lombok.Getter;
import lombok.Setter;
import org.openmrs.attribute.Attribute;
import org.openmrs.attribute.BaseAttribute;

public class CohortMemberAttribute extends BaseAttribute<CohortMemberAttributeType, CohortMember> implements Attribute<CohortMemberAttributeType, CohortMember> {
	
	private static final long serialVersionUID = 1L;
	
	@Getter
	@Setter
	private Integer id;
	
	public CohortMember getCohortMember() {
		return this.getOwner();
	}
	
	public void setCohortMember(CohortMember cohortMember) {
		this.setOwner(cohortMember);
	}
}
