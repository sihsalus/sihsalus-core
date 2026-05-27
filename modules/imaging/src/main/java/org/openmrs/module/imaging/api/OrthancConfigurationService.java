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

import static org.openmrs.module.imaging.ImagingConstants.TASK_MANAGER_ORTHANC_CONFIGURATION;

import java.util.List;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.openmrs.module.imaging.api.client.OrthancHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service exposes module's core functionality. It is a Spring managed bean which is configured
 * in moduleApplicationContext.xml. <code>
 * Context.getService(OrthancConfigurationService.class).someMethod();
 * </code>
 */
@Service
@Transactional
public interface OrthancConfigurationService extends OpenmrsService {

  @Transactional(readOnly = true)
  @Authorized(TASK_MANAGER_ORTHANC_CONFIGURATION)
  List<OrthancConfiguration> getAllOrthancConfigurations();

  @Transactional(readOnly = true)
  @Authorized(TASK_MANAGER_ORTHANC_CONFIGURATION)
  OrthancConfiguration getOrthancConfiguration(int id);

  void setHttpClient(OrthancHttpClient client);

  @Authorized(TASK_MANAGER_ORTHANC_CONFIGURATION)
  void saveOrthancConfiguration(OrthancConfiguration orthancConfiguration);

  @Authorized(TASK_MANAGER_ORTHANC_CONFIGURATION)
  void removeOrthancConfiguration(OrthancConfiguration orthancConfiguration);

  @Authorized(TASK_MANAGER_ORTHANC_CONFIGURATION)
  void updateOrthancConfiguration(OrthancConfiguration orthancConfiguration);
}
