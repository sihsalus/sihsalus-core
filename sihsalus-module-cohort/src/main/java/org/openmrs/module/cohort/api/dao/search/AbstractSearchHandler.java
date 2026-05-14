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

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class AbstractSearchHandler implements ISearchQuery {
	
	@Autowired
	@Qualifier("sessionFactory")
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private SessionFactory sessionFactory;
	
	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
	
	/**
	 * Finds cohort members matching the specified name(can be givenName, middleName, or familyName)
	 *
	 * @param name givenName, middleName, or familyName
	 * @return {@link org.hibernate.Criteria}
	 */
	protected Criteria handleNames(Criteria criteria, String patientAlias, @NotNull String name) {
		return criteria.createCriteria(patientAlias + ".names", "_pn")
		        .add(Restrictions.or(Restrictions.like("_pn.givenName", name, MatchMode.START),
		            Restrictions.like("_pn.familyName", name, MatchMode.START),
		            Restrictions.like("_pn.middleName", name, MatchMode.START)));
	}
}
