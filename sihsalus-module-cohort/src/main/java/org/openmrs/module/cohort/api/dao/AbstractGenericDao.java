/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cohort.api.dao;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.module.cohort.api.dao.search.ISearchQuery;
import org.openmrs.module.cohort.api.dao.search.PropValue;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

@SuppressWarnings("unchecked")
public abstract class AbstractGenericDao<W extends OpenmrsObject & Auditable> implements GenericDao<W> {
	
	private final SessionFactory sessionFactory;
	
	private final SearchQueryHandler searchHandler;
	
	private final Class<W> clazz;
	
	public AbstractGenericDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		this.sessionFactory = sessionFactory;
		this.searchHandler = searchHandler;
		
		clazz = (Class<W>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Override
	public ISearchQuery getSearchHandler() {
		return searchHandler;
	}
	
	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
	
	@Override
	public W get(int id) {
		return (W) getCurrentSession().get(clazz, id);
	}
	
	@Override
	public W get(String uuid) {
		return get(uuid, false);
	}
	
	@Override
	public W get(String uuid, boolean includeVoided) {
		StringBuilder hql = new StringBuilder(entityAlias()).append(" where e.uuid = :uuid");
		appendDeletedFilter(hql, includeVoided);
		return getCurrentSession().createQuery(hql.toString(), clazz).setParameter("uuid", uuid).uniqueResult();
	}
	
	@Override
	public Collection<W> findAll() {
		return this.findAll(false);
	}
	
	@Override
	public Collection<W> findAll(boolean includeRetired) {
		StringBuilder hql = new StringBuilder(entityAlias());
		appendWhereDeletedFilter(hql, includeRetired);
		return getCurrentSession().createQuery(hql.toString(), clazz).list();
	}
	
	@Override
	public W createOrUpdate(W entity) {
		return getCurrentSession().merge(entity);
	}
	
	@Override
	public void delete(W entity) {
		Session session = getCurrentSession();
		session.remove(session.contains(entity) ? entity : session.merge(entity));
	}
	
	@Override
	public void delete(String uuid) {
		this.delete(this.get(uuid));
	}
	
	/**
	 * By default, retired/voided objects are excluded for searches
	 * 
	 * @param propValue Property and value
	 * @return A Collection of W objects
	 */
	@Override
	public Collection<W> findBy(PropValue propValue) {
		return this.findBy(propValue, false);
	}
	
	@Override
	public Collection<W> findBy(PropValue propValue, boolean includeRetired) {
		QueryParts queryParts = queryPartsFor(propValue, includeRetired);
		return queryParts.applyParameters(getCurrentSession().createQuery(queryParts.hql(), clazz)).list();
	}
	
	@Override
	public W findByUniqueProp(PropValue propValue) {
		return this.findByUniqueProp(propValue, false);
	}
	
	@Override
	public W findByUniqueProp(PropValue propValue, boolean includeRetired) {
		QueryParts queryParts = queryPartsFor(propValue, includeRetired);
		return queryParts.applyParameters(getCurrentSession().createQuery(queryParts.hql(), clazz)).uniqueResult();
	}
	
	protected boolean isVoidable() {
		return Voidable.class.isAssignableFrom(clazz);
	}
	
	protected boolean isRetireable() {
		return Retireable.class.isAssignableFrom(clazz);
	}
	
	private QueryParts queryPartsFor(PropValue propValue, boolean includeDeleted) {
		Objects.requireNonNull(propValue, "propValue must not be null");
		String path = propValue.getAssociationPath().map(association -> "e." + association + ".")
		        .orElse("e.") + propValue.getProperty();
		StringBuilder hql = new StringBuilder(entityAlias()).append(" where ").append(path).append(" = :value");
		appendDeletedFilter(hql, includeDeleted);
		return new QueryParts(hql.toString(), List.of(new Parameter("value", propValue.getValue())));
	}

	private String entityAlias() {
		return "from " + clazz.getName() + " e";
	}

	private void appendWhereDeletedFilter(StringBuilder hql, boolean includeDeleted) {
		if (!includeDeleted) {
			if (isVoidable()) {
				hql.append(" where e.voided = false");
			} else if (isRetireable()) {
				hql.append(" where e.retired = false");
			}
		}
	}

	private void appendDeletedFilter(StringBuilder hql, boolean includeDeleted) {
		if (!includeDeleted) {
			if (isVoidable()) {
				hql.append(" and e.voided = false");
			} else if (isRetireable()) {
				hql.append(" and e.retired = false");
			}
		}
	}

	private record Parameter(String name, Object value) {}

	private record QueryParts(String hql, List<Parameter> parameters) {
		<T> org.hibernate.query.Query<T> applyParameters(org.hibernate.query.Query<T> query) {
			List<Parameter> safeParameters = new ArrayList<>(parameters);
			for (Parameter parameter : safeParameters) {
				query.setParameter(parameter.name(), parameter.value());
			}
			return query;
		}
	}
}
