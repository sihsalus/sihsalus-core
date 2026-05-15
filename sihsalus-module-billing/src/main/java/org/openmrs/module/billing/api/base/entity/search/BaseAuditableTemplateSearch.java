/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.entity.search;

import org.openmrs.Auditable;
import org.openmrs.module.billing.api.base.criteria.BillingCriteria;
import org.openmrs.module.billing.api.base.criteria.BillingRestrictions;

/**
 * Base template search class for {@link org.openmrs.Auditable} models.
 *
 * @param <T> The model class.
 */
public class BaseAuditableTemplateSearch<T extends Auditable> extends BaseObjectTemplateSearch<T> {

	public static final long serialVersionUID = 0L;

	private DateComparisonType dateCreatedComparisonType;

	private DateComparisonType dateChangedComparisonType;

	public BaseAuditableTemplateSearch(T template) {
		super(template);
		this.dateCreatedComparisonType = DateComparisonType.EQUAL;
		this.dateChangedComparisonType = DateComparisonType.EQUAL;
	}

	public DateComparisonType getDateCreatedComparisonType() {
		return dateCreatedComparisonType;
	}

	public void setDateCreatedComparisonType(DateComparisonType dateCreatedComparisonType) {
		this.dateCreatedComparisonType = dateCreatedComparisonType;
	}

	public DateComparisonType getDateChangedComparisonType() {
		return dateChangedComparisonType;
	}

	public void setDateChangedComparisonType(DateComparisonType dateChangedComparisonType) {
		this.dateChangedComparisonType = dateChangedComparisonType;
	}

	@Override
	public void updateCriteria(BillingCriteria criteria) {
		super.updateCriteria(criteria);

		T t = getTemplate();
		if (t.getCreator() != null) {
			criteria.add(BillingRestrictions.eq("creator", t.getCreator()));
		}
		if (t.getDateCreated() != null) {
			criteria.add((cb, root) -> createPredicate(cb, root, "dateCreated", t.getDateCreated(),
			    dateCreatedComparisonType));
		}
		if (t.getChangedBy() != null) {
			criteria.add(BillingRestrictions.eq("changedBy", t.getChangedBy()));
		}
		if (t.getDateChanged() != null) {
			criteria.add((cb, root) -> createPredicate(cb, root, "dateChanged", t.getDateChanged(),
			    dateChangedComparisonType));
		}
	}
}
