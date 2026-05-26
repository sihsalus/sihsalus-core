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
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.billing.api.base.criteria.BillingCriteria;

/** Represents types that can provide access to a data source through hibernate. */
public interface BaseHibernateRepository {

  /**
   * @return the underlying session factory for building queries
   */
  DbSessionFactory getSessionFactory();

  /** Creates an HQL query. */
  Query<?> createQuery(String hql);

  /** Creates a new {@link BillingCriteria} for the given entity class. */
  <E extends OpenmrsObject> BillingCriteria createCriteria(Class<E> cls);

  /** Saves an entity to the database. */
  <E extends OpenmrsObject> E save(E entity);

  /** Saves a collection of entities to the database. */
  void saveAll(Collection<? extends OpenmrsObject> collection);

  /** Deletes an entity from the database. */
  <E extends OpenmrsObject> void delete(E entity);

  /** Executes the {@link BillingCriteria} and returns the first result or null. */
  <T> T selectValue(BillingCriteria criteria);

  /** Selects a single entity by id. */
  <E extends OpenmrsObject> E selectSingle(Class<E> cls, Serializable id);

  /** Selects a single entity matching the criteria (first result if multiple). */
  <E extends OpenmrsObject> E selectSingle(Class<E> cls, BillingCriteria criteria);

  /** Selects all entities of the given class. */
  <E extends OpenmrsObject> List<E> select(Class<E> cls);

  /** Selects entities matching the criteria. */
  <E extends OpenmrsObject> List<E> select(Class<E> cls, BillingCriteria criteria);
}
