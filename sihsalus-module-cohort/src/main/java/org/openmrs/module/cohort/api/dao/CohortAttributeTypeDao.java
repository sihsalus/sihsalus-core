package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortAttributeTypeDao extends AbstractGenericDao<CohortAttributeType> {
	
	public CohortAttributeTypeDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
