package org.openmrs.module.appointments.dao.impl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.module.appointments.dao.AppointmentRecurringPatternDao;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.springframework.transaction.annotation.Transactional;

public class AppointmentRecurringPatternDaoImpl implements AppointmentRecurringPatternDao {

  private SessionFactory sessionFactory;

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Transactional
  @Override
  public void save(AppointmentRecurringPattern appointmentRecurringPattern) {
    session().merge(appointmentRecurringPattern);
  }

  @Override
  public List<AppointmentRecurringPattern> getAllAppointmentRecurringPatterns() {
    return session()
        .createQuery(
            "select pattern from AppointmentRecurringPattern pattern",
            AppointmentRecurringPattern.class)
        .list();
  }

  private Session session() {
    return sessionFactory.getCurrentSession();
  }
}
