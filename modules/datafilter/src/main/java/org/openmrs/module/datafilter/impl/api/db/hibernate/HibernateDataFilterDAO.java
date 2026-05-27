/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.datafilter.impl.api.db.hibernate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.module.datafilter.impl.EntityBasisMap;
import org.openmrs.module.datafilter.impl.api.db.DataFilterDAO;

public class HibernateDataFilterDAO implements DataFilterDAO {

  private SessionFactory sessionFactory;

  /**
   * Sets the sessionFactory
   *
   * @param sessionFactory the sessionFactory to set
   */
  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * @see DataFilterDAO#getEntityBasisMap(String, String, String, String)
   */
  @Override
  public EntityBasisMap getEntityBasisMap(
      String entityIdentifier, String entityType, String basisIdentifier, String basisType) {
    Session session = sessionFactory.getCurrentSession();
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<EntityBasisMap> cq = cb.createQuery(EntityBasisMap.class);
    Root<EntityBasisMap> root = cq.from(EntityBasisMap.class);
    cq.select(root)
        .where(matchingMap(cb, root, entityIdentifier, entityType, basisIdentifier, basisType));
    return session.createQuery(cq).uniqueResult();
  }

  /**
   * @see DataFilterDAO#saveEntityBasisMap(EntityBasisMap)
   */
  @Override
  public EntityBasisMap saveEntityBasisMap(EntityBasisMap entityBasisMap) {
    sessionFactory.getCurrentSession().persist(entityBasisMap);
    return entityBasisMap;
  }

  /**
   * @see DataFilterDAO#deleteEntityBasisMap(EntityBasisMap)
   */
  @Override
  public void deleteEntityBasisMap(EntityBasisMap entityBasisMap) {
    sessionFactory.getCurrentSession().remove(entityBasisMap);
  }

  /**
   * @see DataFilterDAO#getEntityBasisMaps(String, String, String)
   */
  @Override
  public Collection<EntityBasisMap> getEntityBasisMaps(
      String entityIdentifier, String entityType, String basisType) {
    Session session = sessionFactory.getCurrentSession();
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<EntityBasisMap> cq = cb.createQuery(EntityBasisMap.class);
    Root<EntityBasisMap> root = cq.from(EntityBasisMap.class);
    cq.select(root)
        .where(
            cb.and(
                equalIgnoreCase(cb, root, "entityIdentifier", entityIdentifier),
                equalIgnoreCase(cb, root, "entityType", entityType),
                equalIgnoreCase(cb, root, "basisType", basisType)));
    return session.createQuery(cq).getResultList();
  }

  @Override
  public List<EntityBasisMap> getEntityBasisMapsByBasis(
      String entityType, String basisType, String basisIdentifier) {

    Session session = sessionFactory.getCurrentSession();

    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<EntityBasisMap> cq = cb.createQuery(EntityBasisMap.class);

    Root<EntityBasisMap> root = cq.from(EntityBasisMap.class);
    cq.select(root)
        .where(
            cb.and(
                equalIgnoreCase(cb, root, "entityType", entityType),
                equalIgnoreCase(cb, root, "basisType", basisType),
                equalIgnoreCase(cb, root, "basisIdentifier", basisIdentifier)));

    Query<EntityBasisMap> query = session.createQuery(cq);
    return query.getResultList();
  }

  private Predicate matchingMap(
      CriteriaBuilder cb,
      Root<EntityBasisMap> root,
      String entityIdentifier,
      String entityType,
      String basisIdentifier,
      String basisType) {
    return cb.and(
        equalIgnoreCase(cb, root, "entityIdentifier", entityIdentifier),
        equalIgnoreCase(cb, root, "entityType", entityType),
        equalIgnoreCase(cb, root, "basisIdentifier", basisIdentifier),
        equalIgnoreCase(cb, root, "basisType", basisType));
  }

  private Predicate equalIgnoreCase(
      CriteriaBuilder cb, Root<EntityBasisMap> root, String attribute, String value) {
    if (value == null) {
      return cb.isNull(root.get(attribute));
    }
    return cb.equal(cb.lower(root.get(attribute)), value.toLowerCase(Locale.ROOT));
  }
}
