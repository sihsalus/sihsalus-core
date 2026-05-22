package org.openmrs.module.fua.api.dao;

import org.openmrs.module.fua.FuaVersion;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("fua.FuaVersionDao")
public class FuaVersionDao {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}

	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public FuaVersion saveFuaVersion(FuaVersion fuaVersion) {
		return (FuaVersion) getSession().merge(fuaVersion);
	}
}
