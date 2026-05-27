/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.imaging.api.dao;

import java.util.List;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.springframework.stereotype.Repository;

/** Database methods for {@link OrthancConfiguration}. */
@Repository("imaging.OrthancConfigurationDao")
public class OrthancConfigurationDao {

  //	@Autowired
  DbSessionFactory sessionFactory;

  private DbSession getSession() {
    return sessionFactory.getCurrentSession();
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @SuppressWarnings("unchecked")
  public List<OrthancConfiguration> getAll() {
    return getSession().createQuery("FROM OrthancConfiguration").getResultList();
  }

  /**
   * @param id the configuration ID
   */
  public OrthancConfiguration get(int id) {
    return (OrthancConfiguration) getSession().get(OrthancConfiguration.class, id);
  }

  /**
   * @ return Orthanc configuration
   */
  public void saveNew(OrthancConfiguration config) {
    if (!getSession()
        .createQuery("FROM OrthancConfiguration c WHERE c.orthancBaseUrl = :orthancBaseUrl")
        .setParameter("orthancBaseUrl", config.getOrthancBaseUrl())
        .getResultList()
        .isEmpty()) {
      throw new IllegalArgumentException("A configuration with the same base URL already exists");
    }
    getSession().merge(config);
  }

  public void updateExisting(OrthancConfiguration config) {
    getSession().merge(config);
  }

  /**
   * @param config Orthanc Configuration
   */
  public void remove(OrthancConfiguration config) {
    getSession().delete(config);
  }
}
