/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.encounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.EmrApiConceptMappings;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.encounter.exception.ConceptNotFoundException;
import org.openmrs.module.emrapi.encounter.mapper.UserMapper;

public class DispositionMapper {

  private final ConceptService conceptService;

  private final UserMapper userMapper;

  private final EmrApiProperties emrApiProperties;

  public DispositionMapper(ConceptService conceptService, UserMapper userMapper) {
    this(conceptService, userMapper, null);
  }

  public DispositionMapper(
      ConceptService conceptService, UserMapper userMapper, EmrApiProperties emrApiProperties) {
    this.conceptService = conceptService;
    this.userMapper = userMapper;
    this.emrApiProperties = emrApiProperties;
  }

  public EncounterTransaction.Disposition getDisposition(Obs obs) {
    if (obs.isVoided()) return null;
    EncounterTransaction.Disposition disposition = new EncounterTransaction.Disposition();
    Set<Obs> groupMembers = obs.getGroupMembers();
    List<EncounterTransaction.Observation> additionalObservations =
        new ArrayList<EncounterTransaction.Observation>();
    for (Obs groupMember : groupMembers) {
      if (isDisposition(groupMember)) {
        disposition.setCode(
            getConceptMappingCodeBySource(
                getConceptMappings().getConceptSourceName(),
                groupMember.getValueCoded().getConceptMappings()));
        disposition.setVoided(groupMember.getVoided());
        disposition.setVoidReason(groupMember.getVoidReason());
        disposition.setExistingObs(groupMember.getUuid());
        disposition.setConceptName(groupMember.getValueCoded().getName().getName());
      } else {
        EncounterTransaction.Observation observation = new EncounterTransaction.Observation();
        observation.setConcept(
            new EncounterTransaction.Concept(
                groupMember.getConcept().getUuid(),
                groupMember.getConcept().getName().getName(),
                groupMember.getConcept().isSet()));
        observation.setValue(groupMember.getValueAsString(Context.getLocale()));
        observation.setVoidReason(groupMember.getVoidReason());
        observation.setVoided(groupMember.getVoided());
        observation.setComment(groupMember.getComment());
        observation.setUuid(groupMember.getUuid());
        observation.setCreator(userMapper.map(groupMember.getCreator()));
        additionalObservations.add(observation);
      }
    }
    disposition.setAdditionalObs(additionalObservations);
    disposition.setDispositionDateTime(obs.getObsDatetime());
    return disposition;
  }

  private String getConceptMappingCodeBySource(
      String source, Collection<ConceptMap> conceptMappings) {
    for (ConceptMap conceptMapping : conceptMappings) {
      if (conceptMapping.getConceptReferenceTerm().getConceptSource().getName().equals(source)) {
        return conceptMapping.getConceptReferenceTerm().getCode();
      }
    }
    return null;
  }

  private boolean isDisposition(Obs obs) {
    Concept dispositionConcept = getDispositionConcept();
    return obs.getConcept().getUuid().equals(dispositionConcept.getUuid());
  }

  private Concept getDispositionConcept() {
    EmrApiConceptMappings mappings = getConceptMappings();
    Concept concept =
        conceptService.getConceptByMapping(
            mappings.getDispositionCode(), mappings.getConceptSourceName());
    if (concept == null) {
      throw new ConceptNotFoundException(
          "Disposition concept does not exist. Code : " + mappings.getDispositionCode());
    }
    return concept;
  }

  private EmrApiConceptMappings getConceptMappings() {
    return emrApiProperties == null
        ? EmrApiConceptMappings.defaults()
        : emrApiProperties.getConceptMappings();
  }
}
