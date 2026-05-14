package org.sihsalus.module.patientdocuments;

import org.openmrs.module.patientdocuments.reports.PatientIdStickerPdfReport;
import org.openmrs.module.patientdocuments.web.rest.controller.PatientIdStickerDataPdfExportController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {PatientIdStickerPdfReport.class, PatientIdStickerDataPdfExportController.class})
public class SihsalusPatientDocumentsConfiguration {}
