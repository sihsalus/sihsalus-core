package org.openmrs.module.fua.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.FuaVersion;
import org.springframework.transaction.annotation.Transactional;

public interface FuaVersionService extends OpenmrsService {

  @Authorized(FuaConfig.MANAGE_FUA_PRIVILEGE)
  @Transactional
  FuaVersion saveFuaVersion(Fua fua, String descripcion) throws APIException;
}
