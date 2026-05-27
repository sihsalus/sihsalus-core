package org.sihsalus.module.patientdocuments;

import org.openmrs.module.patientdocuments.PatientDocumentsActivator;
import org.openmrs.module.patientdocuments.library.PatientIdStickerDataSetEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = PatientDocumentsActivator.class)
public class SihsalusPatientDocumentsConfiguration {

  @Bean
  PatientIdStickerDataSetEvaluator patientIdStickerDataSetEvaluator() {
    return new PatientIdStickerDataSetEvaluator();
  }
}
