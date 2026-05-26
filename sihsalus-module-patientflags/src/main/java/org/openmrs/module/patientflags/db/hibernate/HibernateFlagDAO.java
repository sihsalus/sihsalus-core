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
package org.openmrs.module.patientflags.db.hibernate;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.patientflags.DisplayPoint;
import org.openmrs.module.patientflags.Flag;
import org.openmrs.module.patientflags.PatientFlag;
import org.openmrs.module.patientflags.Priority;
import org.openmrs.module.patientflags.Tag;
import org.openmrs.module.patientflags.db.FlagDAO;

/** Implementation of the {@link FlagDAO} */
public class HibernateFlagDAO implements FlagDAO {

  /** Hibernate session factory */
  private DbSessionFactory sessionFactory;

  /**
   * Set session factory
   *
   * @param sessionFactory
   */
  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getAllFlags()
   */
  @SuppressWarnings("unchecked")
  public List<Flag> getAllFlags() throws DAOException {
    return sessionFactory.getCurrentSession().createQuery("from Flag").list();
  }

  @SuppressWarnings("unchecked")
  public List<Flag> getAllEnabledFlags() throws DAOException {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from Flag f where f.enabled = true")
        .list();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getFlag(Integer)
   */
  public Flag getFlag(Integer flagId) {
    return (Flag) sessionFactory.getCurrentSession().get(Flag.class, flagId);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getFlagByUuid(String)
   */
  public Flag getFlagByUuid(String uuid) throws DAOException {
    return (Flag)
        this.sessionFactory
            .getCurrentSession()
            .createQuery("from Flag f where f.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getPatientFlagByUuid(String)
   */
  public PatientFlag getPatientFlagByUuid(String uuid) throws DAOException {
    return (PatientFlag)
        this.sessionFactory
            .getCurrentSession()
            .createQuery("from PatientFlag f where f.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getFlagByName(String)
   */
  public Flag getFlagByName(String name) throws DAOException {
    List<Flag> list = getByName("Flag", name);

    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() == 0) {
      return null;
    } else {
      throw new APIException("Multiple flags found with the name '" + name + "'");
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#saveFlag(Flag)
   */
  public void saveFlag(Flag flag) throws DAOException {
    try {
      sessionFactory.getCurrentSession().merge(flag);
    } catch (Throwable t) {
      throw new DAOException(t);
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#searchFlags(String, String, Boolean, List)
   */
  @SuppressWarnings("unchecked")
  public List<Flag> searchFlags(String name, String evaluator, Boolean enabled, List<String> tags)
      throws DAOException {
    StringBuilder hql = new StringBuilder("select distinct f from Flag f");
    if (tags != null && tags.size() > 0) {
      hql.append(" join f.tags t");
    }

    String conjunction = " where ";
    if (StringUtils.isNotBlank(name)) {
      hql.append(conjunction).append("lower(f.name) like :name");
      conjunction = " and ";
    }

    if (StringUtils.isNotBlank(evaluator)) {
      hql.append(conjunction).append("f.evaluator = :evaluator");
      conjunction = " and ";
    }

    if (enabled != null) {
      hql.append(conjunction).append("f.enabled = :enabled");
      conjunction = " and ";
    }

    if (tags != null && tags.size() > 0) {
      hql.append(conjunction).append("t.name in (:tags)");
    }

    Query query = sessionFactory.getCurrentSession().createQuery(hql.toString());
    if (StringUtils.isNotBlank(name)) {
      query.setParameter("name", name.toLowerCase() + "%");
    }
    if (StringUtils.isNotBlank(evaluator)) {
      query.setParameter("evaluator", evaluator);
    }
    if (enabled != null) {
      query.setParameter("enabled", enabled);
    }
    if (tags != null && tags.size() > 0) {
      query.setParameterList("tags", tags);
    }
    return query.list();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#purgeFlag(Integer)
   */
  public void purgeFlag(Integer flagId) throws DAOException {
    Flag flag = getFlag(flagId);
    sessionFactory.getCurrentSession().delete(flag);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getAllTags()
   */
  @SuppressWarnings("unchecked")
  public List<Tag> getAllTags() throws DAOException {
    return sessionFactory.getCurrentSession().createQuery("from Tag").list();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getTag(Integer)
   */
  public Tag getTag(Integer tagId) {
    return (Tag) sessionFactory.getCurrentSession().get(Tag.class, tagId);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getTag(String)
   */
  public Tag getTag(String name) {
    List<Tag> list = getByName("Tag", name);

    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() == 0) {
      return null;
    } else {
      throw new APIException("Multiple tags found with the name '" + name + "'");
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getTagByUuid(String)
   */
  public Tag getTagByUuid(String uuid) throws DAOException {
    return (Tag)
        this.sessionFactory
            .getCurrentSession()
            .createQuery("from Tag t where t.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#saveTag(Tag)
   */
  public void saveTag(Tag tag) throws DAOException {
    try {
      sessionFactory.getCurrentSession().merge(tag);
    } catch (Throwable t) {
      throw new DAOException(t);
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#purgeTag(Integer)
   */
  @SuppressWarnings("unchecked")
  public void purgeTag(Integer tagId) throws DAOException {
    Tag tag = getTag(tagId);

    // first, we need to delete all references to the tag within Flags
    List<Flag> flags =
        sessionFactory
            .getCurrentSession()
            .createQuery("select distinct f from Flag f join f.tags t where t.tagId = :tagId")
            .setParameter("tagId", tagId)
            .list();
    flags.forEach(
        flag -> {
          flag.removeTag(tag);
          sessionFactory.getCurrentSession().merge(flag);
        });

    // then we can delete the tag itself
    sessionFactory.getCurrentSession().delete(tag);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getAllPriorities()
   */
  @SuppressWarnings("unchecked")
  public List<Priority> getAllPriorities() throws DAOException {
    return sessionFactory.getCurrentSession().createQuery("from Priority").list();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getPriority(Integer)
   */
  public Priority getPriority(Integer priorityId) {
    return (Priority) sessionFactory.getCurrentSession().get(Priority.class, priorityId);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getPriorityByUuid(String)
   */
  public Priority getPriorityByUuid(String uuid) throws DAOException {
    return (Priority)
        this.sessionFactory
            .getCurrentSession()
            .createQuery("from Priority p where p.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getPriorityByName(String)
   */
  public Priority getPriorityByName(String name) throws DAOException {
    List<Priority> list = getByName("Priority", name);

    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() == 0) {
      return null;
    } else {
      throw new APIException("Multiple priorities found with the name '" + name + "'");
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#savePriority(Priority)
   */
  public void savePriority(Priority priority) throws DAOException {
    try {
      sessionFactory.getCurrentSession().merge(priority);
    } catch (Throwable t) {
      throw new DAOException(t);
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#purgePriority(Integer)
   */
  public void purgePriority(Integer priorityId) throws DAOException {
    Priority priority = getPriority(priorityId);
    sessionFactory.getCurrentSession().delete(priority);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getAllDisplayPoints()
   */
  @SuppressWarnings("unchecked")
  public List<DisplayPoint> getAllDisplayPoints() throws DAOException {
    return sessionFactory.getCurrentSession().createQuery("from DisplayPoint").list();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getDisplayPoint(Integer)
   */
  public DisplayPoint getDisplayPoint(Integer displayPointId) {
    return (DisplayPoint)
        sessionFactory.getCurrentSession().get(DisplayPoint.class, displayPointId);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#getDisplayPoint(String)
   */
  @SuppressWarnings("unchecked")
  public DisplayPoint getDisplayPoint(String name) {
    Query query =
        sessionFactory
            .getCurrentSession()
            .createQuery("from DisplayPoint d where lower(d.name) = :name");
    query.setParameter("name", StringUtils.lowerCase(name));
    query.setMaxResults(1);

    List<DisplayPoint> list = query.list();
    if (list.size() > 0) {
      // note the assumption here is that two displaypoints with the same case-insensitive tags
      // aren't allowed; if there are two, this just returns the first one it finds
      return list.get(0);
    } else {
      return null;
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#saveDisplayPoint(DisplayPoint)
   */
  public void saveDisplayPoint(DisplayPoint displayPoint) throws DAOException {
    try {
      sessionFactory.getCurrentSession().merge(displayPoint);
    } catch (Throwable t) {
      throw new DAOException(t);
    }
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#purgeDisplayPoint(Integer)
   */
  public void purgeDisplayPoint(Integer displayPointId) throws DAOException {
    DisplayPoint displayPoint = getDisplayPoint(displayPointId);
    sessionFactory.getCurrentSession().delete(displayPoint);
  }

  public boolean isPriorityNameDuplicated(Priority priority) {
    return isNameDuplicated("Priority", "priorityId", priority.getName(), priority.getPriorityId());
  }

  public boolean isFlagNameDuplicated(Flag flag) {
    return isNameDuplicated("Flag", "flagId", flag.getName(), flag.getFlagId());
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Flag> getFlagsForPatient(Patient patient) throws DAOException {
    return sessionFactory
        .getCurrentSession()
        .createQuery("select pf.flag from PatientFlag pf where pf.patient = :patient")
        .setParameter("patient", patient)
        .list();
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#savePatientFlag(PatientFlag)
   */
  public void savePatientFlag(PatientFlag patientFlag) throws DAOException {
    sessionFactory.getCurrentSession().merge(patientFlag);
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#deletePatientFlagsForPatient(Patient)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void deletePatientFlagsForPatient(Patient patient) throws DAOException {
    List<PatientFlag> flags =
        sessionFactory
            .getCurrentSession()
            .createQuery("from PatientFlag pf where pf.patient = :patient and pf.voided = false")
            .setParameter("patient", patient)
            .list();
    flags.forEach(
        patientFlag -> {
          sessionFactory.getCurrentSession().delete(patientFlag);
        });
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#deletePatientFlagForPatient(Patient, Flag)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void deletePatientFlagForPatient(Patient patient, Flag flag) throws DAOException {
    List<PatientFlag> flags =
        sessionFactory
            .getCurrentSession()
            .createQuery(
                "from PatientFlag pf where pf.patient = :patient and pf.flag = :flag and pf.voided = false")
            .setParameter("patient", patient)
            .setParameter("flag", flag)
            .list(); // Should return a maximum of one flag
    flags.forEach(
        patientFlag -> {
          sessionFactory.getCurrentSession().delete(patientFlag);
        });
  }

  /**
   * @see org.openmrs.module.patientflags.db.FlagDAO#deletePatientFlagsForFlag(Flag)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void deletePatientFlagsForFlag(Flag flag) throws DAOException {
    List<PatientFlag> flags =
        sessionFactory
            .getCurrentSession()
            .createQuery("from PatientFlag pf where pf.flag = :flag and pf.voided = false")
            .setParameter("flag", flag)
            .list();
    flags.forEach(
        patientFlag -> {
          sessionFactory.getCurrentSession().delete(patientFlag);
        });
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<PatientFlag> getPatientFlags(Patient patient) throws DAOException {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from PatientFlag pf where pf.patient = :patient")
        .setParameter("patient", patient)
        .list();
  }

  /**
   * Delete all patient flags.
   *
   * @throws DAOException the dao exception
   */
  @SuppressWarnings("unchecked")
  @Override
  public void deleteAllPatientFlags() throws DAOException {
    List<PatientFlag> flags =
        sessionFactory.getCurrentSession().createQuery("from PatientFlag").list();

    flags.forEach(
        patientFlag -> {
          sessionFactory.getCurrentSession().delete(patientFlag);
        });
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> getByName(String entityName, String name) {
    Query query;
    if (Context.getAdministrationService().isDatabaseStringComparisonCaseSensitive()) {
      query =
          sessionFactory
              .getCurrentSession()
              .createQuery("from " + entityName + " e where lower(e.name) = :name");
      query.setParameter("name", StringUtils.lowerCase(name));
    } else {
      query =
          sessionFactory
              .getCurrentSession()
              .createQuery("from " + entityName + " e where e.name = :name");
      query.setParameter("name", name);
    }
    return query.list();
  }

  private boolean isNameDuplicated(String entityName, String idProperty, String name, Integer id) {
    StringBuilder hql =
        new StringBuilder("from ").append(entityName).append(" e where e.retired = false");
    if (name != null) {
      hql.append(" and e.name = :name");
    }
    if (id != null) {
      hql.append(" and e.").append(idProperty).append(" <> :id");
    }

    Query query = sessionFactory.getCurrentSession().createQuery(hql.toString());
    if (name != null) {
      query.setParameter("name", name);
    }
    if (id != null) {
      query.setParameter("id", id);
    }
    query.setMaxResults(1);
    return !query.list().isEmpty();
  }
}
