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
package org.openmrs.module.bedmanagement.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.bedmanagement.AdmissionLocation;
import org.openmrs.module.bedmanagement.BedLayout;
import org.openmrs.module.bedmanagement.constants.BedStatus;
import org.openmrs.module.bedmanagement.dao.BedManagementDao;
import org.openmrs.module.bedmanagement.entity.Bed;
import org.openmrs.module.bedmanagement.entity.BedLocationMapping;
import org.openmrs.module.bedmanagement.entity.BedPatientAssignment;
import org.openmrs.module.bedmanagement.entity.BedTag;
import org.openmrs.module.bedmanagement.entity.BedTagMap;
import org.openmrs.module.bedmanagement.entity.BedType;

public class BedManagementDaoImpl implements BedManagementDao {

  SessionFactory sessionFactory;

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Bed getBedById(int id) {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from Bed b where b.id = :id", Bed.class)
        .setParameter("id", id)
        .uniqueResult();
  }

  @Override
  public Bed getBedByUuid(String uuid) {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from Bed b where b.uuid = :uuid and b.voided=false", Bed.class)
        .setParameter("uuid", uuid)
        .uniqueResult();
  }

  @Override
  public Bed getBedByPatient(Patient patient) {
    Session session = sessionFactory.getCurrentSession();
    Object[] row =
        session
            .createQuery(
                "select bpa.bed.bedNumber, bpa.bed.id from BedPatientAssignment bpa where"
                    + " bpa.patient = :patient and bpa.endDatetime is null AND bpa.voided is false",
                Object[].class)
            .setParameter("patient", patient)
            .uniqueResult();
    Bed bed = null;
    if (row != null) {
      bed = new Bed();
      bed.setBedNumber((String) row[0]);
      bed.setId((Integer) row[1]);
    }
    return bed;
  }

  @Override
  public Location getWardForBed(Bed bed) {
    Session session = sessionFactory.getCurrentSession();
    return session
        .createQuery(
            "select blm.location from BedLocationMapping blm where blm.bed = :bed", Location.class)
        .setParameter("bed", bed)
        .uniqueResult();
  }

  @Override
  public BedPatientAssignment getBedPatientAssignmentByUuid(String uuid) {
    Session session = sessionFactory.getCurrentSession();
    return session
        .createQuery(
            "from BedPatientAssignment bpa " + "where bpa.uuid = :uuid", BedPatientAssignment.class)
        .setParameter("uuid", uuid)
        .uniqueResult();
  }

  @Override
  public List<BedPatientAssignment> getBedPatientAssignmentByPatient(
      String patientUuid, boolean includeEnded) {
    Session session = sessionFactory.getCurrentSession();
    List<BedPatientAssignment> bpaList =
        session
            .createQuery(
                "select bpa from BedPatientAssignment bpa "
                    + "inner join bpa.patient p "
                    + "where p.uuid = :patientUuid AND "
                    + "(bpa.endDatetime IS NULL OR :includeEnded IS TRUE) AND "
                    + "(bpa.voided IS FALSE) "
                    + "order by bpa.startDatetime DESC",
                BedPatientAssignment.class)
            .setParameter("patientUuid", patientUuid)
            .setParameter("includeEnded", includeEnded)
            .getResultList();

    return bpaList;
  }

  @Override
  public List<BedPatientAssignment> getBedPatientAssignmentByEncounter(
      String encounterUuid, boolean includeEnded) {
    Session session = sessionFactory.getCurrentSession();
    List<BedPatientAssignment> bpaList;
    FlushMode flushMode = session.getHibernateFlushMode();
    try {
      session.setHibernateFlushMode(FlushMode.MANUAL);
      bpaList =
          session
              .createQuery(
                  "select bpa from BedPatientAssignment bpa "
                      + "inner join bpa.encounter enc "
                      + "where enc.uuid = :encounterUuid AND "
                      + "(bpa.endDatetime IS NULL OR :includeEnded IS TRUE) AND "
                      + "(bpa.voided IS FALSE) "
                      + "order by bpa.startDatetime DESC",
                  BedPatientAssignment.class)
              .setParameter("encounterUuid", encounterUuid)
              .setParameter("includeEnded", includeEnded)
              .getResultList();
    } finally {
      session.setHibernateFlushMode(flushMode);
    }
    return bpaList;
  }

  public List<BedPatientAssignment> getBedPatientAssignmentByVisit(
      String visitUuid, boolean includeEnded) {
    Session session = sessionFactory.getCurrentSession();
    List<BedPatientAssignment> bpaList =
        session
            .createQuery(
                "select bpa from BedPatientAssignment bpa "
                    + "inner join bpa.encounter enc "
                    + "inner join enc.visit v "
                    + "where v.uuid = :visitUuid AND "
                    + "(bpa.endDatetime IS NULL OR :includeEnded IS TRUE) AND "
                    + "(bpa.voided IS FALSE) "
                    + "order by bpa.startDatetime DESC",
                BedPatientAssignment.class)
            .setParameter("visitUuid", visitUuid)
            .setParameter("includeEnded", includeEnded)
            .getResultList();

    return bpaList;
  }

  @Override
  public List<BedPatientAssignment> getCurrentAssignmentsByBed(Bed bed) {
    Session session = sessionFactory.getCurrentSession();
    List<BedPatientAssignment> assignments =
        session
            .createQuery(
                "from BedPatientAssignment where bed=:bed and endDatetime is null and voided is"
                    + " false",
                BedPatientAssignment.class)
            .setParameter("bed", bed)
            .getResultList();
    return assignments;
  }

  @Override
  public Bed getLatestBedByVisit(String visitUuid) {
    Session session = sessionFactory.getCurrentSession();
    return session
        .createQuery(
            "select bpa.bed from BedPatientAssignment bpa inner join bpa.encounter enc inner join"
                + " enc.visit v where v.uuid = :visitUuid and bpa.voided is false order by"
                + " bpa.startDatetime DESC",
            Bed.class)
        .setParameter("visitUuid", visitUuid)
        .setMaxResults(1)
        .uniqueResult();
  }

  @Override
  public List<BedTag> getAllBedTags() {
    Session session = sessionFactory.getCurrentSession();
    return session
        .createQuery("from BedTag where voided =:voided", BedTag.class)
        .setParameter("voided", false)
        .getResultList();
  }

  @Override
  public List<AdmissionLocation> getAdmissionLocations(List<Location> locations) {
    String sql =
        "select l from Location l "
            + "where l in :locations and "
            + "(l.parentLocation not in :locations or l.parentLocation is null) and "
            + "l.retired = false";
    Query<Location> query = sessionFactory.getCurrentSession().createQuery(sql, Location.class);
    query.setParameterList("locations", locations);
    List<Location> locationList = query.getResultList();

    List<AdmissionLocation> admissionLocations = new ArrayList<>();
    for (Location location : locationList) {
      admissionLocations.add(this.getAdmissionLocationForLocation(location));
    }

    return admissionLocations;
  }

  @Override
  public AdmissionLocation getAdmissionLocationForLocation(Location location) {
    Session session = sessionFactory.getCurrentSession();
    Set<Location> locations = new HashSet<Location>(Arrays.asList(location));
    Set<Location> childLocations = location.getChildLocations();
    if (!CollectionUtils.isEmpty(childLocations)) {
      locations.addAll(childLocations);
    }

    String hql =
        "select count(blm.bed) as totalBeds , COALESCE(sum(CASE WHEN blm.bed IS NOT NULL AND"
            + " blm.bed.status = :occupied THEN 1 ELSE 0 END), 0) as occupiedBeds from"
            + " BedLocationMapping blm where blm.location in (:locations)";

    Object[] row =
        session
            .createQuery(hql, Object[].class)
            .setParameterList("locations", locations)
            .setParameter("occupied", BedStatus.OCCUPIED.toString())
            .uniqueResult();
    AdmissionLocation admissionLocation = new AdmissionLocation();
    if (row != null) {
      admissionLocation.setTotalBeds(((Number) row[0]).longValue());
      admissionLocation.setOccupiedBeds(((Number) row[1]).longValue());
    }
    List<BedLayout> bedLayouts = getBedLayoutsByLocation(location);

    admissionLocation.setWard(location);
    admissionLocation.setBedLayouts(bedLayouts);
    return admissionLocation;
  }

  @Override
  public List<BedLocationMapping> getBedLocationMappingsByLocation(Location location) {
    String hql = "select blm " + "from BedLocationMapping blm " + "where blm.location=:location";

    Query<BedLocationMapping> query =
        sessionFactory.getCurrentSession().createQuery(hql, BedLocationMapping.class);
    query.setParameter("location", location);
    return query.getResultList();
  }

  @Override
  public BedLocationMapping getBedLocationMappingByLocationAndRowAndColumn(
      Location location, Integer row, Integer column) {
    String hql =
        "select blm "
            + "from BedLocationMapping blm "
            + "where blm.location=:location "
            + "and blm.row=:row and blm.column=:column";

    Query<BedLocationMapping> query =
        sessionFactory.getCurrentSession().createQuery(hql, BedLocationMapping.class);
    query.setParameter("location", location);
    query.setParameter("row", row);
    query.setParameter("column", column);
    return query.uniqueResult();
  }

  @Override
  public BedLocationMapping saveBedLocationMapping(BedLocationMapping bedLocationMapping) {
    Session session = this.sessionFactory.getCurrentSession();
    return session.merge(bedLocationMapping);
  }

  @Override
  public List<BedLayout> getBedLayoutsByLocation(Location location) {
    List<BedLayout> bedLayouts = new ArrayList<>();
    Set<Location> locations = new HashSet<>(Arrays.asList(location));
    Set<Location> childLocations = location.getChildLocations();
    if (!CollectionUtils.isEmpty(childLocations)) {
      locations.addAll(childLocations);
    }
    List<BedLocationMapping> bedLocationMappings =
        sessionFactory
            .getCurrentSession()
            .createQuery(
                "select blm from BedLocationMapping blm where blm.location in (:locations) ",
                BedLocationMapping.class)
            .setParameter("locations", locations)
            .getResultList();
    for (BedLocationMapping blm : bedLocationMappings) {
      BedLayout bedLayout = new BedLayout();
      bedLayout.setRowNumber(blm.getRow());
      bedLayout.setColumnNumber(blm.getColumn());
      bedLayout.setLocation(blm.getLocation().getName());
      if (blm.getBed() != null) {
        bedLayout.setBedNumber(blm.getBed().getBedNumber());
        bedLayout.setBedId(blm.getBed().getId());
        bedLayout.setBedUuid(blm.getBed().getUuid());
        bedLayout.setStatus(blm.getBed().getStatus());
        bedLayout.setBedType(blm.getBed().getBedType());
        bedLayout.setBedTagMaps(new HashSet<>());
        if (blm.getBed().getBedTagMap() != null) {
          for (BedTagMap bedTagMap : blm.getBed().getBedTagMap()) {
            if (BooleanUtils.isNotTrue(bedTagMap.getVoided())) {
              bedLayout.addBedTagMap(bedTagMap);
            }
          }
        }
        bedLayout.setBedPatientAssignments(getCurrentAssignmentsByBed(blm.getBed()));
      }
      bedLayouts.add(bedLayout);
    }
    return bedLayouts;
  }

  @Override
  public BedLocationMapping getBedLocationMappingByBed(Bed bed) {
    String hql =
        "select blm "
            + "from BedLocationMapping blm "
            + "where blm.bed.voided=:voided and blm.bed=:bed";

    Query<BedLocationMapping> query =
        sessionFactory.getCurrentSession().createQuery(hql, BedLocationMapping.class);
    query.setParameter("voided", false);
    query.setParameter("bed", bed);
    return query.uniqueResult();
  }

  private Query<Bed> createGetBedsQuery(
      Location location, BedType bedType, BedStatus bedStatus, Integer limit, Integer offset) {
    StringBuilder hql;
    if (location != null) {
      hql =
          new StringBuilder(
              "select blm.bed from BedLocationMapping blm join blm.bed bed "
                  + "where blm.location = :location and bed.voided = false");
    } else {
      hql = new StringBuilder("from Bed bed where bed.voided = false");
    }
    if (bedStatus != null) hql.append(" and bed.status = :bedStatus");
    if (bedType != null) hql.append(" and bed.bedType = :bedType");

    Query<Bed> query = sessionFactory.getCurrentSession().createQuery(hql.toString(), Bed.class);
    if (location != null) query.setParameter("location", location);
    if (bedStatus != null) query.setParameter("bedStatus", bedStatus.toString());
    if (bedType != null) query.setParameter("bedType", bedType);
    return applyPaging(query, limit, offset);
  }

  @Override
  public List<Bed> getBeds(
      Location location, BedType bedType, BedStatus status, Integer limit, Integer offset) {
    return createGetBedsQuery(location, bedType, status, limit, offset).list();
  }

  @Override
  public Integer getBedCountByLocation(Location location) {
    return sessionFactory
        .getCurrentSession()
        .createQuery(
            "select count(blm.bed) from BedLocationMapping blm join blm.bed bed "
                + "where blm.location = :location and bed.voided = false",
            Long.class)
        .setParameter("location", location)
        .uniqueResult()
        .intValue();
  }

  @Override
  public Bed saveBed(Bed bed) {
    Session session = this.sessionFactory.getCurrentSession();
    return session.merge(bed);
  }

  @Override
  public BedTag getBedTagByName(String name) {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from BedTag where name = :name and voided = false", BedTag.class)
        .setParameter("name", name)
        .uniqueResult();
  }

  @Override
  public BedTag getBedTagByUuid(String uuid) {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from BedTag where uuid = :uuid and voided = false", BedTag.class)
        .setParameter("uuid", uuid)
        .uniqueResult();
  }

  @Override
  public List<BedTag> getBedTags(String name, Integer limit, Integer offset) {
    StringBuilder hql = new StringBuilder("from BedTag where voided = false");
    if (name != null) hql.append(" and name = :name");
    Query<BedTag> query =
        sessionFactory.getCurrentSession().createQuery(hql.toString(), BedTag.class);
    if (name != null) query.setParameter("name", name);
    return applyPaging(query, limit, offset).list();
  }

  @Override
  public BedTag saveBedTag(BedTag bedTag) {
    Session session = this.sessionFactory.getCurrentSession();
    return session.merge(bedTag);
  }

  @Override
  public void deleteBedTag(BedTag bedTag) {
    Session session = this.sessionFactory.getCurrentSession();
    session.remove(session.contains(bedTag) ? bedTag : session.merge(bedTag));
  }

  @Override
  public BedType getBedTypeById(Integer id) {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from BedType where id = :id", BedType.class)
        .setParameter("id", id)
        .uniqueResult();
  }

  @Override
  public BedType getBedTypeByUuid(String uuid) {
    return sessionFactory
        .getCurrentSession()
        .createQuery("from BedType where uuid = :uuid and retired = false", BedType.class)
        .setParameter("uuid", uuid)
        .uniqueResult();
  }

  @Override
  public List<BedType> getBedTypes(String name, Integer limit, Integer offset) {
    StringBuilder hql = new StringBuilder("from BedType where retired = false");
    if (name != null) hql.append(" and name = :name");
    Query<BedType> query =
        sessionFactory.getCurrentSession().createQuery(hql.toString(), BedType.class);
    if (name != null) query.setParameter("name", name);
    return applyPaging(query, limit, offset).list();
  }

  private <T> Query<T> applyPaging(Query<T> query, Integer limit, Integer offset) {
    if (limit != null) {
      query.setMaxResults(limit);
      if (offset != null) query.setFirstResult(offset);
    }
    return query;
  }

  @Override
  public BedType saveBedType(BedType bedType) {
    Session session = this.sessionFactory.getCurrentSession();
    return session.merge(bedType);
  }

  @Override
  public void deleteBedType(BedType bedType) {
    Session session = this.sessionFactory.getCurrentSession();
    session.remove(session.contains(bedType) ? bedType : session.merge(bedType));
  }

  @Override
  public void deleteBedLocationMapping(BedLocationMapping bedLocationMapping) {
    Session session = this.sessionFactory.getCurrentSession();
    session.remove(
        session.contains(bedLocationMapping)
            ? bedLocationMapping
            : session.merge(bedLocationMapping));
  }

  @Override
  public BedPatientAssignment saveBedPatientAssignment(BedPatientAssignment bedPatientAssignment) {
    Session session = this.sessionFactory.getCurrentSession();
    return session.merge(bedPatientAssignment);
  }
}
