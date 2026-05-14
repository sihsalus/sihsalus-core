package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortTypeDao extends AbstractGenericDao<CohortType> {
	
	public CohortTypeDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
