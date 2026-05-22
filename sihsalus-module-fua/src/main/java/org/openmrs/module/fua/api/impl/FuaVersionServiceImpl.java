package org.openmrs.module.fua.api.impl;

import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaVersion;
import org.openmrs.module.fua.api.FuaVersionService;
import org.openmrs.module.fua.api.dao.FuaVersionDao;

public class FuaVersionServiceImpl extends BaseOpenmrsService implements FuaVersionService {
	
	private FuaVersionDao dao;
	
	public void setDao(FuaVersionDao dao) {
		this.dao = dao;
	}
	
	@Override
	public FuaVersion saveFuaVersion(Fua fua, String descripcion) throws APIException {
		
		FuaVersion fuaVersion = new FuaVersion(fua);
		fuaVersion.setDescripcion(descripcion);
		
		fua.setVersion(fua.getVersion() + 1);

		return dao.saveFuaVersion(fuaVersion);
	}
}
