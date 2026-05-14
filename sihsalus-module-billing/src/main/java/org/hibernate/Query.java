package org.hibernate;

import java.util.List;

/**
 * Minimal compatibility wrapper for legacy module code that still expects the
 * pre-Hibernate 6 org.hibernate.Query type.
 */
public class Query {
	
	private final org.hibernate.query.Query<?> delegate;
	
	public Query(org.hibernate.query.Query<?> delegate) {
		this.delegate = delegate;
	}
	
	public Query setParameter(String name, Object value) {
		delegate.setParameter(name, value);
		return this;
	}
	
	public Query setParameter(int position, Object value) {
		delegate.setParameter(position, value);
		return this;
	}
	
	public Query setFirstResult(int startPosition) {
		delegate.setFirstResult(startPosition);
		return this;
	}
	
	public Query setMaxResults(int maxResult) {
		delegate.setMaxResults(maxResult);
		return this;
	}
	
	public Query setFetchSize(int fetchSize) {
		delegate.setFetchSize(fetchSize);
		return this;
	}
	
	public Object uniqueResult() {
		return delegate.uniqueResult();
	}
	
	public List<?> list() {
		return delegate.getResultList();
	}
}
