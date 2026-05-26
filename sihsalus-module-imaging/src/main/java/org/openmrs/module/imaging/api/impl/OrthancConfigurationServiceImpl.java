/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */
package org.openmrs.module.imaging.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.openmrs.module.imaging.api.OrthancConfigurationService;
import org.openmrs.module.imaging.api.client.OrthancHttpClient;
import org.openmrs.module.imaging.api.dao.OrthancConfigurationDao;

public class OrthancConfigurationServiceImpl extends BaseOpenmrsService
    implements OrthancConfigurationService {

  protected Log log = LogFactory.getLog(this.getClass());

  private OrthancHttpClient httpClient = new OrthancHttpClient();

  private OrthancConfigurationDao dao;

  public void setDao(OrthancConfigurationDao dao) {
    this.dao = dao;
  }

  public void setHttpClient(OrthancHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public OrthancConfigurationDao getDao() {
    return dao;
  }

  @Override
  public List<OrthancConfiguration> getAllOrthancConfigurations() {
    return dao.getAll();
  }

  @Override
  public OrthancConfiguration getOrthancConfiguration(int id) {
    return dao.get(id);
  }

  @Override
  public void saveOrthancConfiguration(OrthancConfiguration config) {
    validateOrthancBaseUrl(config);
    if (httpClient.isOrthancReachable(config)) {
      dao.saveNew(config);
    } else {
      throw new IllegalArgumentException(
          "The Orthanc instance is not reachable or credentials are invalid");
    }
  }

  @Override
  public void removeOrthancConfiguration(OrthancConfiguration orthancConfiguration) {
    dao.remove(orthancConfiguration);
  }

  @Override
  public void updateOrthancConfiguration(OrthancConfiguration orthancConfiguration) {
    validateOrthancBaseUrl(orthancConfiguration);
    dao.updateExisting(orthancConfiguration);
  }

  private void validateOrthancBaseUrl(OrthancConfiguration orthancConfiguration) {
    String baseUrl = orthancConfiguration == null ? null : orthancConfiguration.getOrthancBaseUrl();
    if (baseUrl == null || baseUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("Orthanc base URL is required");
    }
    try {
      URI uri = new URI(baseUrl.trim());
      String scheme = uri.getScheme();
      if (scheme == null
          || uri.getHost() == null
          || !("http".equals(scheme.toLowerCase(Locale.ROOT))
              || "https".equals(scheme.toLowerCase(Locale.ROOT)))) {
        throw new IllegalArgumentException("Orthanc base URL must be a valid HTTP(S) URL");
      }
      if (uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
        throw new IllegalArgumentException(
            "Orthanc base URL must not include credentials, query, or fragment");
      }
      orthancConfiguration.setOrthancBaseUrl(uri.toString());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Orthanc base URL must be a valid HTTP(S) URL", e);
    }
  }
}
