package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortMemberAttributeType;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortMemberAttributeTypeDao extends AbstractGenericDao<CohortMemberAttributeType> {
	
	public CohortMemberAttributeTypeDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
