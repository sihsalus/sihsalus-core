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

import com.fasterxml.jackson.databind.JsonNode;
import org.openmrs.annotation.Authorized;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.imaging.OrthancConfiguration;
import org.openmrs.module.imaging.api.client.OrthancHttpClient;
import org.openmrs.module.imaging.api.dao.DicomStudyDao;
import org.openmrs.module.imaging.api.study.DicomInstance;
import org.openmrs.module.imaging.api.study.DicomSeries;
import org.openmrs.module.imaging.api.study.DicomStudy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_DELETE_IMAGE_DATA;
import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_LINK_IMAGE_STUDIES;
import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_RECEIVE_ORTHANC_UPDATES;
import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_UPLOAD_IMAGE_DATA;
import static org.openmrs.module.imaging.ImagingConstants.PRIVILEGE_VIEW_IMAGE_DATA;

/**
 * This service exposes module's core functionality. It is a Spring managed bean which is configured
 * in moduleApplicationContext.xml.
 * <p>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(DicomStudyService.class).someMethod();
 * </code>
 * 
 * @see org.openmrs.api.context.Context
 */
@Service
@Transactional
public interface DicomStudyService extends OpenmrsService {
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	DicomStudyDao getDao();
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	List<DicomStudy> getStudiesOfPatient(Patient pt);
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	List<DicomStudy> getStudiesByConfiguration(OrthancConfiguration config);
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	List<DicomStudy> getAllStudies();
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	DicomStudy getDicomStudy(int id);
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	DicomStudy getDicomStudy(OrthancConfiguration config, String studyInstanceUID);
	
	@Authorized(PRIVILEGE_RECEIVE_ORTHANC_UPDATES)
	void fetchAllStudies() throws IOException;
	
	@Authorized(PRIVILEGE_RECEIVE_ORTHANC_UPDATES)
	void fetchAllStudies(OrthancConfiguration orthancConfiguration) throws IOException;
	
	@Authorized(PRIVILEGE_RECEIVE_ORTHANC_UPDATES)
	void createOrUpdateStudy(OrthancConfiguration orthancConfiguration, JsonNode studyData);
	
	@Authorized(PRIVILEGE_RECEIVE_ORTHANC_UPDATES)
	void fetchNewChangedStudies() throws IOException;
	
	@Authorized(PRIVILEGE_RECEIVE_ORTHANC_UPDATES)
	void fetchNewChangedStudiesByConfiguration(OrthancConfiguration orthancConfiguration) throws IOException;
	
	@Authorized(PRIVILEGE_RECEIVE_ORTHANC_UPDATES)
	void fetchNewChangedStudiesByConfigurationAndStudyUIDs(OrthancConfiguration config, List<String> orthancStudyIds)
	        throws IOException;
	
	@Authorized(PRIVILEGE_UPLOAD_IMAGE_DATA)
	int uploadFile(OrthancConfiguration config, InputStream is) throws IOException;
	
	@Authorized(PRIVILEGE_LINK_IMAGE_STUDIES)
	void setPatient(DicomStudy study, Patient patient);
	
	@Authorized(PRIVILEGE_DELETE_IMAGE_DATA)
	void deleteStudy(DicomStudy dicomStudy) throws IOException;
	
	@Authorized(PRIVILEGE_DELETE_IMAGE_DATA)
	void deleteStudyFromOpenmrs(DicomStudy dicomStudy);
	
	@Authorized(PRIVILEGE_DELETE_IMAGE_DATA)
	void deleteSeries(String seriesOrthancUID, DicomStudy study) throws IOException;
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	List<DicomSeries> fetchSeries(DicomStudy study) throws IOException;
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	List<DicomInstance> fetchInstances(String seriesInstanceUID, DicomStudy study) throws IOException;
	
	void setHttpClient(OrthancHttpClient client);
	
	@Authorized(PRIVILEGE_LINK_IMAGE_STUDIES)
	void updateLinkStatus(DicomStudy study, int newLinkStatus);
	
	class PreviewResult {
		
		public byte[] data;
		
		public String contentType;
	}
	
	@Authorized(PRIVILEGE_VIEW_IMAGE_DATA)
	PreviewResult fetchInstancePreview(String orthancInstanceUID, DicomStudy study) throws IOException;
}
