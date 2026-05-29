/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.imaging.api.dao;

import java.util.List;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.imaging.api.worklist.RequestProcedure;
import org.openmrs.module.imaging.api.worklist.RequestProcedureStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository("imaging.RequestProcedureStepDao")
public class RequestProcedureStepDao {

  private static final Logger log = LoggerFactory.getLogger(RequestProcedureStepDao.class);

  DbSessionFactory sessionFactory;

  private DbSession getSession() {
    return sessionFactory.getCurrentSession();
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public RequestProcedureStep get(int id) {
    return (RequestProcedureStep) getSession().get(RequestProcedureStep.class, id);
  }

  public List<RequestProcedureStep> getAll() {
    return getSession()
        .createQuery("FROM RequestProcedureStep", RequestProcedureStep.class)
        .getResultList();
  }

  public List<RequestProcedureStep> getAllStepByRequestProcedure(
      RequestProcedure requestProcedure) {
    return getSession()
        .createQuery(
            "FROM RequestProcedureStep s WHERE s.requestProcedure = :requestProcedure",
            RequestProcedureStep.class)
        .setParameter("requestProcedure", requestProcedure)
        .getResultList();
  }

  public void save(RequestProcedureStep requestProcedureStep) {
    getSession().save(requestProcedureStep);
  }

  public void update(RequestProcedureStep requestProcedureStep) {
    getSession().merge(requestProcedureStep);
  }

  public void remove(RequestProcedureStep requestProcedureStep) {
    getSession().delete(requestProcedureStep);
  }

  public void updatePerformedProcedureStepStatus(RequestProcedureStep step, String newStatus) {
    step.setPerformedProcedureStepStatus(newStatus);
    getSession().merge(step);
  }
}
