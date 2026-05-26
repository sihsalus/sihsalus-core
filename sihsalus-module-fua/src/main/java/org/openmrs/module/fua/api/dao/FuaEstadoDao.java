package org.openmrs.module.fua.api.dao;

import java.util.List;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.fua.FuaEstado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("fua.FuaEstadoDao")
public class FuaEstadoDao {

  @Autowired private DbSessionFactory sessionFactory;

  private DbSession getSession() {
    return sessionFactory.getCurrentSession();
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @SuppressWarnings("unchecked")
  public List<FuaEstado> getAllEstados() {
    return getSession().createQuery("FROM FuaEstado").getResultList();
  }

  public FuaEstado getEstado(Integer id) {
    return (FuaEstado) getSession().get(FuaEstado.class, id);
  }

  public FuaEstado saveEstado(FuaEstado estado) {
    return (FuaEstado) getSession().merge(estado);
  }

  public void purgeEstado(FuaEstado estado) {
    getSession().delete(estado);
  }

  public FuaEstado getByUuid(String uuid) {
    return (FuaEstado)
        getSession()
            .createQuery("FROM FuaEstado fe WHERE fe.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
  }
}
