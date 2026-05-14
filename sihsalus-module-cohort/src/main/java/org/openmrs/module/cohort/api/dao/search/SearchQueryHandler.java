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

import static org.hibernate.criterion.Restrictions.eq;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.CohortType;
import org.springframework.stereotype.Component;

@SuppressWarnings("unchecked")
@Component(value = "cohort.search.cohortSearchHandler")
public class SearchQueryHandler extends AbstractSearchHandler implements ISearchQuery {
	
	public List<CohortM> findCohorts(String nameMatching, Map<String, String> attributes, CohortType cohortType,
	        boolean includeVoided) {
		Criteria criteria = getCurrentSession().createCriteria(CohortM.class);
		criteria.add(eq("voided", includeVoided));
		
		if (StringUtils.isNotBlank(nameMatching)) {
			criteria.add(Restrictions.ilike("name", nameMatching, MatchMode.ANYWHERE));
		}
		
		if (attributes != null && !attributes.isEmpty()) {
			Criteria attributeCriteria = criteria.createCriteria("attributes").add(Restrictions.eq("voided", false))
			        .createAlias("attributeType", "attrType");
			
			Disjunction disjunction = Restrictions.disjunction();
			for (String attribute : attributes.keySet()) {
				disjunction.add(Restrictions.conjunction(Restrictions.eq("attrType.name", attribute),
				    Restrictions.like("value", attributes.get(attribute), MatchMode.ANYWHERE)));
			}
			
			attributeCriteria.add(disjunction);
		}
		
		if (cohortType != null) {
			criteria.add(Restrictions.eq("cohortType.cohortTypeId", cohortType.getCohortTypeId()));
		}
		
		criteria.setProjection(null).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		
		return criteria.list();
	}
	
	@Override
	public Collection<CohortMember> findCohortMembersByPatientNames(String name) {
		String patientAlias = "_p";
		Criteria criteria = getCurrentSession().createCriteria(CohortMember.class).createCriteria("patient", patientAlias);
		return handleNames(criteria, patientAlias, name).list();
	}
	
	@Override
	public Collection<CohortMember> findCohortMembersByCohortAndPatient(String cohortUuid, String query) {
		String patientAlias = "_p21";
		Criteria criteria = getCurrentSession().createCriteria(CohortMember.class);
		criteria.createCriteria("cohort", "c").add(Restrictions.eq("c.uuid", cohortUuid));
		Criteria patientCriteria = criteria.createCriteria("patient", patientAlias);
		handleNames(patientCriteria, patientAlias, query);
		
		return criteria.list();
	}
}
