/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cohort.api.dao.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.CohortType;
import org.springframework.stereotype.Component;

@SuppressWarnings("unchecked")
@Component(value = "cohort.search.cohortSearchHandler")
public class SearchQueryHandler extends AbstractSearchHandler implements ISearchQuery {

  public List<CohortM> findCohorts(
      String nameMatching,
      Map<String, String> attributes,
      CohortType cohortType,
      boolean includeVoided) {
    StringBuilder hql =
        new StringBuilder("select distinct c from org.openmrs.module.cohort.CohortM c where 1 = 1");

    if (!includeVoided) {
      hql.append(" and c.voided = false");
    }
    if (StringUtils.isNotBlank(nameMatching)) {
      hql.append(" and lower(c.name) like :nameMatching");
    }
    if (attributes != null && !attributes.isEmpty()) {
      hql.append(
          " and exists (select a.id from org.openmrs.module.cohort.CohortAttribute a "
              + "where a.cohort = c and a.voided = false and (");
      int index = 0;
      for (String ignored : attributes.keySet()) {
        if (index > 0) {
          hql.append(" or ");
        }
        hql.append("(a.attributeType.name = :attributeName")
            .append(index)
            .append(" and lower(a.valueReference) like :attributeValue")
            .append(index)
            .append(")");
        index++;
      }
      hql.append("))");
    }
    if (cohortType != null) {
      hql.append(" and c.cohortType.cohortTypeId = :cohortTypeId");
    }

    org.hibernate.query.Query<CohortM> query =
        getCurrentSession().createQuery(hql.toString(), CohortM.class);
    if (StringUtils.isNotBlank(nameMatching)) {
      query.setParameter("nameMatching", "%" + nameMatching.toLowerCase() + "%");
    }
    if (attributes != null && !attributes.isEmpty()) {
      int index = 0;
      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
        query.setParameter("attributeName" + index, attribute.getKey());
        query.setParameter(
            "attributeValue" + index, "%" + attribute.getValue().toLowerCase() + "%");
        index++;
      }
    }
    if (cohortType != null) {
      query.setParameter("cohortTypeId", cohortType.getCohortTypeId());
    }
    return query.list();
  }

  @Override
  public Collection<CohortMember> findCohortMembersByPatientNames(String name) {
    return getCurrentSession()
        .createQuery(
            """
		                select distinct cm
		                from org.openmrs.module.cohort.CohortMember cm
		                join cm.patient p
		                join p.names n
		                where lower(n.givenName) like :name
		                    or lower(n.familyName) like :name
		                    or lower(n.middleName) like :name
		                """,
            CohortMember.class)
        .setParameter("name", startsWith(name))
        .list();
  }

  @Override
  public Collection<CohortMember> findCohortMembersByCohortAndPatient(
      String cohortUuid, String query) {
    return getCurrentSession()
        .createQuery(
            """
		                select distinct cm
		                from org.openmrs.module.cohort.CohortMember cm
		                join cm.patient p
		                join p.names n
		                where cm.cohort.uuid = :cohortUuid
		                    and (
		                        lower(n.givenName) like :name
		                        or lower(n.familyName) like :name
		                        or lower(n.middleName) like :name
		                    )
		                """,
            CohortMember.class)
        .setParameter("cohortUuid", cohortUuid)
        .setParameter("name", startsWith(query))
        .list();
  }
}
