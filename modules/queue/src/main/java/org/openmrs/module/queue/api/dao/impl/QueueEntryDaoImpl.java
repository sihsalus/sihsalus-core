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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Patient;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry>
    implements QueueEntryDao {

  public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
    QueryParts queryParts = queryPartsFromSearchCriteria("select qe", searchCriteria);
    return list(
        queryParts.hql()
            + " order by qe.sortWeight desc, qe.startedAt asc, qe.dateCreated asc, qe.queueEntryId asc",
        QueueEntry.class,
        queryParts.parameters());
  }

  @Override
  public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
    QueryParts queryParts = queryPartsFromSearchCriteria("select count(qe)", searchCriteria);
    return createQuery(queryParts.hql(), Long.class, queryParts.parameters()).uniqueResult();
  }

  @Override
  public List<QueueEntry> getOverlappingQueueEntries(QueueEntrySearchCriteria searchCriteria) {
    Session session = getSessionFactory().getCurrentSession();
    CriteriaBuilder cb = session.getCriteriaBuilder();
    CriteriaQuery<QueueEntry> query = cb.createQuery(QueueEntry.class);
    Root<QueueEntry> root = query.from(QueueEntry.class);
    List<Predicate> predicates = new ArrayList<>();

    predicates.add(cb.equal(root.get("voided"), false));

    Collection<Queue> queues = searchCriteria.getQueues();
    if (queues != null) {
      if (queues.isEmpty()) {
        predicates.add(root.get("queue").isNull());
      } else {
        predicates.add(root.get("queue").in(searchCriteria.getQueues()));
      }
    }

    Patient patient = searchCriteria.getPatient();
    if (patient != null) {
      predicates.add(cb.equal(root.get("patient"), patient));
    }

    Date startedAt = searchCriteria.getStartedOn();
    if (startedAt != null) {
      // any queue entries that have either not ended or end after this queue entry starts
      predicates.add(
          cb.or(root.get("endedAt").isNull(), cb.greaterThan(root.get("endedAt"), startedAt)));
    }

    Date endedAt = searchCriteria.getEndedOn();
    if (endedAt != null) {
      // any queue entries that started before this queue entry ends
      predicates.add(cb.lessThan(root.get("startedAt"), endedAt));
    }

    query.where(cb.and(predicates.toArray(new Predicate[0])));

    return session.createQuery(query).list();
  }

  @Override
  public void flushSession() {
    getSessionFactory().getCurrentSession().flush();
  }

  @Override
  public boolean updateIfUnmodified(QueueEntry queueEntry, Date expectedDateChanged) {
    Session session = getSessionFactory().getCurrentSession();

    // This path issues a direct JPQL UPDATE and bypasses QueueEntryValidator; enforce the
    // strict-positive-duration invariant here so the DB never ends up with ended_at <= started_at.
    Date endedAt = queueEntry.getEndedAt();
    Date startedAt = queueEntry.getStartedAt();
    if (endedAt != null && startedAt != null && !endedAt.after(startedAt)) {
      throw new IllegalArgumentException(
          "Queue entry endedAt (" + endedAt + ") must be after startedAt (" + startedAt + ")");
    }

    // Evict the entity to prevent Hibernate from auto-flushing changes
    session.evict(queueEntry);

    // Build conditional update query - only succeeds if dateChanged matches expected value
    StringBuilder jpql = new StringBuilder();
    jpql.append("UPDATE QueueEntry qe SET ");
    jpql.append("qe.endedAt = :endedAt ");
    jpql.append("WHERE qe.queueEntryId = :id ");

    if (expectedDateChanged == null) {
      jpql.append("AND qe.dateChanged IS NULL");
    } else {
      jpql.append("AND qe.dateChanged = :expectedDateChanged");
    }

    jakarta.persistence.Query query = session.createQuery(jpql.toString());
    query.setParameter("endedAt", endedAt);
    query.setParameter("id", queueEntry.getQueueEntryId());
    if (expectedDateChanged != null) {
      query.setParameter("expectedDateChanged", expectedDateChanged);
    }

    int rowsUpdated = query.executeUpdate();
    return rowsUpdated > 0;
  }

  private QueryParts queryPartsFromSearchCriteria(
      String selectClause, QueueEntrySearchCriteria searchCriteria) {
    StringBuilder hql =
        new StringBuilder(selectClause).append(" from QueueEntry qe join qe.queue q where 1 = 1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    appendDeletedFilter(hql, "qe", searchCriteria.isIncludedVoided());
    limitByCollectionProperty(hql, parameters, "qe.queue", searchCriteria.getQueues());
    limitByCollectionProperty(hql, parameters, "q.location", searchCriteria.getLocations());
    limitByCollectionProperty(hql, parameters, "q.service", searchCriteria.getServices());
    limitToEqualsProperty(hql, parameters, "qe.patient", searchCriteria.getPatient());
    limitToEqualsProperty(hql, parameters, "qe.visit", searchCriteria.getVisit());
    limitByCollectionProperty(hql, parameters, "qe.priority", searchCriteria.getPriorities());
    limitByCollectionProperty(hql, parameters, "qe.status", searchCriteria.getStatuses());
    limitByCollectionProperty(
        hql, parameters, "qe.locationWaitingFor", searchCriteria.getLocationsWaitingFor());
    limitByCollectionProperty(
        hql, parameters, "qe.providerWaitingFor", searchCriteria.getProvidersWaitingFor());
    limitByCollectionProperty(
        hql, parameters, "qe.queueComingFrom", searchCriteria.getQueuesComingFrom());
    limitToGreaterThanOrEqualToProperty(
        hql, parameters, "qe.startedAt", searchCriteria.getStartedOnOrAfter());
    limitToLessThanOrEqualToProperty(
        hql, parameters, "qe.startedAt", searchCriteria.getStartedOnOrBefore());
    limitToEqualsProperty(hql, parameters, "qe.startedAt", searchCriteria.getStartedOn());
    limitToGreaterThanOrEqualToProperty(
        hql, parameters, "qe.endedAt", searchCriteria.getEndedOnOrAfter());
    limitToLessThanOrEqualToProperty(
        hql, parameters, "qe.endedAt", searchCriteria.getEndedOnOrBefore());
    limitToEqualsProperty(hql, parameters, "qe.endedAt", searchCriteria.getEndedOn());
    if (Boolean.TRUE.equals(searchCriteria.getHasVisit())) {
      hql.append(" and qe.visit is not null");
    } else if (Boolean.FALSE.equals(searchCriteria.getHasVisit())) {
      hql.append(" and qe.visit is null");
    }
    if (Boolean.TRUE.equals(searchCriteria.getIsEnded())) {
      hql.append(" and qe.endedAt is not null");
    } else if (Boolean.FALSE.equals(searchCriteria.getIsEnded())) {
      hql.append(" and qe.endedAt is null");
    }
    return new QueryParts(hql.toString(), parameters);
  }

  private record QueryParts(String hql, Map<String, Object> parameters) {}
}
