/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.imaging.api;

import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_EDIT_WORKLIST;
import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_VIEW_IMAGE_DATA;

import java.io.IOException;
import java.util.List;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.openmrs.module.imaging.api.worklist.RequestProcedure;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public interface RequestProcedureService extends OpenmrsService {

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  List<RequestProcedure> getAllRequestProcedures();

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  List<RequestProcedure> getRequestProceduresByStatus(String status);

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  List<RequestProcedure> getAllByStudyInstanceUID(String studyInstanceUID);

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  List<RequestProcedure> getRequestProcedureByPatient(Patient pt);

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  RequestProcedure getRequestProcedure(int requestProcedureId);

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  RequestProcedure getRequestProcedureByAccessionNUmber(String accessionNumber);

  @Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
  List<RequestProcedure> getRequestProcedureByConfig(OrthancConfiguration orthancConfiguration);

  @Authorized(PRIVILEGE_EDIT_WORKLIST)
  void deleteRequestProcedure(RequestProcedure requestProcedure) throws IOException;

  @Authorized(PRIVILEGE_EDIT_WORKLIST)
  void newRequest(RequestProcedure requestProcedure) throws IOException;

  @Authorized(PRIVILEGE_EDIT_WORKLIST)
  void updateRequestStatus(RequestProcedure requestProcedure);
}
