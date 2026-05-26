package org.openmrs.module.appointments.dao.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Provider;
import org.openmrs.module.appointments.dao.AppointmentDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentPriority;
import org.openmrs.module.appointments.model.AppointmentSearchRequest;
import org.openmrs.module.appointments.model.AppointmentSearchRequestModel;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.util.DateUtil;
import org.springframework.transaction.annotation.Transactional;

public class AppointmentDaoImpl implements AppointmentDao {

  private static final int APPOINTMENT_SEARCH_DEFAULT_LIMIT = 50;
  private SessionFactory sessionFactory;

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<Appointment> getAllAppointments(Date forDate) {
    StringBuilder hql =
        new StringBuilder(
            "select a from Appointment a join a.patient p where a.voided = false"
                + " and p.voided = false and p.personVoided = false");
    Map<String, Object> params = new HashMap<>();
    if (forDate != null) {
      Date maxDate = new Date(forDate.getTime() + TimeUnit.DAYS.toMillis(1));
      hql.append(" and a.startDateTime >= :forDate and a.endDateTime < :maxDate");
      params.put("forDate", forDate);
      params.put("maxDate", maxDate);
    }
    return listAppointments(hql.toString(), params);
  }

  @Override
  public List<Appointment> getAllAppointmentsReminder(String hours) {
    StringBuilder hql =
        new StringBuilder(
            "select a from Appointment a join a.patient p where p.voided = false"
                + " and p.personVoided = false and a.status <> :cancelled");
    Map<String, Object> params = new HashMap<>();
    params.put("cancelled", AppointmentStatus.Cancelled);
    if (hours != null) {
      Integer hoursOffset;
      try {
        hoursOffset = Integer.valueOf(hours);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid appointment reminder hour value: " + hours, e);
      }
      Date minDate =
          new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(hoursOffset));
      Date maxDate = new Date(minDate.getTime() + TimeUnit.HOURS.toMillis(1));
      hql.append(" and a.startDateTime >= :minDate and a.startDateTime < :maxDate");
      params.put("minDate", minDate);
      params.put("maxDate", maxDate);
    }
    return listAppointments(hql.toString(), params);
  }

  @Transactional
  @Override
  public void save(Appointment appointment) {
    sessionFactory.getCurrentSession().merge(appointment);
  }

  @Override
  public List<Appointment> search(Appointment appointment) {
    StringBuilder hql = new StringBuilder("select distinct a from Appointment a");
    Map<String, Object> params = new HashMap<>();
    Provider provider = appointment == null ? null : appointment.getProvider();
    if (provider != null) {
      hql.append(" join a.providers ap join ap.provider provider");
    }
    hql.append(" where 1 = 1");
    if (appointment != null) {
      addEqual(hql, params, "a.uuid", "uuid", appointment.getUuid());
      addEqual(
          hql,
          params,
          "a.appointmentNumber",
          "appointmentNumber",
          appointment.getAppointmentNumber());
      addEqual(hql, params, "a.patient", "patient", appointment.getPatient());
      addEqual(hql, params, "a.location", "location", appointment.getLocation());
      addEqual(hql, params, "a.service", "service", appointment.getService());
      addEqual(hql, params, "a.serviceType", "serviceType", appointment.getServiceType());
      addEqual(hql, params, "a.status", "status", appointment.getStatus());
      addEqual(hql, params, "a.priority", "priority", appointment.getPriority());
      addEqual(hql, params, "a.voided", "voided", appointment.getVoided());
      if (provider != null) {
        addEqual(hql, params, "provider", "provider", provider);
      }
    }
    return listAppointments(hql.toString(), params);
  }

  @Override
  public List<Appointment> search(AppointmentSearchRequestModel searchQuery) {
    AppointmentSearchQuery appointmentSearchQuery = buildAppointmentSearchQuery(searchQuery);
    return listAppointments(appointmentSearchQuery.hql(), appointmentSearchQuery.params());
  }

  @Override
  public List<Appointment> getAllFutureAppointmentsForService(
      AppointmentServiceDefinition appointmentServiceDefinition) {
    return listAppointments(
        "select a from Appointment a join a.patient p where a.service = :service"
            + " and a.endDateTime > :now and a.voided = false and p.voided = false"
            + " and p.personVoided = false and a.status <> :cancelled",
        Map.of(
            "service",
            appointmentServiceDefinition,
            "now",
            new Date(),
            "cancelled",
            AppointmentStatus.Cancelled));
  }

  @Override
  public List<Appointment> getAllFutureAppointmentsForServiceType(
      AppointmentServiceType appointmentServiceType) {
    return listAppointments(
        "select a from Appointment a join a.patient p where a.serviceType = :serviceType"
            + " and a.endDateTime > :now and a.voided = false and p.voided = false"
            + " and p.personVoided = false and a.status <> :cancelled",
        Map.of(
            "serviceType",
            appointmentServiceType,
            "now",
            new Date(),
            "cancelled",
            AppointmentStatus.Cancelled));
  }

  @Override
  public List<Appointment> getAppointmentsForService(
      AppointmentServiceDefinition appointmentServiceDefinition,
      Date startDate,
      Date endDate,
      List<AppointmentStatus> appointmentStatusFilterList) {
    StringBuilder hql =
        new StringBuilder(
            "select a from Appointment a join a.patient p left join a.serviceType st"
                + " where (a.serviceType is null or st.voided = false) and a.voided = false"
                + " and p.voided = false and p.personVoided = false"
                + " and a.startDateTime >= :startDate and a.startDateTime <= :endDate"
                + " and a.service = :service");
    Map<String, Object> params = new HashMap<>();
    params.put("startDate", startDate);
    params.put("endDate", endDate);
    params.put("service", appointmentServiceDefinition);
    if (appointmentStatusFilterList != null && !appointmentStatusFilterList.isEmpty()) {
      hql.append(" and a.status in (:statuses)");
      params.put("statuses", appointmentStatusFilterList);
    }
    return listAppointments(hql.toString(), params);
  }

  @Override
  public Appointment getAppointmentByUuid(String uuid) {
    return uniqueAppointment(
        "select a from Appointment a where a.uuid = :uuid", Map.of("uuid", uuid));
  }

  @Override
  public List<Appointment> getAllAppointmentsInDateRange(Date startDate, Date endDate) {
    StringBuilder hql =
        new StringBuilder(
            "select a from Appointment a join a.patient p where a.voided = false"
                + " and p.voided = false and p.personVoided = false");
    Map<String, Object> params = new HashMap<>();
    if (startDate != null) {
      hql.append(" and a.startDateTime >= :startDate");
      params.put("startDate", startDate);
    }
    if (endDate != null) {
      hql.append(" and a.endDateTime < :endDate");
      params.put("endDate", endDate);
    }
    return listAppointments(hql.toString(), params);
  }

  @Override
  public List<Appointment> search(AppointmentSearchRequest appointmentSearchRequest) {
    StringBuilder hql = new StringBuilder("select distinct a from Appointment a join a.patient p");
    Map<String, Object> params = new HashMap<>();
    if (StringUtils.isNotEmpty(appointmentSearchRequest.getProviderUuid())) {
      hql.append(" join a.providers aps join aps.provider provider");
    }
    hql.append(" where a.voided = false and p.voided = false and p.personVoided = false");

    if (appointmentSearchRequest.getStartDate() != null) {
      hql.append(" and a.startDateTime >= :startDate");
      params.put("startDate", appointmentSearchRequest.getStartDate());
    }
    if (appointmentSearchRequest.getEndDate() != null) {
      hql.append(" and a.startDateTime <= :endDate");
      params.put("endDate", appointmentSearchRequest.getEndDate());
    }
    if (StringUtils.isNotEmpty(appointmentSearchRequest.getPatientUuid())) {
      hql.append(" and p.uuid = :patientUuid");
      params.put("patientUuid", appointmentSearchRequest.getPatientUuid());
    }
    if (StringUtils.isNotEmpty(appointmentSearchRequest.getProviderUuid())) {
      hql.append(" and provider.uuid = :providerUuid");
      params.put("providerUuid", appointmentSearchRequest.getProviderUuid());
    }
    if (appointmentSearchRequest.getStatus() != null) {
      hql.append(" and a.status = :status");
      params.put("status", appointmentSearchRequest.getStatus());
    }
    if (StringUtils.isNotEmpty(appointmentSearchRequest.getAppointmentNumber())) {
      hql.append(" and a.appointmentNumber = :appointmentNumber");
      params.put("appointmentNumber", appointmentSearchRequest.getAppointmentNumber());
    }
    hql.append(" order by a.startDateTime asc");

    Query<Appointment> query = createAppointmentQuery(hql.toString(), params);
    if (appointmentSearchRequest.getLimit() > 0) {
      query.setMaxResults(appointmentSearchRequest.getLimit());
    } else if (appointmentSearchRequest.getEndDate() == null) {
      query.setMaxResults(APPOINTMENT_SEARCH_DEFAULT_LIMIT);
    }
    return query.list();
  }

  @Override
  public List<Appointment> getAppointmentsForPatient(Integer patientId) {
    return listAppointments(
        "select a from Appointment a join a.patient p where p.patientId = :patientId"
            + " and a.voided = false and p.voided = false and p.personVoided = false"
            + " and a.startDateTime >= :startOfDay",
        Map.of("patientId", patientId, "startOfDay", DateUtil.getStartOfDay()));
  }

  @Override
  public List<Appointment> getAppointmentsWithoutDates(
      AppointmentSearchRequestModel searchQuery, Integer limit) {
    AppointmentSearchQuery appointmentSearchQuery = buildAppointmentSearchQuery(searchQuery);
    String hql =
        appointmentSearchQuery.hql()
            + " and a.startDateTime is null and a.endDateTime is null order by a.dateCreated asc";
    Query<Appointment> query = createAppointmentQuery(hql, appointmentSearchQuery.params());
    if (limit != null) {
      query.setMaxResults(limit);
    }
    return query.list();
  }

  @Override
  public List<Appointment> getAppointmentsByUuids(List<String> uuids) {
    if (uuids == null || uuids.isEmpty()) {
      return Collections.emptyList();
    }
    return listAppointments(
        "select a from Appointment a where a.uuid in (:uuids) and a.voided = false",
        Map.of("uuids", uuids));
  }

  private AppointmentSearchQuery buildAppointmentSearchQuery(
      AppointmentSearchRequestModel searchQuery) {
    StringBuilder hql =
        new StringBuilder("select distinct a from Appointment a join a.patient p join a.service s");
    Map<String, Object> params = new HashMap<>();
    if (searchQuery != null) {
      if (searchQuery.getServiceTypeUuids() != null
          && !searchQuery.getServiceTypeUuids().isEmpty()) {
        hql.append(" join a.serviceType st");
      }
      if (searchQuery.getProviderUuids() != null && !searchQuery.getProviderUuids().isEmpty()) {
        hql.append(" join a.providers aps join aps.provider provider");
      }
      if (searchQuery.getLocationUuids() != null && !searchQuery.getLocationUuids().isEmpty()) {
        hql.append(" join a.location location");
      }
    }
    hql.append(" where p.voided = false and p.personVoided = false");
    if (searchQuery != null) {
      addCollectionFilter(hql, params, "p.uuid", "patientUuids", searchQuery.getPatientUuids());
      addCollectionFilter(hql, params, "s.uuid", "serviceUuids", searchQuery.getServiceUuids());
      addCollectionFilter(
          hql, params, "st.uuid", "serviceTypeUuids", searchQuery.getServiceTypeUuids());
      addCollectionFilter(
          hql, params, "provider.uuid", "providerUuids", searchQuery.getProviderUuids());
      addCollectionFilter(
          hql, params, "location.uuid", "locationUuids", searchQuery.getLocationUuids());
      if (searchQuery.getStatus() != null) {
        hql.append(" and a.status = :status");
        params.put("status", AppointmentStatus.valueOf(searchQuery.getStatus()));
      }
      if (searchQuery.getPriorities() != null && !searchQuery.getPriorities().isEmpty()) {
        hql.append(" and a.priority in (:priorities)");
        params.put(
            "priorities",
            searchQuery.getPriorities().stream().map(AppointmentPriority::valueOf).toList());
      }
    }
    return new AppointmentSearchQuery(hql.toString(), params);
  }

  private void addEqual(
      StringBuilder hql, Map<String, Object> params, String property, String name, Object value) {
    if (value != null) {
      hql.append(" and ").append(property).append(" = :").append(name);
      params.put(name, value);
    }
  }

  private void addCollectionFilter(
      StringBuilder hql,
      Map<String, Object> params,
      String property,
      String name,
      Collection<?> value) {
    if (value != null && !value.isEmpty()) {
      hql.append(" and ").append(property).append(" in (:").append(name).append(")");
      params.put(name, value);
    }
  }

  private List<Appointment> listAppointments(String hql, Map<String, Object> params) {
    return createAppointmentQuery(hql, params).list();
  }

  private Appointment uniqueAppointment(String hql, Map<String, Object> params) {
    return createAppointmentQuery(hql, params).uniqueResult();
  }

  private Query<Appointment> createAppointmentQuery(String hql, Map<String, Object> params) {
    Query<Appointment> query =
        sessionFactory.getCurrentSession().createQuery(hql, Appointment.class);
    bindParameters(query, params);
    return query;
  }

  private void bindParameters(Query<?> query, Map<String, Object> params) {
    params.forEach(
        (name, value) -> {
          if (value instanceof Collection<?> collection) {
            query.setParameterList(name, collection);
          } else {
            query.setParameter(name, value);
          }
        });
  }

  private record AppointmentSearchQuery(String hql, Map<String, Object> params) {}
}
