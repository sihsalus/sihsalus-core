package org.openmrs.module.fua.api.impl;

import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fua.FuaEstado;
import org.openmrs.module.fua.FuaEstadoVersion;
import org.openmrs.module.fua.api.FuaEstadoVersionService;
import org.openmrs.module.fua.api.dao.FuaEstadoVersionDao;

public class FuaEstadoVersionServiceImpl extends BaseOpenmrsService
    implements FuaEstadoVersionService {

  private FuaEstadoVersionDao dao;

  public void setDao(FuaEstadoVersionDao dao) {
    this.dao = dao;
  }

  @Override
  public FuaEstadoVersion saveFuaEstadoVersion(FuaEstado fuaEstado, String descripcion)
      throws APIException {

    FuaEstadoVersion version = new FuaEstadoVersion(fuaEstado);
    version.setDescripcion(descripcion);

    return dao.saveFuaEstadoVersion(version);
  }
}
