/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.entity.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;

/**
 * Wrapper class retained for the legacy service code. The billing module now uses a local
 * compatibility Criteria wrapper that exposes projection and result transformer state directly.
 */
public class CriteriaImplWrapper {
	
	public static final long serialVersionUID = 0L;
	
	private final Criteria criteria;
	
	public CriteriaImplWrapper(Criteria criteria) {
		this.criteria = criteria;
	}
	
	public Projection getProjection() {
		return criteria.getProjection();
	}
	
	public ResultTransformer getResultTransformer() {
		return criteria.getResultTransformer();
	}
}
