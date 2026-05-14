package org.openmrs.module.appointments.dao.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.module.appointments.dao.SpecialityDao;
import org.openmrs.module.appointments.model.Speciality;
import org.springframework.transaction.annotation.Transactional;

public class SpecialityDaoImpl implements SpecialityDao{
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

	@Override
	public Speciality getSpecialityByUuid(String uuid) {
	    return sessionFactory.getCurrentSession()
	            .createQuery("select speciality from Speciality speciality where speciality.uuid = :uuid", Speciality.class)
	            .setParameter("uuid", uuid)
	            .setMaxResults(1)
	            .uniqueResult();
	}

	public List<Speciality> getAllSpecialities() {
	    return sessionFactory.getCurrentSession()
	            .createQuery("select speciality from Speciality speciality", Speciality.class)
	            .list();
	}
    
    @Transactional
	@Override
	public Speciality save(Speciality speciality) {
		 Session currentSession = sessionFactory.getCurrentSession();
		 currentSession.saveOrUpdate(speciality);
	     return speciality;
	}
}
