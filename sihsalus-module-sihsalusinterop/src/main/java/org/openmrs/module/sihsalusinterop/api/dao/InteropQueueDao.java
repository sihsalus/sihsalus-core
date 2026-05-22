/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.sihsalusinterop.api.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.sihsalusinterop.api.model.InteropQueueItem;
import org.springframework.stereotype.Repository;

/**
 * DAO para InteropQueueItem
 */
@Repository("sihsalusinterop.InteropQueueDao")
public class InteropQueueDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public InteropQueueItem save(InteropQueueItem item) {
		setAuditFields(item);
		if (item.getQueueId() == null) {
			sessionFactory.getCurrentSession().persist(item);
			return item;
		}
		return sessionFactory.getCurrentSession().merge(item);
	}

	private void setAuditFields(InteropQueueItem item) {
		Date now = new Date();
		User authenticatedUser = Context.getAuthenticatedUser();
		if (item.getQueueId() == null) {
			if (item.getUuid() == null) {
				item.setUuid(UUID.randomUUID().toString());
			}
			if (item.getCreator() == null) {
				item.setCreator(authenticatedUser);
			}
			if (item.getDateCreated() == null) {
				item.setDateCreated(now);
			}
			return;
		}
		item.setChangedBy(authenticatedUser);
		item.setDateChanged(now);
	}
	
	public InteropQueueItem getById(Integer id) {
		return (InteropQueueItem) sessionFactory.getCurrentSession()
				.get(InteropQueueItem.class, id);
	}
	
	@SuppressWarnings("unchecked")
	public List<InteropQueueItem> getPendingItems() {
		return sessionFactory.getCurrentSession()
				.createQuery("from InteropQueueItem where status = :status", InteropQueueItem.class)
				.setParameter("status", "PENDING")
				.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public List<InteropQueueItem> getAll() {
		return sessionFactory.getCurrentSession()
				.createQuery("from InteropQueueItem", InteropQueueItem.class)
				.getResultList();
	}
	
	public void delete(InteropQueueItem item) {
		sessionFactory.getCurrentSession().remove(item);
	}
}








