package org.openmrs.module.cohort.api.dao;

import org.hibernate.SessionFactory;
import org.openmrs.module.cohort.CohortAttribute;
import org.openmrs.module.cohort.api.dao.search.SearchQueryHandler;

public class CohortAttributeDao extends AbstractGenericDao<CohortAttribute> {
	
	public CohortAttributeDao(SessionFactory sessionFactory, SearchQueryHandler searchHandler) {
		super(sessionFactory, searchHandler);
	}
}
