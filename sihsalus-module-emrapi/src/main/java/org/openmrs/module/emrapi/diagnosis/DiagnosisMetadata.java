/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.diagnosis;

import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.emrapi.EmrApiConceptMappings;
import org.openmrs.module.emrapi.descriptor.ConceptSetDescriptor;
import org.openmrs.module.emrapi.descriptor.ConceptSetDescriptorField;

/** Metadata describing how a diagnosis is represented as an Obs group. */
public class DiagnosisMetadata extends ConceptSetDescriptor {

  private Concept diagnosisSetConcept;

  private Concept codedDiagnosisConcept;

  private Concept nonCodedDiagnosisConcept;

  private Concept diagnosisOrderConcept;

  private Concept diagnosisCertaintyConcept;

  private ConceptSource emrConceptSource;

  private EmrApiConceptMappings conceptMappings = EmrApiConceptMappings.defaults();

  public DiagnosisMetadata(ConceptService conceptService, ConceptSource emrConceptSource) {
    this(conceptService, emrConceptSource, EmrApiConceptMappings.defaults());
  }

  public DiagnosisMetadata(
      ConceptService conceptService,
      ConceptSource emrConceptSource,
      EmrApiConceptMappings conceptMappings) {
    this.conceptMappings =
        conceptMappings == null ? EmrApiConceptMappings.defaults() : conceptMappings;
    setup(
        conceptService,
        this.conceptMappings.getConceptSourceName(),
        ConceptSetDescriptorField.required(
            "diagnosisSetConcept", this.conceptMappings.getDiagnosisConceptSetCode()),
        ConceptSetDescriptorField.required(
            "codedDiagnosisConcept", this.conceptMappings.getCodedDiagnosisCode()),
        ConceptSetDescriptorField.required(
            "nonCodedDiagnosisConcept", this.conceptMappings.getNonCodedDiagnosisCode()),
        ConceptSetDescriptorField.required(
            "diagnosisOrderConcept", this.conceptMappings.getDiagnosisOrderCode()),
        ConceptSetDescriptorField.required(
            "diagnosisCertaintyConcept", this.conceptMappings.getDiagnosisCertaintyCode()));
    this.emrConceptSource = emrConceptSource;
  }

  /** Used for testing -- in production you'll use the constructor that takes ConceptService */
  public DiagnosisMetadata() {}

  public Concept getDiagnosisSetConcept() {
    return diagnosisSetConcept;
  }

  public Concept getCodedDiagnosisConcept() {
    return codedDiagnosisConcept;
  }

  public Concept getNonCodedDiagnosisConcept() {
    return nonCodedDiagnosisConcept;
  }

  public Concept getDiagnosisOrderConcept() {
    return diagnosisOrderConcept;
  }

  public Concept getDiagnosisCertaintyConcept() {
    return diagnosisCertaintyConcept;
  }

  public void setDiagnosisSetConcept(Concept diagnosisSetConcept) {
    this.diagnosisSetConcept = diagnosisSetConcept;
  }

  public void setCodedDiagnosisConcept(Concept codedDiagnosisConcept) {
    this.codedDiagnosisConcept = codedDiagnosisConcept;
  }

  public void setNonCodedDiagnosisConcept(Concept nonCodedDiagnosisConcept) {
    this.nonCodedDiagnosisConcept = nonCodedDiagnosisConcept;
  }

  public void setDiagnosisOrderConcept(Concept diagnosisOrderConcept) {
    this.diagnosisOrderConcept = diagnosisOrderConcept;
  }

  public void setDiagnosisCertaintyConcept(Concept diagnosisCertaintyConcept) {
    this.diagnosisCertaintyConcept = diagnosisCertaintyConcept;
  }

  public void setEmrConceptSource(ConceptSource emrConceptSource) {
    this.emrConceptSource = emrConceptSource;
  }

  public Obs buildDiagnosisObsGroup(Diagnosis diagnosis) {
    Concept orderAnswer = findEmrAnswer(diagnosisOrderConcept, getCodeFor(diagnosis.getOrder()));
    Concept certaintyAnswer =
        findEmrAnswer(diagnosisCertaintyConcept, getCodeFor(diagnosis.getCertainty()));

    Obs existingObs = diagnosis.getExistingObs();
    if (existingObs != null) {
      setCodedMember(existingObs, diagnosisOrderConcept, orderAnswer, null);
      setCodedMember(existingObs, diagnosisCertaintyConcept, certaintyAnswer, null);
      setCodedOrFreeTextMember(
          existingObs, diagnosis.getDiagnosis(), codedDiagnosisConcept, nonCodedDiagnosisConcept);
      return existingObs;
    } else {
      Obs order = buildObsFor(diagnosisOrderConcept, orderAnswer, null);
      Obs certainty = buildObsFor(diagnosisCertaintyConcept, certaintyAnswer, null);
      Obs diagnosisObs =
          buildObsFor(diagnosis.getDiagnosis(), codedDiagnosisConcept, nonCodedDiagnosisConcept);

      Obs obs = new Obs();
      obs.setConcept(diagnosisSetConcept);
      obs.addGroupMember(order);
      obs.addGroupMember(certainty);
      obs.addGroupMember(diagnosisObs);
      return obs;
    }
  }

  public boolean isDiagnosis(Obs obsGroup) {
    return obsGroup.getConcept().equals(diagnosisSetConcept);
  }

  public boolean isPrimaryDiagnosis(Obs obsGroup) {
    return isDiagnosis(obsGroup)
        && hasDiagnosisOrder(obsGroup, conceptMappings.getDiagnosisOrderPrimaryCode());
  }

  private boolean hasDiagnosisOrder(Obs obsGroup, String codeForDiagnosisOrderToCheckFor) {
    // return orderObs.getValueCoded()
    throw new RuntimeException("Not Yet Implemented");
  }

  public Diagnosis toDiagnosis(Obs obsGroup) {
    if (!isDiagnosis(obsGroup)) {
      throw new IllegalArgumentException("Not an obs group for a diagnosis: " + obsGroup);
    }

    Obs orderObs = findMember(obsGroup, diagnosisOrderConcept);
    Obs certaintyObs = findMember(obsGroup, diagnosisCertaintyConcept);
    Obs codedObs = findMember(obsGroup, codedDiagnosisConcept);
    Obs nonCodedObs = null;
    if (codedObs == null) {
      nonCodedObs = findMember(obsGroup, nonCodedDiagnosisConcept);
    }
    if (codedObs == null && nonCodedObs == null) {
      throw new IllegalArgumentException(
          "Obs group doesn't contain a coded or non-coded diagnosis: " + obsGroup);
    }
    CodedOrFreeTextAnswer diagnosisValue = buildFrom(codedObs, nonCodedObs);
    Diagnosis diagnosis = new Diagnosis(diagnosisValue, getDiagnosisOrderFrom(orderObs));
    if (certaintyObs != null) {
      diagnosis.setCertainty(getDiagnosisCertaintyFrom(certaintyObs));
    }
    diagnosis.setExistingObs(obsGroup);
    return diagnosis;
  }

  /**
   * @param order
   * @return the Concept representing this diagnosis order
   */
  public Concept getConceptFor(Diagnosis.Order order) {
    if (order == null) {
      return null;
    }
    return findEmrAnswer(getDiagnosisOrderConcept(), getCodeFor(order));
  }

  private Diagnosis.Order getDiagnosisOrderFrom(Obs obs) {
    String mapping = findMapping(obs.getValueCoded());
    return parseDiagnosisOrder(mapping);
  }

  public Concept getConceptFor(Diagnosis.Certainty certainty) {
    if (certainty == null) {
      return null;
    }
    return findEmrAnswer(getDiagnosisCertaintyConcept(), getCodeFor(certainty));
  }

  private Diagnosis.Certainty getDiagnosisCertaintyFrom(Obs certaintyObs) {
    String mapping = findMapping(certaintyObs.getValueCoded());
    return parseDiagnosisCertainty(mapping);
  }

  private Concept findEmrAnswer(Concept concept, String codeForAnswer) {
    return findAnswer(concept, conceptMappings.getConceptSourceName(), codeForAnswer);
  }

  private String getCodeFor(Diagnosis.Order order) {
    if (order == Diagnosis.Order.PRIMARY) {
      return conceptMappings.getDiagnosisOrderPrimaryCode();
    }
    if (order == Diagnosis.Order.SECONDARY) {
      return conceptMappings.getDiagnosisOrderSecondaryCode();
    }
    return null;
  }

  private String getCodeFor(Diagnosis.Certainty certainty) {
    if (certainty == Diagnosis.Certainty.CONFIRMED) {
      return conceptMappings.getDiagnosisCertaintyConfirmedCode();
    }
    if (certainty == Diagnosis.Certainty.PRESUMED) {
      return conceptMappings.getDiagnosisCertaintyPresumedCode();
    }
    return null;
  }

  private Diagnosis.Order parseDiagnosisOrder(String mapping) {
    if (conceptMappings.getDiagnosisOrderPrimaryCode().equals(mapping)) {
      return Diagnosis.Order.PRIMARY;
    }
    if (conceptMappings.getDiagnosisOrderSecondaryCode().equals(mapping)) {
      return Diagnosis.Order.SECONDARY;
    }
    return Diagnosis.Order.parseConceptReferenceCode(mapping);
  }

  private Diagnosis.Certainty parseDiagnosisCertainty(String mapping) {
    if (conceptMappings.getDiagnosisCertaintyConfirmedCode().equals(mapping)) {
      return Diagnosis.Certainty.CONFIRMED;
    }
    if (conceptMappings.getDiagnosisCertaintyPresumedCode().equals(mapping)) {
      return Diagnosis.Certainty.PRESUMED;
    }
    return Diagnosis.Certainty.parseConceptReferenceCode(mapping);
  }

  private String findMapping(Concept concept) {
    for (ConceptMap conceptMap : concept.getConceptMappings()) {
      ConceptReferenceTerm conceptReferenceTerm = conceptMap.getConceptReferenceTerm();
      if (conceptReferenceTerm.getConceptSource().equals(emrConceptSource)) {
        return conceptReferenceTerm.getCode();
      }
    }
    return null;
  }

  private CodedOrFreeTextAnswer buildFrom(Obs codedObs, Obs nonCodedObs) {
    if (codedObs != null) {
      if (codedObs.getValueCodedName() != null) {
        return new CodedOrFreeTextAnswer(codedObs.getValueCodedName());
      } else {
        return new CodedOrFreeTextAnswer(codedObs.getValueCoded());
      }
    } else {
      return new CodedOrFreeTextAnswer(nonCodedObs.getValueText());
    }
  }
}
