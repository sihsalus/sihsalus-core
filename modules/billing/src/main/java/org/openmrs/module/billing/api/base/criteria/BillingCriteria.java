/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.criteria;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;

public class BillingCriteria {

  private final Session session;

  private final Class<?> entityClass;

  private final List<BillingCriterion> criteria = new ArrayList<>();

  private final List<BillingOrder> orders = new ArrayList<>();

  private Integer firstResult;

  private Integer maxResults;

  private Integer fetchSize;

  public BillingCriteria(Session session, Class<?> entityClass) {
    this.session = session;
    this.entityClass = entityClass;
  }

  public BillingCriteria add(BillingCriterion criterion) {
    if (criterion != null) {
      criteria.add(criterion);
    }
    return this;
  }

  public BillingCriteria addOrder(BillingOrder order) {
    if (order != null) {
      orders.add(order);
    }
    return this;
  }

  public BillingCriteria setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

  public BillingCriteria setMaxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  public BillingCriteria setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
    return this;
  }

  public Object uniqueResult() {
    List<?> results = list();
    return results.isEmpty() ? null : results.get(0);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public List list() {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery query = builder.createQuery(entityClass);
    Root root = query.from(entityClass);
    query.select(root);
    applyRestrictions(query, builder, root);
    if (!orders.isEmpty()) {
      query.orderBy(orders.stream().map(o -> o.toJpaOrder(builder, root)).toList());
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

  public long count() {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<Long> query = builder.createQuery(Long.class);
    Root<?> root = query.from(entityClass);
    query.select(builder.count(root));
    applyRestrictions(query, builder, root);
    Long result = session.createQuery(query).uniqueResult();
    return result == null ? 0L : result;
  }

  private void applyRestrictions(AbstractQuery<?> query, CriteriaBuilder builder, Root<?> root) {
    if (!criteria.isEmpty()) {
      List<Predicate> predicates =
          criteria.stream().map(c -> c.toPredicate(builder, root)).toList();
      query.where(predicates.toArray(new Predicate[0]));
    }
  }
}
