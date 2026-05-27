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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.search.QueueRoomSearchCriteria;
import org.openmrs.module.queue.model.QueueRoom;
import org.springframework.beans.factory.annotation.Qualifier;

public class QueueRoomDaoImpl extends AbstractBaseQueueDaoImpl<QueueRoom> implements QueueRoomDao {

  public QueueRoomDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<QueueRoom> getQueueRooms(QueueRoomSearchCriteria searchCriteria) {
    StringBuilder hql =
        new StringBuilder("select qr from QueueRoom qr join qr.queue q where 1 = 1");
    Map<String, Object> parameters = new LinkedHashMap<>();
    appendDeletedFilter(hql, "qr", searchCriteria.isIncludeRetired());
    limitByCollectionProperty(hql, parameters, "qr.queue", searchCriteria.getQueues());
    limitByCollectionProperty(hql, parameters, "q.location", searchCriteria.getLocations());
    limitByCollectionProperty(hql, parameters, "q.service", searchCriteria.getServices());
    return list(hql.toString(), QueueRoom.class, parameters);
  }
}
