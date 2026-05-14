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

import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.or;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.module.cohort.api.dao.search.PropValue;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

@Slf4j
@SuppressWarnings("unchecked")
@Getter
@Setter
public abstract class AbstractGenericDao<W extends OpenmrsObject & Auditable> implements GenericDao<W> {
	
	private final SessionFactory sessionFactory;
	
	private final SearchQueryHandler searchHandler;
	
	private final Class<W> clazz;
	
	public AbstractGenericDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		this.sessionFactory = sessionFactory;
		this.searchHandler = searchHandler;
		
		clazz = (Class<W>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
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
		Criteria criteria = getCurrentSession().createCriteria(clazz);
		includeDeletedObjects(criteria, includeVoided);
		return (W) criteria.add(eq("uuid", uuid)).uniqueResult();
	}
	
	@Override
	public Collection<W> findAll() {
		return this.findAll(false);
	}
	
	@Override
	public Collection<W> findAll(boolean includeRetired) {
		Criteria criteria = getCurrentSession().createCriteria(clazz);
		includeDeletedObjects(criteria, includeRetired);
		return criteria.list();
	}
	
	@Override
	public W createOrUpdate(W entity) {
		getCurrentSession().saveOrUpdate(entity);
		return entity;
	}
	
	@Override
	public void delete(W entity) {
		getCurrentSession().delete(entity);
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
		Criteria criteria = getCurrentSession().createCriteria(clazz);
		includeDeletedObjects(criteria, includeRetired);
		return propValue.getAssociationPath().isPresent()
		        ? criteria.createCriteria(propValue.getAssociationPath().get(), "_pv2021")
		                .add(eq("_pv2021." + propValue.getProperty(), propValue.getValue())).list()
		        : criteria.add(eq(propValue.getProperty(), propValue.getValue())).list();
	}
	
	@Override
	public W findByUniqueProp(PropValue propValue) {
		return this.findByUniqueProp(propValue, false);
	}
	
	@Override
	public W findByUniqueProp(PropValue propValue, boolean includeRetired) {
		Criteria criteria = getCurrentSession().createCriteria(clazz);
		includeDeletedObjects(criteria, includeRetired);
		return (W) (propValue.getAssociationPath().isPresent()
		        ? criteria.createCriteria(propValue.getAssociationPath().get(), "_cu2021")
		                .add(eq("_cu2021." + propValue.getProperty(), propValue.getValue())).uniqueResult()
		        : criteria.add(eq(propValue.getProperty(), propValue.getValue())).uniqueResult());
	}
	
	@Override
	public Collection<W> findByOr(Criterion... predicates) {
		Criteria orByCriteria = getCurrentSession().createCriteria(clazz);
		return orByCriteria.add(or(predicates)).list();
	}
	
	@Override
	public Collection<W> findByAnd(Criterion... predicates) {
		Criteria andByCriteria = getCurrentSession().createCriteria(clazz);
		return andByCriteria.add(and(predicates)).list();
	}
	
	protected boolean isVoidable() {
		return Voidable.class.isAssignableFrom(clazz);
	}
	
	protected boolean isRetireable() {
		return Retireable.class.isAssignableFrom(clazz);
	}
	
	protected void handleVoidable(Criteria criteria) {
		criteria.add(eq("voided", false));
	}
	
	protected void handleRetireable(Criteria criteria) {
		criteria.add(eq("retired", false));
	}
	
	protected void includeDeletedObjects(Criteria criteria, boolean includeDeleted) {
		if (!includeDeleted) {
			if (isVoidable()) {
				handleVoidable(criteria);
			} else if (isRetireable()) {
				handleRetireable(criteria);
			}
		}
	}
	
	@Override
	public Criteria createCriteria() {
		return getCurrentSession().createCriteria(clazz);
	}
}
