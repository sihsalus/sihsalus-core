/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.db;

import jakarta.persistence.Query;
import java.util.List;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.DbSessionFactory;

public class HibernateEmrEncounterDAO implements EmrEncounterDAO {

  private DbSessionFactory sessionFactory;

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Encounter> getEncountersByObsValueText(
      Patient patient,
      Concept obsConcept,
      String valueText,
      EncounterType encounterType,
      boolean includeAll) {

    StringBuilder hql =
        new StringBuilder(
            "select distinct obs.encounter from Obs obs where obs.valueText = :valueText");
    if (!includeAll) {
      hql.append(" and obs.voided = false");
    }
    if (obsConcept != null) {
      hql.append(" and obs.concept = :obsConcept");
    }
    if (encounterType != null) {
      hql.append(" and obs.encounter.encounterType = :encounterType");
    }
    if (patient != null) {
      hql.append(" and obs.person = :patient");
    }

    Query query = sessionFactory.getCurrentSession().createQuery(hql.toString());
    query.setParameter("valueText", valueText);
    if (obsConcept != null) {
      query.setParameter("obsConcept", obsConcept);
    }
    if (encounterType != null) {
      query.setParameter("encounterType", encounterType);
    }
    if (patient != null) {
      query.setParameter("patient", patient);
    }
    return query.getResultList();
  }

  @Override
  public List<Encounter> getEncountersByObsValueText(
      Concept obsConcept, String valueText, EncounterType encounterType, boolean includeAll) {
    return getEncountersByObsValueText(null, obsConcept, valueText, encounterType, includeAll);
  }
}
