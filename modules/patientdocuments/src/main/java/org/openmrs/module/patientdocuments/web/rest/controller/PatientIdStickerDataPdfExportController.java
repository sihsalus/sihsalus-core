/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.web.rest.controller;

import static org.openmrs.module.patientdocuments.common.PatientDocumentsConstants.MODULE_ARTIFACT_ID;
import static org.openmrs.module.patientdocuments.common.PatientDocumentsConstants.PATIENT_ID_STICKER_ID;

import org.openmrs.Patient;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.patientdocuments.common.PatientDocumentsPrivilegeConstants;
import org.openmrs.module.patientdocuments.reports.PatientIdStickerPdfReport;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(
    value =
        "/rest/" + RestConstants.VERSION_1 + "/" + MODULE_ARTIFACT_ID + "/" + PATIENT_ID_STICKER_ID)
public class PatientIdStickerDataPdfExportController extends BaseRestController {

  private static final Logger logger =
      LoggerFactory.getLogger(PatientIdStickerDataPdfExportController.class);

  private PatientIdStickerPdfReport pdfReport;

  private PatientService ps;

  @Autowired
  public PatientIdStickerDataPdfExportController(
      @Qualifier("patientService") PatientService ps, PatientIdStickerPdfReport pdfReport) {
    this.ps = ps;
    this.pdfReport = pdfReport;
  }

  private ResponseEntity<byte[]> writeResponse(Patient patient, boolean inline) {
    try {
      byte[] pdfBytes = pdfReport.generatePdf(patient);

      HttpHeaders headers = new HttpHeaders();
      headers.set("Content-Type", "application/pdf");
      String disposition = inline ? "inline" : "attachment";
      headers.add(
          "Content-Disposition", disposition + "; filename=\"" + PATIENT_ID_STICKER_ID + ".pdf\"");
      headers.add("X-Content-Type-Options", "nosniff");
      headers.setContentLength(pdfBytes.length);

      return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    } catch (APIAuthenticationException | ContextAuthenticationException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .contentType(MediaType.TEXT_PLAIN)
          .body(e.getMessage().getBytes());
    } catch (Exception e) {
      logger.error("An error occurred while processing the request", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.TEXT_PLAIN)
          .body("Error generating PDF".getBytes());
    }
  }

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<byte[]> getPatientIdSticker(
      @RequestParam(value = "patientUuid", required = false) String patientUuid,
      @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline) {
    try {
      if (patientUuid == null || patientUuid.isBlank()) {
        return ResponseEntity.badRequest().build();
      }

      Context.requirePrivilege(PatientDocumentsPrivilegeConstants.VIEW_PATIENT_ID_STICKER);
      Patient patient = ps.getPatientByUuid(patientUuid);
      if (patient == null) {
        return ResponseEntity.notFound().build();
      }

      return writeResponse(patient, inline);
    } catch (APIAuthenticationException | ContextAuthenticationException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .contentType(MediaType.TEXT_PLAIN)
          .body(e.getMessage().getBytes());
    }
  }
}
