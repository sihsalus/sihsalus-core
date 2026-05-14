package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortMemberAttribute;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortMemberAttributeDao extends AbstractGenericDao<CohortMemberAttribute> {
	
	public CohortMemberAttributeDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
