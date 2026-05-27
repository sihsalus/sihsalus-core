/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.entity.db.hibernate;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.hibernate.query.Query;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.billing.api.base.criteria.BillingCriteria;
import org.springframework.transaction.annotation.Transactional;

/** Provides access to a data source through hibernate. */
public class BaseHibernateRepositoryImpl implements BaseHibernateRepository {

  private DbSessionFactory sessionFactory;

  public BaseHibernateRepositoryImpl(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public DbSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Query<?> createQuery(String hql) {
    return sessionFactory.getCurrentSession().createQuery(hql);
  }

  @Override
  public <E extends OpenmrsObject> BillingCriteria createCriteria(Class<E> cls) {
    return new BillingCriteria(
        sessionFactory.getHibernateSessionFactory().getCurrentSession(), cls);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends OpenmrsObject> E save(E entity) {
    DbSession session = sessionFactory.getCurrentSession();
    try {
      entity = (E) session.merge(entity);
    } catch (Exception ex) {
      throw new APIException(
          "An exception occurred while attempting to add a "
              + entity.getClass().getSimpleName()
              + " entity.",
          ex);
    }
    return entity;
  }

  @Override
  @Transactional
  public void saveAll(Collection<? extends OpenmrsObject> collection) {
    DbSession session = sessionFactory.getCurrentSession();
    try {
      if (collection != null && !collection.isEmpty()) {
        for (OpenmrsObject obj : collection) {
          session.merge(obj);
        }
      }
    } catch (Exception ex) {
      throw new APIException("An exception occurred while attempting to add a entity.", ex);
    }
  }

  @Override
  public <E extends OpenmrsObject> void delete(E entity) {
    DbSession session = sessionFactory.getCurrentSession();
    try {
      session.delete(entity);
    } catch (Exception ex) {
      throw new APIException(
          "An exception occurred while attempting to delete a "
              + entity.getClass().getSimpleName()
              + " entity.",
          ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T selectValue(BillingCriteria criteria) {
    try {
      return (T) criteria.uniqueResult();
    } catch (Exception ex) {
      throw new APIException("An exception occurred while attempting to select a value.", ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends OpenmrsObject> E selectSingle(Class<E> cls, Serializable id) {
    DbSession session = sessionFactory.getCurrentSession();
    try {
      return (E) session.get(cls, id);
    } catch (Exception ex) {
      throw new APIException(
          "An exception occurred while attempting to select a single "
              + cls.getSimpleName()
              + " entity with ID "
              + id
              + ".",
          ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends OpenmrsObject> E selectSingle(Class<E> cls, BillingCriteria criteria) {
    try {
      List<E> results = criteria.list();
      return results.isEmpty() ? null : results.get(0);
    } catch (Exception ex) {
      throw new APIException(
          "An exception occurred while attempting to select a single "
              + cls.getSimpleName()
              + " entity.",
          ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends OpenmrsObject> List<E> select(Class<E> cls) {
    try {
      return createCriteria(cls).list();
    } catch (Exception ex) {
      throw new APIException(
          "An exception occurred while attempting to get " + cls.getSimpleName() + " entities.",
          ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends OpenmrsObject> List<E> select(Class<E> cls, BillingCriteria criteria) {
    if (criteria == null) {
      return select(cls);
    }
    try {
      return criteria.list();
    } catch (Exception ex) {
      throw new APIException(
          "An exception occurred while attempting to select " + cls.getSimpleName() + " entities.",
          ex);
    }
  }
}
