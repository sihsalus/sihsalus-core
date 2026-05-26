/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */
package org.openmrs.module.imaging.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.openmrs.module.imaging.api.DicomStudyService;
import org.openmrs.module.imaging.api.OrthancConfigurationService;
import org.openmrs.module.imaging.api.client.OrthancHttpClient;
import org.openmrs.module.imaging.api.dao.DicomStudyDao;
import org.openmrs.module.imaging.api.study.DicomInstance;
import org.openmrs.module.imaging.api.study.DicomSeries;
import org.openmrs.module.imaging.api.study.DicomStudy;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DicomStudyServiceImpl extends BaseOpenmrsService implements DicomStudyService {

  protected final Log log = LogFactory.getLog(this.getClass());

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private OrthancHttpClient httpClient = new OrthancHttpClient();

  private DicomStudyDao dao;

  /**
   * @param dao the dao to set
   */
  public void setDao(DicomStudyDao dao) {
    this.dao = dao;
  }

  /**
   * @return the dao
   */
  public DicomStudyDao getDao() {
    return dao;
  }

  public void setHttpClient(OrthancHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void updateLinkStatus(DicomStudy study, int newLinkStatus) {
    dao.updateLinkStatus(study, newLinkStatus);
  }

  /**
   * @throws IOException the IO exception
   */
  @Override
  public void fetchAllStudies() throws IOException {
    OrthancConfigurationService orthancConfigurationService =
        Context.getService(OrthancConfigurationService.class);
    List<OrthancConfiguration> configs = orthancConfigurationService.getAllOrthancConfigurations();
    for (OrthancConfiguration config : configs) {
      fetchAllStudies(config);
    }
  }

  /**
   * @param config the configuration
   * @throws IOException the IO exception
   */
  @Override
  public void fetchAllStudies(OrthancConfiguration config) throws IOException {
    log.info("Fetching all studies from orthanc server " + config.getOrthancBaseUrl());
    HttpURLConnection con =
        httpClient.createConnection(
            "POST",
            config.getOrthancBaseUrl(),
            "/tools/find",
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    try {
      httpClient.sendOrthancQuery(con, orthancFindQuery("Studies", null, null));
      int status = con.getResponseCode();
      if (status == HttpURLConnection.HTTP_OK) {
        try (InputStream response = con.getInputStream()) {
          JsonNode studiesData = OBJECT_MAPPER.readTree(response);
          for (JsonNode studyData : studiesData) {
            createOrUpdateStudy(config, studyData);
          }
        }
      } else {
        OrthancHttpClient.throwConnectionException(config, con);
      }
    } finally {
      con.disconnect();
    }
  }

  /**
   * @param config the orthanc configuration
   * @param studyData the patient image study data
   */
  public void createOrUpdateStudy(OrthancConfiguration config, JsonNode studyData) {
    String studyInstanceUID = studyData.path("MainDicomTags").path("StudyInstanceUID").asText();
    String orthancStudyUID = studyData.path("ID").asText();
    String patientName = studyData.path("PatientMainDicomTags").path("PatientName").asText();
    String studyDate =
        Optional.ofNullable(studyData.path("MainDicomTags").path("StudyDate").asText()).orElse("");
    String studyTime =
        Optional.ofNullable(studyData.path("MainDicomTags").path("StudyTime").asText()).orElse("");
    String studyDescription =
        Optional.ofNullable(studyData.path("MainDicomTags").path("StudyDescription").asText())
            .orElse("");
    String gender =
        Optional.ofNullable(studyData.path("PatientMainDicomTags").path("Gender").asText())
            .orElse("");
    DicomStudy study =
        new DicomStudy(
            studyInstanceUID,
            orthancStudyUID,
            0,
            60,
            "{\"differences\":[], \"score\": 0}",
            null,
            config,
            patientName,
            studyDate,
            studyTime,
            studyDescription,
            gender);

    DicomStudy existingStudy = dao.getByStudyInstanceUID(config, studyInstanceUID);
    // new study? -> save new
    if (existingStudy == null) {
      dao.save(study);
    } else {
      // existing study? -> update
      // DICOM studies are immutable. Only the Orthanc ID can change.
      existingStudy.setOrthancStudyUID(study.getOrthancStudyUID());
      dao.save(existingStudy);
    }
  }

  /**
   * @param config the orthanc configuration
   * @param is the input strem
   * @return the respose code
   * @throws IOException the IO exception
   */
  @Override
  public int uploadFile(OrthancConfiguration config, InputStream is) throws IOException {
    HttpURLConnection con =
        httpClient.createConnection(
            "POST",
            config.getOrthancBaseUrl(),
            "/instances",
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    con.setRequestProperty("Content-Type", "application/dicom");
    con.setDoOutput(true);
    try {
      try (OutputStream outputStream = con.getOutputStream()) {
        IOUtils.copy(is, outputStream);
      }
      return con.getResponseCode();
    } finally {
      con.disconnect();
    }
  }

  /**
   * @param pt the openmrs patient
   * @return the list of studies
   */
  @Override
  public List<DicomStudy> getStudiesOfPatient(Patient pt) {
    return dao.getByPatient(pt);
  }

  public List<DicomStudy> getStudiesByConfiguration(OrthancConfiguration config) {
    return dao.getByConfiguration(config);
  }

  /**
   * @return the list dicom studies
   */
  @Override
  public List<DicomStudy> getAllStudies() {
    return dao.getAll();
  }

  /**
   * @throws IOException the IO exception
   */
  @Override
  public void fetchNewChangedStudies() throws IOException {
    OrthancConfigurationService orthancConfigurationService =
        Context.getService(OrthancConfigurationService.class);
    List<OrthancConfiguration> configs = orthancConfigurationService.getAllOrthancConfigurations();
    for (OrthancConfiguration config : configs) {
      fetchNewChangedStudiesByConfiguration(config);
    }
  }

  /**
   * @param config The orthanc configuation
   * @throws IOException the IO exception
   */
  public void fetchNewChangedStudiesByConfiguration(OrthancConfiguration config)
      throws IOException {
    // repeat until all updates have been received
    while (true) {
      // get changes from server
      String params = "?limit=1000";
      int lastChangedIndex =
          config.getLastChangedIndex() == null ? -1 : config.getLastChangedIndex();
      if (lastChangedIndex != -1) {
        params += "&since=" + lastChangedIndex;
      }
      HttpURLConnection con =
          httpClient.createConnection(
              "GET",
              config.getOrthancBaseUrl(),
              "/changes" + params,
              config.getOrthancUsername(),
              config.getOrthancPassword());
      if (con == null) {
        throw new IOException("Failed to create HTTP connection");
      }
      try {
        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
          // collect changes
          JsonNode changesData;
          try (InputStream response = con.getInputStream()) {
            changesData = OBJECT_MAPPER.readTree(response);
          }
          JsonNode changes = changesData.path("Changes");
          List<String> orthancStudyIds = new ArrayList<>();
          for (JsonNode change : changes) {
            String changeType = change.path("ChangeType").asText();
            if (changeType.equals("NewStudy") || changeType.equals("StableStudy")) {
              orthancStudyIds.add(change.path("ID").asText());
            }
          }
          // update the studies
          fetchNewChangedStudiesByConfigurationAndStudyUIDs(config, orthancStudyIds);
          // remember last processed change
          OrthancConfigurationService orthancConfigurationService =
              Context.getService(OrthancConfigurationService.class);
          config.setLastChangedIndex(changesData.path("Last").asInt(lastChangedIndex));
          orthancConfigurationService.updateOrthancConfiguration(config);
          // stop when all changes read
          if (changesData.path("Done").asBoolean(true)) {
            break;
          }
        } else {
          OrthancHttpClient.throwConnectionException(config, con);
        }
      } finally {
        con.disconnect();
      }
    }
  }

  /**
   * @param config the orthanc configuration
   * @param orthancStudyIds the study instance UIDs
   * @throws IOException the IO exception
   */
  public void fetchNewChangedStudiesByConfigurationAndStudyUIDs(
      OrthancConfiguration config, List<String> orthancStudyIds) throws IOException {
    for (String orthancStudyId : orthancStudyIds) {
      HttpURLConnection con =
          httpClient.createConnection(
              "GET",
              config.getOrthancBaseUrl(),
              "/studies/" + OrthancHttpClient.encodePathSegment(orthancStudyId),
              config.getOrthancUsername(),
              config.getOrthancPassword());
      if (con == null) {
        throw new IOException("Failed to create HTTP connection");
      }
      try {
        // Enable connection reuse (Keep-Alive)
        con.setRequestProperty("Connection", "keep-alive");
        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
          try (InputStream response = con.getInputStream()) {
            JsonNode studyData = OBJECT_MAPPER.readTree(response);
            createOrUpdateStudy(config, studyData);
          }
        } else {
          OrthancHttpClient.throwConnectionException(config, con);
        }
      } finally {
        con.disconnect();
      }
    }
  }

  @Override
  public DicomStudy getDicomStudy(int id) {
    return dao.get(id);
  }

  /**
   * @param studyInstanceUID the study instance UID
   * @return the dicom study
   */
  @Override
  public DicomStudy getDicomStudy(OrthancConfiguration config, String studyInstanceUID) {
    return dao.getByStudyInstanceUID(config, studyInstanceUID);
  }

  /**
   * @param study the dicom study
   * @param patient the openmrs patient
   */
  @Override
  public void setPatient(DicomStudy study, Patient patient) {
    study.setMrsPatient(patient);
    dao.save(study);
  }

  /**
   * @param dicomStudy the dicom study
   */
  @Override
  public void deleteStudy(DicomStudy dicomStudy) throws IOException {
    OrthancConfigurationService orthancConfigurationService =
        Context.getService(OrthancConfigurationService.class);
    OrthancConfiguration config =
        orthancConfigurationService.getOrthancConfiguration(
            dicomStudy.getOrthancConfiguration().getId());
    HttpURLConnection con =
        httpClient.createConnection(
            "DELETE",
            config.getOrthancBaseUrl(),
            "/studies/" + OrthancHttpClient.encodePathSegment(dicomStudy.getOrthancStudyUID()),
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    try {
      int responseCode = con.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK
          || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
        dao.remove(dicomStudy);
      } else {
        OrthancHttpClient.throwConnectionException(config, con);
      }
    } finally {
      con.disconnect();
    }
  }

  @Override
  public void deleteStudyFromOpenmrs(DicomStudy dicomStudy) {
    dao.remove(dicomStudy);
  }

  /**
   * @param seriesOrthancUID the series of the dicom study
   * @param seriesStudy the dicom study
   */
  @Override
  public void deleteSeries(String seriesOrthancUID, DicomStudy seriesStudy) throws IOException {
    OrthancConfigurationService orthancConfigurationService =
        Context.getService(OrthancConfigurationService.class);
    OrthancConfiguration config =
        orthancConfigurationService.getOrthancConfiguration(
            seriesStudy.getOrthancConfiguration().getId());
    HttpURLConnection con =
        httpClient.createConnection(
            "DELETE",
            config.getOrthancBaseUrl(),
            "/series/" + OrthancHttpClient.encodePathSegment(seriesOrthancUID),
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    try {
      int responseCode = con.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        OrthancHttpClient.throwConnectionException(config, con);
      }
    } finally {
      con.disconnect();
    }
  }

  /**
   * @param study the study
   * @return the list of series of the dicom study
   * @throws IOException the IO exception
   */
  @Override
  public List<DicomSeries> fetchSeries(DicomStudy study) throws IOException {
    List<DicomSeries> seriesList = new ArrayList<>();

    OrthancConfiguration config = study.getOrthancConfiguration();
    HttpURLConnection con =
        httpClient.createConnection(
            "POST",
            config.getOrthancBaseUrl(),
            "/tools/find",
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    try {
      httpClient.sendOrthancQuery(
          con, orthancFindQuery("Series", "StudyInstanceUID", study.getStudyInstanceUID()));
      int status = con.getResponseCode();
      if (status == HttpURLConnection.HTTP_OK) {
        try (InputStream response = con.getInputStream()) {
          JsonNode seriesesData = OBJECT_MAPPER.readTree(response);
          for (JsonNode seriesData : seriesesData) {
            String seriesInstanceUID =
                seriesData.path("MainDicomTags").path("SeriesInstanceUID").asText();
            String orthancSeriesUID = seriesData.path("ID").asText();
            String seriesDescription =
                Optional.ofNullable(
                        seriesData.path("MainDicomTags").path("SeriesDescription").asText())
                    .orElse("");
            String seriesNumber = seriesData.path("MainDicomTags").path("SeriesNumber").asText();
            String modality = seriesData.path("MainDicomTags").path("Modality").asText();
            String seriesDate =
                Optional.ofNullable(seriesData.path("MainDicomTags").path("SeriesDate").asText())
                    .orElse("");
            String seriesTime =
                Optional.ofNullable(seriesData.path("MainDicomTags").path("SeriesTime").asText())
                    .orElse("");
            DicomSeries series =
                new DicomSeries(
                    seriesInstanceUID,
                    orthancSeriesUID,
                    config,
                    seriesDescription,
                    seriesNumber,
                    modality,
                    seriesDate,
                    seriesTime);
            seriesList.add(series);
          }
        }
      } else {
        OrthancHttpClient.throwConnectionException(config, con);
      }
    } finally {
      con.disconnect();
    }
    return seriesList;
  }

  /**
   * @param seriesInstanceUID the series instance UID
   * @return the list of the series of the dicom study
   * @throws IOException the IO exception
   */
  @Override
  public List<DicomInstance> fetchInstances(String seriesInstanceUID, DicomStudy study)
      throws IOException {
    List<DicomInstance> instanceList = new ArrayList<>();

    OrthancConfiguration config = study.getOrthancConfiguration();
    HttpURLConnection con =
        httpClient.createConnection(
            "POST",
            config.getOrthancBaseUrl(),
            "/tools/find",
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    try {
      httpClient.sendOrthancQuery(
          con, orthancFindQuery("Instance", "SeriesInstanceUID", seriesInstanceUID));
      int status = con.getResponseCode();
      if (status == HttpURLConnection.HTTP_OK) {
        try (InputStream response = con.getInputStream()) {
          JsonNode instancesData = OBJECT_MAPPER.readTree(response);
          for (JsonNode instanceData : instancesData) {
            String sopInstanceUID =
                instanceData.path("MainDicomTags").path("SOPInstanceUID").asText();
            String orthancInstanceUID = instanceData.path("ID").asText();
            String instanceNumber =
                instanceData.path("MainDicomTags").path("InstanceNumber").asText();
            String imagePositionPatient =
                Optional.ofNullable(
                        instanceData.path("MainDicomTags").path("ImagePositionPatient").asText())
                    .orElse("");
            String numberOfFrames =
                Optional.ofNullable(
                        instanceData.path("MainDicomTags").path("NumberOfFrames").asText())
                    .orElse("");
            DicomInstance instance =
                new DicomInstance(
                    sopInstanceUID,
                    orthancInstanceUID,
                    instanceNumber,
                    imagePositionPatient,
                    numberOfFrames,
                    config);
            instanceList.add(instance);
          }
        }
      } else {
        OrthancHttpClient.throwConnectionException(config, con);
      }
    } finally {
      con.disconnect();
    }
    return instanceList;
  }

  /**
   * @param orthancInstanceUID the orthanc identifier UID
   * @param study the dicom study
   * @return the preview image
   * @throws IOException the IO exception
   */
  @Override
  public PreviewResult fetchInstancePreview(String orthancInstanceUID, DicomStudy study)
      throws IOException {
    OrthancConfiguration config = study.getOrthancConfiguration();
    HttpURLConnection con =
        httpClient.createConnection(
            "GET",
            config.getOrthancBaseUrl(),
            "/instances/" + OrthancHttpClient.encodePathSegment(orthancInstanceUID) + "/preview",
            config.getOrthancUsername(),
            config.getOrthancPassword());
    if (con == null) {
      throw new IOException("Failed to create HTTP connection");
    }
    try {
      int responseCode = con.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        // read image
        try (InputStream inputStream = con.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }

          PreviewResult result = new PreviewResult();
          result.data = outputStream.toByteArray();
          result.contentType = con.getContentType();
          return result;
        }
      } else {
        OrthancHttpClient.throwConnectionException(config, con);
        return null;
      }
    } finally {
      con.disconnect();
    }
  }

  private String orthancFindQuery(String level, String queryTag, String queryValue)
      throws IOException {
    ObjectNode root = OBJECT_MAPPER.createObjectNode();
    root.put("Level", level);
    root.put("Expand", true);
    ObjectNode query = root.putObject("Query");
    if (queryTag != null) {
      query.put(queryTag, queryValue == null ? "" : queryValue);
    }
    return OBJECT_MAPPER.writeValueAsString(root);
  }
}
