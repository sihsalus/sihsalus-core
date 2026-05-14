package org.hibernate.criterion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;

public class Order {
	
	private final String propertyName;
	
	private final boolean ascending;
	
	protected Order(String propertyName, boolean ascending) {
		this.propertyName = propertyName;
		this.ascending = ascending;
	}
	
	public static Order asc(String propertyName) {
		return new Order(propertyName, true);
	}
	
	public static Order desc(String propertyName) {
		return new Order(propertyName, false);
	}
	
	public jakarta.persistence.criteria.Order toJpaOrder(CriteriaBuilder builder, Root<?> root) {
		Path<?> path = Restrictions.path(root, propertyName);
		return ascending ? builder.asc(path) : builder.desc(path);
	}
	
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return propertyName + (ascending ? " asc" : " desc");
	}
	
	@Override
	public String toString() {
		return toSqlString(null, null);
	}
}
