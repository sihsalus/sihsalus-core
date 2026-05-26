/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi;

import java.util.function.BiFunction;
import org.apache.commons.lang3.StringUtils;

public class EmrApiConceptMappings {

  private final String conceptSourceName;

  private final String sameAsConceptMapTypeUuid;

  private final String narrowerThanConceptMapTypeUuid;

  private final String diagnosisConceptSetCode;

  private final String codedDiagnosisCode;

  private final String nonCodedDiagnosisCode;

  private final String diagnosisOrderCode;

  private final String diagnosisOrderPrimaryCode;

  private final String diagnosisOrderSecondaryCode;

  private final String diagnosisCertaintyCode;

  private final String diagnosisCertaintyConfirmedCode;

  private final String diagnosisCertaintyPresumedCode;

  private final String dispositionConceptSetCode;

  private final String dispositionCode;

  private final String admissionLocationCode;

  private final String internalTransferLocationCode;

  private final String dateOfDeathCode;

  private final String unknownCauseOfDeathCode;

  private final String patientDiedCode;

  private final String admissionDecisionCode;

  private final String denyAdmissionCode;

  private final String consultFreeTextCommentsCode;

  public EmrApiConceptMappings(
      String conceptSourceName,
      String sameAsConceptMapTypeUuid,
      String narrowerThanConceptMapTypeUuid,
      String diagnosisConceptSetCode,
      String codedDiagnosisCode,
      String nonCodedDiagnosisCode,
      String diagnosisOrderCode,
      String diagnosisOrderPrimaryCode,
      String diagnosisOrderSecondaryCode,
      String diagnosisCertaintyCode,
      String diagnosisCertaintyConfirmedCode,
      String diagnosisCertaintyPresumedCode,
      String dispositionConceptSetCode,
      String dispositionCode,
      String admissionLocationCode,
      String internalTransferLocationCode,
      String dateOfDeathCode,
      String unknownCauseOfDeathCode,
      String patientDiedCode,
      String admissionDecisionCode,
      String denyAdmissionCode,
      String consultFreeTextCommentsCode) {
    this.conceptSourceName = conceptSourceName;
    this.sameAsConceptMapTypeUuid = sameAsConceptMapTypeUuid;
    this.narrowerThanConceptMapTypeUuid = narrowerThanConceptMapTypeUuid;
    this.diagnosisConceptSetCode = diagnosisConceptSetCode;
    this.codedDiagnosisCode = codedDiagnosisCode;
    this.nonCodedDiagnosisCode = nonCodedDiagnosisCode;
    this.diagnosisOrderCode = diagnosisOrderCode;
    this.diagnosisOrderPrimaryCode = diagnosisOrderPrimaryCode;
    this.diagnosisOrderSecondaryCode = diagnosisOrderSecondaryCode;
    this.diagnosisCertaintyCode = diagnosisCertaintyCode;
    this.diagnosisCertaintyConfirmedCode = diagnosisCertaintyConfirmedCode;
    this.diagnosisCertaintyPresumedCode = diagnosisCertaintyPresumedCode;
    this.dispositionConceptSetCode = dispositionConceptSetCode;
    this.dispositionCode = dispositionCode;
    this.admissionLocationCode = admissionLocationCode;
    this.internalTransferLocationCode = internalTransferLocationCode;
    this.dateOfDeathCode = dateOfDeathCode;
    this.unknownCauseOfDeathCode = unknownCauseOfDeathCode;
    this.patientDiedCode = patientDiedCode;
    this.admissionDecisionCode = admissionDecisionCode;
    this.denyAdmissionCode = denyAdmissionCode;
    this.consultFreeTextCommentsCode = consultFreeTextCommentsCode;
  }

  public static EmrApiConceptMappings defaults() {
    return from((propertyName, defaultValue) -> defaultValue);
  }

  public static EmrApiConceptMappings from(BiFunction<String, String, String> propertyResolver) {
    return new EmrApiConceptMappings(
        resolve(
            propertyResolver,
            EmrApiConstants.GP_EMR_CONCEPT_SOURCE_NAME,
            EmrApiConstants.EMR_CONCEPT_SOURCE_NAME),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_SAME_AS_CONCEPT_MAP_TYPE_UUID,
            EmrApiConstants.SAME_AS_CONCEPT_MAP_TYPE_UUID),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_NARROWER_THAN_CONCEPT_MAP_TYPE_UUID,
            EmrApiConstants.NARROWER_THAN_CONCEPT_MAP_TYPE_UUID),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_CONCEPT_SET,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_CONCEPT_SET),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_CODED_DIAGNOSIS,
            EmrApiConstants.CONCEPT_CODE_CODED_DIAGNOSIS),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_NON_CODED_DIAGNOSIS,
            EmrApiConstants.CONCEPT_CODE_NON_CODED_DIAGNOSIS),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_ORDER,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_ORDER),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_ORDER_PRIMARY,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_ORDER_PRIMARY),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_ORDER_SECONDARY,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_ORDER_SECONDARY),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_CERTAINTY,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_CERTAINTY),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_CERTAINTY_CONFIRMED,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_CERTAINTY_CONFIRMED),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DIAGNOSIS_CERTAINTY_PRESUMED,
            EmrApiConstants.CONCEPT_CODE_DIAGNOSIS_CERTAINTY_PRESUMED),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DISPOSITION_CONCEPT_SET,
            EmrApiConstants.CONCEPT_CODE_DISPOSITION_CONCEPT_SET),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DISPOSITION,
            EmrApiConstants.CONCEPT_CODE_DISPOSITION),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_ADMISSION_LOCATION,
            EmrApiConstants.CONCEPT_CODE_ADMISSION_LOCATION),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_INTERNAL_TRANSFER_LOCATION,
            EmrApiConstants.CONCEPT_CODE_INTERNAL_TRANSFER_LOCATION),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DATE_OF_DEATH,
            EmrApiConstants.CONCEPT_CODE_DATE_OF_DEATH),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_UNKNOWN_CAUSE_OF_DEATH,
            EmrApiConstants.CONCEPT_CODE_UNKNOWN_CAUSE_OF_DEATH),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_PATIENT_DIED,
            EmrApiConstants.CONCEPT_CODE_PATIENT_DIED),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_ADMISSION_DECISION,
            EmrApiConstants.CONCEPT_CODE_ADMISSION_DECISION),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_DENY_ADMISSION,
            EmrApiConstants.CONCEPT_CODE_DENY_ADMISSION),
        resolve(
            propertyResolver,
            EmrApiConstants.GP_CONCEPT_CODE_CONSULT_FREE_TEXT_COMMENTS,
            EmrApiConstants.CONCEPT_CODE_CONSULT_FREE_TEXT_COMMENTS));
  }

  private static String resolve(
      BiFunction<String, String, String> propertyResolver,
      String propertyName,
      String defaultValue) {
    String value = propertyResolver.apply(propertyName, defaultValue);
    return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
  }

  public String getConceptSourceName() {
    return conceptSourceName;
  }

  public String getSameAsConceptMapTypeUuid() {
    return sameAsConceptMapTypeUuid;
  }

  public String getNarrowerThanConceptMapTypeUuid() {
    return narrowerThanConceptMapTypeUuid;
  }

  public String getDiagnosisConceptSetCode() {
    return diagnosisConceptSetCode;
  }

  public String getCodedDiagnosisCode() {
    return codedDiagnosisCode;
  }

  public String getNonCodedDiagnosisCode() {
    return nonCodedDiagnosisCode;
  }

  public String getDiagnosisOrderCode() {
    return diagnosisOrderCode;
  }

  public String getDiagnosisOrderPrimaryCode() {
    return diagnosisOrderPrimaryCode;
  }

  public String getDiagnosisOrderSecondaryCode() {
    return diagnosisOrderSecondaryCode;
  }

  public String getDiagnosisCertaintyCode() {
    return diagnosisCertaintyCode;
  }

  public String getDiagnosisCertaintyConfirmedCode() {
    return diagnosisCertaintyConfirmedCode;
  }

  public String getDiagnosisCertaintyPresumedCode() {
    return diagnosisCertaintyPresumedCode;
  }

  public String getDispositionConceptSetCode() {
    return dispositionConceptSetCode;
  }

  public String getDispositionCode() {
    return dispositionCode;
  }

  public String getAdmissionLocationCode() {
    return admissionLocationCode;
  }

  public String getInternalTransferLocationCode() {
    return internalTransferLocationCode;
  }

  public String getDateOfDeathCode() {
    return dateOfDeathCode;
  }

  public String getUnknownCauseOfDeathCode() {
    return unknownCauseOfDeathCode;
  }

  public String getPatientDiedCode() {
    return patientDiedCode;
  }

  public String getAdmissionDecisionCode() {
    return admissionDecisionCode;
  }

  public String getDenyAdmissionCode() {
    return denyAdmissionCode;
  }

  public String getConsultFreeTextCommentsCode() {
    return consultFreeTextCommentsCode;
  }
}
