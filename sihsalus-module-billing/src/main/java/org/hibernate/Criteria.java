package org.hibernate;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.transform.ResultTransformer;

/**
 * Minimal compatibility wrapper for legacy module code that still expects the
 * removed Hibernate Criteria API.
 */
public class Criteria {
	
	private final Session session;
	
	private final Class<?> entityClass;
	
	private final List<Criterion> criteria = new ArrayList<>();
	
	private final List<Order> orders = new ArrayList<>();
	
	private Projection projection;
	
	private ResultTransformer resultTransformer;
	
	private Integer firstResult;
	
	private Integer maxResults;
	
	private Integer fetchSize;
	
	public Criteria(Session session, Class<?> entityClass) {
		this.session = session;
		this.entityClass = entityClass;
	}
	
	public Criteria add(Criterion criterion) {
		if (criterion != null) {
			criteria.add(criterion);
		}
		return this;
	}
	
	public Criteria addOrder(Order order) {
		if (order != null) {
			orders.add(order);
		}
		return this;
	}
	
	public Criteria setProjection(Projection projection) {
		this.projection = projection;
		return this;
	}
	
	public Projection getProjection() {
		return projection;
	}
	
	public Criteria setResultTransformer(ResultTransformer resultTransformer) {
		this.resultTransformer = resultTransformer;
		return this;
	}
	
	public ResultTransformer getResultTransformer() {
		return resultTransformer;
	}
	
	public Criteria setFirstResult(int firstResult) {
		this.firstResult = firstResult;
		return this;
	}
	
	public Criteria setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}
	
	public Criteria setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}
	
	public Object uniqueResult() {
		List<?> results = list();
		return results.isEmpty() ? null : results.get(0);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List list() {
		CriteriaBuilder builder = session.getCriteriaBuilder();
		
		if (Projections.isRowCount(projection)) {
			jakarta.persistence.criteria.CriteriaQuery<Long> query = builder.createQuery(Long.class);
			Root<?> root = query.from(entityClass);
			query.select(builder.count(root));
			applyRestrictions(query, builder, root);
			return session.createQuery(query).getResultList();
		}
		
		jakarta.persistence.criteria.CriteriaQuery query = builder.createQuery(entityClass);
		Root root = query.from(entityClass);
		query.select(root);
		applyRestrictions(query, builder, root);
		if (!orders.isEmpty()) {
			query.orderBy(orders.stream().map(order -> order.toJpaOrder(builder, root)).toList());
		}
		
		org.hibernate.query.Query<?> executable = session.createQuery(query);
		if (firstResult != null) {
			executable.setFirstResult(firstResult);
		}
		if (maxResults != null) {
			executable.setMaxResults(maxResults);
		}
		if (fetchSize != null) {
			executable.setFetchSize(fetchSize);
		}
		return executable.getResultList();
	}
	
	private void applyRestrictions(AbstractQuery<?> query, CriteriaBuilder builder, Root<?> root) {
		if (!criteria.isEmpty()) {
			List<Predicate> predicates = criteria.stream().map(criterion -> criterion.toPredicate(builder, root)).toList();
			query.where(predicates.toArray(new Predicate[0]));
		}
	}
}
