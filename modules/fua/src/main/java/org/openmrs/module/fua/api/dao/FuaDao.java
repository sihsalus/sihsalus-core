package org.openmrs.module.fua.api.dao;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.hibernate.query.Query;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.fua.Fua;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("fua.FuaDao")
public class FuaDao {

  @Autowired private DbSessionFactory sessionFactory;

  private DbSession getSession() {
    return sessionFactory.getCurrentSession();
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public List<Fua> getAllFuas() {
    return getSession().createQuery("FROM Fua", Fua.class).getResultList();
  }

  public Fua getFua(Integer fuaId) {
    return (Fua) getSession().get(Fua.class, fuaId);
  }

  public Fua saveFua(Fua fua) {
    return (Fua) getSession().merge(fua);
  }

  public void purgeFua(Fua fua) {
    getSession().delete(fua);
  }

  /*public void updateEstado(Integer fuaId, Integer nuevoEstadoId) {
    Fua fua = getFua(fuaId);
    FuaEstado fuaEstado = fuaEstadoService.getEstado(nuevoEstadoId);
    if (fua != null) {
      fua.setFuaEstado(fuaEstado);
      saveFua(fua);
    }
  }*/

  public Fua getFuaByUuid(String uuid) {
    return getSession()
        .createQuery("FROM Fua f WHERE f.uuid = :uuid", Fua.class)
        .setParameter("uuid", uuid)
        .uniqueResult();
  }

  public Fua getFuaByVisitUuid(String visitUuid) {
    return getSession()
        .createQuery(
            "FROM Fua f WHERE f.visitUuid = :visitUuid ORDER BY f.fechaActualizacion DESC",
            Fua.class)
        .setParameter("visitUuid", visitUuid)
        .setMaxResults(1)
        .uniqueResult();
  }

  public List<Fua> getFuasFiltrados(
      String estadoNombre, LocalDate fechaInicio, LocalDate fechaFin, int offset, int limit) {
    String hql = "SELECT f FROM Fua f";

    if (estadoNombre != null) {
      hql += " JOIN f.fuaEstado fe";
    }
    hql += " WHERE 1=1";

    if (estadoNombre != null) {
      hql += " AND fe.nombre = :estado";
    }
    if (fechaInicio != null) {
      hql += " AND f.fechaCreacion >= :inicio";
    }
    if (fechaFin != null) {
      hql += " AND f.fechaCreacion <= :fin";
    }

    Query<Fua> query = getSession().createQuery(hql, Fua.class);

    if (estadoNombre != null) query.setParameter("estado", estadoNombre);
    if (fechaInicio != null) query.setParameter("inicio", toDate(fechaInicio));
    if (fechaFin != null) query.setParameter("fin", toDate(fechaFin));

    query.setFirstResult(offset);
    query.setMaxResults(limit);

    return query.getResultList();
  }

  public List<Fua> getFuasByPatientUuid(String patientUuid) {
    String hql =
        "SELECT f FROM Fua f, org.openmrs.Visit v "
            + "WHERE f.visitUuid = v.uuid AND v.patient.uuid = :patientUuid "
            + "ORDER BY f.fechaCreacion DESC";
    return getSession()
        .createQuery(hql, Fua.class)
        .setParameter("patientUuid", patientUuid)
        .getResultList();
  }

  private Date toDate(LocalDate localDate) {
    return localDate == null
        ? null
        : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
