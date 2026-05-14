package org.openmrs.module.appointments.dao.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.module.appointments.dao.AppointmentServiceAttributeTypeDao;
import org.openmrs.module.appointments.model.AppointmentServiceAttributeType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AppointmentServiceAttributeTypeDaoImpl implements AppointmentServiceAttributeTypeDao {
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<AppointmentServiceAttributeType> getAllAttributeTypes(boolean includeRetired) {
        String hql = "select type from AppointmentServiceAttributeType type"
                + (includeRetired ? "" : " where type.retired = false");
        return sessionFactory.getCurrentSession()
                .createQuery(hql, AppointmentServiceAttributeType.class)
                .list();
    }

    @Override
    public AppointmentServiceAttributeType getAttributeTypeByUuid(String uuid) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "select type from AppointmentServiceAttributeType type where type.uuid = :uuid",
                        AppointmentServiceAttributeType.class)
                .setParameter("uuid", uuid)
                .setMaxResults(1)
                .uniqueResult();
    }

    @Override
    public AppointmentServiceAttributeType getAttributeTypeById(Integer id) {
        return (AppointmentServiceAttributeType) sessionFactory.getCurrentSession().get(AppointmentServiceAttributeType.class, id);
    }

    @Transactional
    @Override
    public AppointmentServiceAttributeType save(AppointmentServiceAttributeType attributeType) {
        Session currentSession = sessionFactory.getCurrentSession();
        return currentSession.merge(attributeType);
    }
}
