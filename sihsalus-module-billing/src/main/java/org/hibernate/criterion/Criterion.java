package org.hibernate.criterion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public interface Criterion {
	
	Predicate toPredicate(CriteriaBuilder builder, Root<?> root);
}
