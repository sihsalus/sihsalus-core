/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.criteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

public class BillingOrder {

	private final String propertyName;

	private final boolean ascending;

	protected BillingOrder(String propertyName, boolean ascending) {
		this.propertyName = propertyName;
		this.ascending = ascending;
	}

	public static BillingOrder asc(String propertyName) {
		return new BillingOrder(propertyName, true);
	}

	public static BillingOrder desc(String propertyName) {
		return new BillingOrder(propertyName, false);
	}

	public jakarta.persistence.criteria.Order toJpaOrder(CriteriaBuilder builder, Root<?> root) {
		var path = BillingRestrictions.path(root, propertyName);
		return ascending ? builder.asc(path) : builder.desc(path);
	}

	@Override
	public String toString() {
		return propertyName + (ascending ? " asc" : " desc");
	}
}
