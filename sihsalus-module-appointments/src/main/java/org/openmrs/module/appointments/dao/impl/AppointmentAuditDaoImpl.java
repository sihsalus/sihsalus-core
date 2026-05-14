package org.openmrs.module.appointments.dao.impl;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.module.appointments.dao.AppointmentAuditDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentAudit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AppointmentAuditDaoImpl implements AppointmentAuditDao{

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Transactional
	@Override
	public void save(AppointmentAudit appointmentAuditEvent) {
		sessionFactory.getCurrentSession().merge(appointmentAuditEvent);
	}

	@Override
	public List<AppointmentAudit> getAppointmentHistoryForAppointment(Appointment appointment) {
		return sessionFactory.getCurrentSession()
				.createQuery("select aa from AppointmentAudit aa where aa.appointment = :appointment", AppointmentAudit.class)
				.setParameter("appointment", appointment)
				.list();
	}

	@Override
	public AppointmentAudit getPriorStatusChangeEvent(Appointment appointment) {
		Query<AppointmentAudit> query = sessionFactory.getCurrentSession()
				.createQuery(
						"select aa from AppointmentAudit aa where aa.appointment = :appointment"
								+ " and aa.status <> :status order by aa.dateCreated desc",
						AppointmentAudit.class);
		query.setParameter("appointment", appointment);
		query.setParameter("status", appointment.getStatus());
		query.setMaxResults(1);
		return query.uniqueResult();
	}

}
