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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;

public final class BillingRestrictions {

	private BillingRestrictions() {
	}

	public static BillingCriterion eq(String propertyName, Object value) {
		return (builder, root) -> builder.equal(path(root, propertyName), value);
	}

	public static BillingCriterion ne(String propertyName, Object value) {
		return (builder, root) -> builder.notEqual(path(root, propertyName), value);
	}

	public static BillingCriterion isNull(String propertyName) {
		return (builder, root) -> builder.isNull(path(root, propertyName));
	}

	public static BillingCriterion isNotNull(String propertyName) {
		return (builder, root) -> builder.isNotNull(path(root, propertyName));
	}

	public static BillingCriterion isEmpty(String propertyName) {
		return (builder, root) -> {
			Expression<String> expression = path(root, propertyName).as(String.class);
			return builder.or(builder.isNull(expression), builder.equal(expression, ""));
		};
	}

	public static BillingCriterion isNotEmpty(String propertyName) {
		return (builder, root) -> {
			Expression<String> expression = path(root, propertyName).as(String.class);
			return builder.and(builder.isNotNull(expression), builder.notEqual(expression, ""));
		};
	}

	public static BillingCriterion ilike(String propertyName, String value) {
		return ilike(propertyName, value, BillingMatchMode.ANYWHERE);
	}

	public static BillingCriterion ilike(String propertyName, String value, BillingMatchMode matchMode) {
		return (builder, root) -> {
			Expression<String> expression = builder.lower(path(root, propertyName).as(String.class));
			return builder.like(expression, matchMode.toMatchString(value));
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BillingCriterion gt(String propertyName, Comparable value) {
		return (builder, root) -> builder.greaterThan(path(root, propertyName).as(value.getClass()), value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BillingCriterion ge(String propertyName, Comparable value) {
		return (builder, root) -> builder.greaterThanOrEqualTo(path(root, propertyName).as(value.getClass()), value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BillingCriterion lt(String propertyName, Comparable value) {
		return (builder, root) -> builder.lessThan(path(root, propertyName).as(value.getClass()), value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BillingCriterion le(String propertyName, Comparable value) {
		return (builder, root) -> builder.lessThanOrEqualTo(path(root, propertyName).as(value.getClass()), value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BillingCriterion between(String propertyName, Comparable low, Comparable high) {
		return (builder, root) -> builder.between(path(root, propertyName).as(low.getClass()), low, high);
	}

	public static BillingCriterion and(BillingCriterion... criteria) {
		return (builder, root) -> builder.and(predicates(builder, root, criteria));
	}

	public static BillingCriterion or(BillingCriterion... criteria) {
		return (builder, root) -> builder.or(predicates(builder, root, criteria));
	}

	public static Path<?> path(Root<?> root, String propertyName) {
		Path<?> path = root;
		for (String segment : propertyName.split("\\.")) {
			path = path.get(segment);
		}
		return path;
	}

	private static Predicate[] predicates(CriteriaBuilder builder, Root<?> root, BillingCriterion[] criteria) {
		return Arrays.stream(criteria)
		        .filter(c -> c != null)
		        .map(c -> c.toPredicate(builder, root))
		        .toArray(Predicate[]::new);
	}
}
