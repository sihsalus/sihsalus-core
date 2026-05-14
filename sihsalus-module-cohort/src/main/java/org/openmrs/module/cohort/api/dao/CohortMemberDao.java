package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortMemberDao extends AbstractGenericDao<CohortMember> {
	
	public CohortMemberDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
