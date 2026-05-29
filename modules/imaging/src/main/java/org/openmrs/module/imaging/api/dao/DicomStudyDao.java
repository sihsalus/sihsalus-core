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
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.openmrs.module.imaging.api.study.DicomStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository("imaging.DicomStudyDao")
public class DicomStudyDao {

  private static final Logger log = LoggerFactory.getLogger(DicomStudyDao.class);

  //	@Autowired
  DbSessionFactory sessionFactory;

  private DbSession getSession() {
    return sessionFactory.getCurrentSession();
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public DicomStudy get(int id) {
    return (DicomStudy) getSession().get(DicomStudy.class, id);
  }

  public List<DicomStudy> getAll() {
    return getSession().createQuery("FROM DicomStudy", DicomStudy.class).getResultList();
  }

  public List<DicomStudy> getByPatient(Patient patient) {
    return getSession()
        .createQuery("FROM DicomStudy d WHERE d.mrsPatient = :patient", DicomStudy.class)
        .setParameter("patient", patient)
        .getResultList();
  }

  public List<DicomStudy> getByConfiguration(OrthancConfiguration config) {
    return getSession()
        .createQuery("FROM DicomStudy d WHERE d.orthancConfiguration = :config", DicomStudy.class)
        .setParameter("config", config)
        .getResultList();
  }

  public DicomStudy getByStudyInstanceUID(OrthancConfiguration config, String studyInstanceUID) {
    return getSession()
        .createQuery(
            "FROM DicomStudy d WHERE d.studyInstanceUID = :studyInstanceUID "
                + "AND d.orthancConfiguration = :config",
            DicomStudy.class)
        .setParameter("studyInstanceUID", studyInstanceUID)
        .setParameter("config", config)
        .uniqueResult();
  }

  public void save(DicomStudy study) {
    getSession().merge(study);
  }

  public void remove(DicomStudy study) {
    getSession().delete(study);
  }

  public void updateLinkStatus(DicomStudy study, int newLinkStatus) {
    study.setLinkStatus(newLinkStatus);
    getSession().merge(study);
  }
}
