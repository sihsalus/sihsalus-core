package org.hibernate.criterion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.Locale;

public final class Restrictions {
	
	private Restrictions() {
	}
	
	public static Criterion eq(String propertyName, Object value) {
		return (builder, root) -> builder.equal(path(root, propertyName), value);
	}
	
	public static Criterion ne(String propertyName, Object value) {
		return (builder, root) -> builder.notEqual(path(root, propertyName), value);
	}
	
	public static Criterion isNull(String propertyName) {
		return (builder, root) -> builder.isNull(path(root, propertyName));
	}
	
	public static Criterion isNotNull(String propertyName) {
		return (builder, root) -> builder.isNotNull(path(root, propertyName));
	}
	
	public static Criterion isEmpty(String propertyName) {
		return (builder, root) -> {
			Expression<String> expression = path(root, propertyName).as(String.class);
			return builder.or(builder.isNull(expression), builder.equal(expression, ""));
		};
	}
	
	public static Criterion isNotEmpty(String propertyName) {
		return (builder, root) -> {
			Expression<String> expression = path(root, propertyName).as(String.class);
			return builder.and(builder.isNotNull(expression), builder.notEqual(expression, ""));
		};
	}
	
	public static Criterion ilike(String propertyName, String value) {
		return ilike(propertyName, value, MatchMode.ANYWHERE);
	}
	
	public static Criterion ilike(String propertyName, String value, MatchMode matchMode) {
		return (builder, root) -> {
			Expression<String> expression = builder.lower(path(root, propertyName).as(String.class));
			return builder.like(expression, matchMode.toMatchString(value));
		};
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Criterion gt(String propertyName, Comparable value) {
		return (builder, root) -> builder.greaterThan(path(root, propertyName).as(value.getClass()), value);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Criterion ge(String propertyName, Comparable value) {
		return (builder, root) -> builder.greaterThanOrEqualTo(path(root, propertyName).as(value.getClass()), value);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Criterion lt(String propertyName, Comparable value) {
		return (builder, root) -> builder.lessThan(path(root, propertyName).as(value.getClass()), value);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Criterion le(String propertyName, Comparable value) {
		return (builder, root) -> builder.lessThanOrEqualTo(path(root, propertyName).as(value.getClass()), value);
	}
	
	public static Criterion and(Criterion... criteria) {
		return (builder, root) -> builder.and(predicates(builder, root, criteria));
	}
	
	public static Criterion or(Criterion... criteria) {
		return (builder, root) -> builder.or(predicates(builder, root, criteria));
	}
	
	static Path<?> path(Root<?> root, String propertyName) {
		Path<?> path = root;
		for (String segment : propertyName.split("\\.")) {
			path = path.get(segment);
		}
		return path;
	}
	
	private static Predicate[] predicates(CriteriaBuilder builder, Root<?> root, Criterion[] criteria) {
		return Arrays.stream(criteria)
		        .filter(criterion -> criterion != null)
		        .map(criterion -> criterion.toPredicate(builder, root))
		        .toArray(Predicate[]::new);
	}
}
