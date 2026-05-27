/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.patient;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.module.emrapi.person.image.EmrPersonImageService;
import org.openmrs.module.emrapi.person.image.PersonImage;

public class EmrPatientProfileServiceImpl implements EmrPatientProfileService {

  private PatientService patientService;

  private PersonService personService;

  private EmrPersonImageService emrPersonImageService;

  @Override
  public PatientProfile save(PatientProfile patientProfile) {
    if (patientProfile == null || patientProfile.getPatient() == null) {
      throw new APIException("Patient profile with patient is required");
    }

    Patient patient = patientService.savePatient(patientProfile.getPatient());

    saveRelationships(patientProfile.getRelationships());

    patientProfile.setPatient(patient);

    PersonImage personImage = new PersonImage();
    personImage.setPerson(patient);
    personImage.setBase64EncodedImage(patientProfile.getImage());

    emrPersonImageService.savePersonImage(personImage);
    return patientProfile;
  }

  @Override
  public PatientProfile get(String patientUuid) {
    if (StringUtils.isBlank(patientUuid)) {
      throw new APIException("Patient uuid is required");
    }

    PatientProfile delegate = new PatientProfile();

    Patient patient = patientService.getPatientByUuid(patientUuid);
    if (patient == null) {
      throw new APIException("Patient not found: " + patientUuid);
    }
    delegate.setPatient(patient);

    Person person = personService.getPerson(patient.getPersonId());
    List<Relationship> relationships = personService.getRelationshipsByPerson(person);
    delegate.setRelationships(relationships);

    return delegate;
  }

  private void saveRelationships(List<Relationship> relationships) {
    if (relationships == null) {
      return;
    }

    for (Relationship relationship : relationships) {
      if (relationship != null) {
        personService.saveRelationship(relationship);
      }
    }
  }

  public void setPatientService(PatientService patientService) {
    this.patientService = patientService;
  }

  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  public void setEmrPersonImageService(EmrPersonImageService emrPersonImageService) {
    this.emrPersonImageService = emrPersonImageService;
  }
}
