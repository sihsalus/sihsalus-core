/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.imaging.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.imaging.api.worklist.RequestProcedure;
import org.openmrs.module.imaging.api.worklist.RequestProcedureStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_EDIT_WORKLIST;
import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_VIEW_IMAGE_DATA;

@Service
@Transactional
public interface RequestProcedureStepService extends OpenmrsService {
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	List<RequestProcedureStep> getAllStepByRequestProcedure(RequestProcedure requestProcedure);
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	RequestProcedureStep getProcedureStep(int Id);
	
	@Authorized(PRIVILEGE_EDIT_WORKLIST)
	void newProcedureStep(RequestProcedureStep requestProcedureStep) throws IOException;
	
	@Authorized(PRIVILEGE_EDIT_WORKLIST)
	void deleteProcedureStep(RequestProcedureStep requestProcedureStep) throws IOException;
	
	@Authorized(PRIVILEGE_EDIT_WORKLIST)
	void updateProcedureStep(RequestProcedureStep requestProcedureStep);
	
	@Authorized(PRIVILEGE_EDIT_WORKLIST)
	void updatePerformedProcedureStepStatus(RequestProcedureStep step, String newStatus);
}
