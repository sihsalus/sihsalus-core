/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao.impl;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.module.queue.api.dao.BaseQueueDao;

@Slf4j
@Getter
@Setter(AccessLevel.PACKAGE)
@SuppressWarnings("unchecked")
public class AbstractBaseQueueDaoImpl<Q extends OpenmrsObject & Auditable>
    implements BaseQueueDao<Q> {

  private final SessionFactory sessionFactory;

  private final Class<Q> clazz;

  public AbstractBaseQueueDaoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    this.clazz =
        (Class<Q>)
            ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }

  protected Session getCurrentSession() {
    return this.getSessionFactory().getCurrentSession();
  }

  @Override
  public Optional<Q> get(int id) {
    return Optional.ofNullable(getCurrentSession().find(this.clazz, id));
  }

  @Override
  public Optional<Q> get(@NotNull String uuid) {
    StringBuilder hql =
        new StringBuilder("from ").append(clazz.getName()).append(" e where e.uuid = :uuid");
    appendDeletedFilter(hql, "e", false);
    return Optional.ofNullable(
        createQuery(hql.toString(), clazz, Map.of("uuid", uuid)).uniqueResult());
  }

  @Override
  public Q createOrUpdate(Q queue) {
    return this.getCurrentSession().merge(queue);
  }

  @Override
  public void delete(Q queue) {
    Session session = this.getCurrentSession();
    session.remove(session.contains(queue) ? queue : session.merge(queue));
  }

  @Override
  public void delete(@NotNull String uuid) {
    this.get(uuid).ifPresent(this::delete);
  }

  @Override
  public List<Q> findAll() {
    return this.findAll(false);
  }

  @Override
  public List<Q> findAll(boolean includeVoided) {
    StringBuilder hql = new StringBuilder("from ").append(clazz.getName()).append(" e where 1 = 1");
    appendDeletedFilter(hql, "e", includeVoided);
    return list(hql.toString(), clazz, Map.of());
  }

  protected boolean isVoidable() {
    return Voidable.class.isAssignableFrom(clazz);
  }

  protected boolean isRetireable() {
    return Retireable.class.isAssignableFrom(clazz);
  }

  protected void appendDeletedFilter(StringBuilder hql, String alias, boolean includeDeleted) {
    if (!includeDeleted && isVoidable()) {
      hql.append(" and ").append(alias).append(".voided = false");
    } else if (!includeDeleted && isRetireable()) {
      hql.append(" and ").append(alias).append(".retired = false");
    }
  }

  /**
   * If the passed value is null, return without limiting If the passed value is not null, add
   * clause that the property must be equal to the value
   */
  protected void limitToEqualsProperty(
      StringBuilder hql, Map<String, Object> parameters, String property, Object value) {
    if (value != null) {
      String parameter = parameterName(property, parameters.size());
      hql.append(" and ").append(property).append(" = :").append(parameter);
      parameters.put(parameter, value);
    }
  }

  /**
   * If the passed value is null, return without limiting If the passed value is not null, add
   * clause that the property must greater or equal to the value
   */
  protected void limitToGreaterThanOrEqualToProperty(
      StringBuilder hql, Map<String, Object> parameters, String property, Object value) {
    if (value != null) {
      String parameter = parameterName(property, parameters.size());
      hql.append(" and ").append(property).append(" >= :").append(parameter);
      parameters.put(parameter, value);
    }
  }

  /**
   * If the passed value is null, return without limiting If the passed value is not null, add
   * clause that the property must be less or equal to the value
   */
  protected void limitToLessThanOrEqualToProperty(
      StringBuilder hql, Map<String, Object> parameters, String property, Object value) {
    if (value != null) {
      String parameter = parameterName(property, parameters.size());
      hql.append(" and ").append(property).append(" <= :").append(parameter);
      parameters.put(parameter, value);
    }
  }

  /**
   * If the passed values is null, return without limiting If the passed values is empty, add clause
   * that the property must be null If the passed values is not empty, add clause that the property
   * must be one of the given values
   */
  protected void limitByCollectionProperty(
      StringBuilder hql, Map<String, Object> parameters, String property, Collection<?> values) {
    if (values != null) {
      if (values.isEmpty()) {
        hql.append(" and ").append(property).append(" is null");
      } else {
        String parameter = parameterName(property, parameters.size());
        hql.append(" and ").append(property).append(" in (:").append(parameter).append(")");
        parameters.put(parameter, values);
      }
    }
  }

  protected <T> List<T> list(String hql, Class<T> resultClass, Map<String, Object> parameters) {
    return createQuery(hql, resultClass, parameters).list();
  }

  protected <T> Query<T> createQuery(
      String hql, Class<T> resultClass, Map<String, Object> parameters) {
    Query<T> query = getCurrentSession().createQuery(hql, resultClass);
    new LinkedHashMap<>(parameters)
        .forEach(
            (name, value) -> {
              if (value instanceof Collection<?> collection) {
                query.setParameterList(name, collection);
              } else {
                query.setParameter(name, value);
              }
            });
    return query;
  }

  private String parameterName(String property, int index) {
    return property.replaceAll("[^A-Za-z0-9]", "_") + "_" + index;
  }
}
