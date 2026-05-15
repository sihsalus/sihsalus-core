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

import java.util.Date;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.billing.api.base.criteria.BillingCriteria;
import org.openmrs.module.billing.api.base.criteria.BillingRestrictions;

/**
 * Base template search class for {@link org.openmrs.OpenmrsObject} models.
 *
 * @param <T> The model class.
 */
public class BaseObjectTemplateSearch<T extends OpenmrsObject> {

	public static final long serialVersionUID = 0L;

	private T template;

	public BaseObjectTemplateSearch(T template) {
		this.template = template;
	}

	public T getTemplate() {
		return template;
	}

	public void setTemplate(T template) {
		this.template = template;
	}

	public void updateCriteria(BillingCriteria criteria) {
	}

	protected Predicate createPredicate(CriteriaBuilder cb, Root<?> root, String field, Object value,
	        ComparisonType comparisonType) {
		ComparisonType comparison = comparisonType == null ? ComparisonType.EQUAL : comparisonType;
		return switch (comparison) {
			case EQUAL -> cb.equal(BillingRestrictions.path(root, field), value);
			case NOT_EQUAL -> cb.notEqual(BillingRestrictions.path(root, field), value);
			case IS_NULL -> cb.isNull(BillingRestrictions.path(root, field));
			case IS_NOT_NULL -> cb.isNotNull(BillingRestrictions.path(root, field));
		};
	}

	protected Predicate createPredicate(CriteriaBuilder cb, Root<?> root, String field, String value,
	        StringComparisonType comparisonType) {
		StringComparisonType comparison = comparisonType == null ? StringComparisonType.EQUAL : comparisonType;
		return switch (comparison) {
			case EQUAL -> cb.equal(BillingRestrictions.path(root, field), value);
			case NOT_EQUAL -> cb.notEqual(BillingRestrictions.path(root, field), value);
			case IS_NULL -> cb.isNull(BillingRestrictions.path(root, field));
			case IS_NOT_NULL -> cb.isNotNull(BillingRestrictions.path(root, field));
			case IS_EMPTY -> cb.or(cb.isNull(BillingRestrictions.path(root, field)),
			    cb.equal(BillingRestrictions.path(root, field), ""));
			case IS_NOT_EMPTY -> cb.and(cb.isNotNull(BillingRestrictions.path(root, field)),
			    cb.notEqual(BillingRestrictions.path(root, field), ""));
			case LIKE -> cb.like(cb.lower(BillingRestrictions.path(root, field).as(String.class)),
			    "%" + value.toLowerCase() + "%");
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Predicate createPredicate(CriteriaBuilder cb, Root<?> root, String field, Date value,
	        DateComparisonType comparisonType) {
		DateComparisonType comparison = comparisonType == null ? DateComparisonType.EQUAL : comparisonType;
		return switch (comparison) {
			case EQUAL -> cb.equal(BillingRestrictions.path(root, field), value);
			case NOT_EQUAL -> cb.notEqual(BillingRestrictions.path(root, field), value);
			case IS_NULL -> cb.isNull(BillingRestrictions.path(root, field));
			case IS_NOT_NULL -> cb.isNotNull(BillingRestrictions.path(root, field));
			case GREATER_THAN -> cb.greaterThan(BillingRestrictions.path(root, field).as(Date.class), value);
			case GREATER_THAN_EQUAL -> cb.greaterThanOrEqualTo(BillingRestrictions.path(root, field).as(Date.class),
			    value);
			case LESS_THAN -> cb.lessThan(BillingRestrictions.path(root, field).as(Date.class), value);
			case LESS_THAN_EQUAL -> cb.lessThanOrEqualTo(BillingRestrictions.path(root, field).as(Date.class), value);
		};
	}

	public enum ComparisonType {
		EQUAL,
		NOT_EQUAL,
		IS_NULL,
		IS_NOT_NULL
	}

	public enum StringComparisonType {
		EQUAL,
		NOT_EQUAL,
		IS_NULL,
		IS_NOT_NULL,
		IS_EMPTY,
		IS_NOT_EMPTY,
		LIKE
	}

	public enum DateComparisonType {
		EQUAL,
		NOT_EQUAL,
		IS_NULL,
		IS_NOT_NULL,
		GREATER_THAN,
		GREATER_THAN_EQUAL,
		LESS_THAN,
		LESS_THAN_EQUAL,
	}
}
