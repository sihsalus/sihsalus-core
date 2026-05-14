package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortDao extends AbstractGenericDao<CohortM> {
	
	public CohortDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
