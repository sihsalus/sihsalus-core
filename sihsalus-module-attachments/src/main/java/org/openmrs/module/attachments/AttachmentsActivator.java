/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.attachments;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.EncounterType;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.attachments.AttachmentsConstants.ContentFamily;
import org.openmrs.module.attachments.obs.DefaultAttachmentHandler;
import org.openmrs.module.attachments.obs.ImageAttachmentHandler;
import org.springframework.web.util.HtmlUtils;

/**
 * This class contains the logic that is run every time this module is either
 * started or stopped.
 */
public class AttachmentsActivator extends BaseModuleActivator {

	protected Log log = LogFactory.getLog(getClass());

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

	private static final String DEFAULT_CONCEPT_COMPLEX_NAME = AttachmentsConstants.MODULE_SHORT_ID
			+ " DEFAULT ATTACHMENT";

	private static final String DEFAULT_CONCEPT_COMPLEX_DESCRIPTION = "Concept complex for 'default attachment' complex obs.";

	private static final String IMAGE_CONCEPT_COMPLEX_NAME = AttachmentsConstants.MODULE_SHORT_ID + " IMAGE ATTACHMENT";

	private static final String IMAGE_CONCEPT_COMPLEX_DESCRIPTION = "Concept complex for 'image attachments with thumbnails' complex obs.";

	/**
	 * see core ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing " + AttachmentsConstants.MODULE_NAME + " Module");
	}

	/**
	 * see core ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		log.info(AttachmentsConstants.MODULE_NAME + " Module refreshed");
	}

	/**
	 * see core ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting " + AttachmentsConstants.MODULE_NAME + " Module");
	}

	/**
	 * see core ModuleActivator#started()
	 */
	public void started() {

		// Concepts Complex
		ConceptService conceptService = Context.getConceptService();
		ensureConceptComplex(conceptService, getDefaultAttachmentConceptDefinition());
		ensureConceptComplex(conceptService, getImageAttachmentConceptDefinition());

		// Encounter Type
		{
			final String name = "Attachment Upload";
			final String desc = "Encounters used to record uploads of attachments.";
			final String uuid = getConfiguredUuidOrDefault(AttachmentsConstants.GP_ENCOUNTER_TYPE_UUID,
					AttachmentsConstants.ENCOUNTER_TYPE_UUID);

			EncounterService es = Context.getEncounterService();
			EncounterType encounterType = es.getEncounterTypeByUuid(uuid);

			if (encounterType == null) {
				encounterType = new EncounterType(name, desc);
				encounterType.setUuid(uuid);
				es.saveEncounterType(encounterType);
			}
		}

		log.info(AttachmentsConstants.MODULE_NAME + " Module started");
	}

	ConceptComplexDefinition getDefaultAttachmentConceptDefinition() {
		return new ConceptComplexDefinition(
				getConfiguredUuidOrDefault(AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_UUID,
						AttachmentsConstants.CONCEPT_DEFAULT_UUID),
				getGlobalPropertyOrDefault(AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_NAME,
						DEFAULT_CONCEPT_COMPLEX_NAME),
				getGlobalPropertyOrDefault(AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_DESCRIPTION,
						DEFAULT_CONCEPT_COMPLEX_DESCRIPTION),
				DefaultAttachmentHandler.class.getSimpleName());
	}

	ConceptComplexDefinition getImageAttachmentConceptDefinition() {
		String imageConceptUuid = getConceptComplexUuidMap().get(ContentFamily.IMAGE.toString());
		if (StringUtils.isBlank(imageConceptUuid)) {
			imageConceptUuid = AttachmentsConstants.CONCEPT_IMAGE_UUID;
		}
		return new ConceptComplexDefinition(
				validateUuid(AttachmentsConstants.GP_CONCEPT_COMPLEX_UUID_MAP + "[" + ContentFamily.IMAGE + "]",
						imageConceptUuid),
				getGlobalPropertyOrDefault(AttachmentsConstants.GP_IMAGE_CONCEPT_COMPLEX_NAME,
						IMAGE_CONCEPT_COMPLEX_NAME),
				getGlobalPropertyOrDefault(AttachmentsConstants.GP_IMAGE_CONCEPT_COMPLEX_DESCRIPTION,
						IMAGE_CONCEPT_COMPLEX_DESCRIPTION),
				ImageAttachmentHandler.class.getSimpleName());
	}

	private void ensureConceptComplex(ConceptService conceptService, ConceptComplexDefinition definition) {
		Concept concept = conceptService.getConceptByUuid(definition.getUuid());
		if (concept != null) {
			if (conceptService.getConceptComplex(concept.getConceptId()) == null) {
				throw new APIException("Configured attachment concept " + definition.getUuid()
						+ " exists but is not a complex concept.");
			}
			return;
		}

		ConceptComplex conceptComplex = new ConceptComplex();
		conceptComplex.setUuid(definition.getUuid());
		conceptComplex.setHandler(definition.getHandler());
		ConceptName conceptName = new ConceptName(definition.getName(), Locale.ENGLISH);
		conceptComplex.setFullySpecifiedName(conceptName);
		conceptComplex.setPreferredName(conceptName);
		conceptComplex.setConceptClass(conceptService.getConceptClassByName("Question"));
		conceptComplex.setDatatype(conceptService.getConceptDatatypeByUuid(ConceptDatatype.COMPLEX_UUID));
		conceptComplex.addDescription(new ConceptDescription(definition.getDescription(), Locale.ENGLISH));

		conceptService.saveConcept(conceptComplex);
	}

	private Map<String, String> getConceptComplexUuidMap() {
		String globalProperty = getGlobalProperty(AttachmentsConstants.GP_CONCEPT_COMPLEX_UUID_MAP);
		if (StringUtils.isBlank(globalProperty)) {
			return Collections.emptyMap();
		}

		TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
		};
		try {
			return MAPPER.readValue(HtmlUtils.htmlUnescape(globalProperty), typeRef);
		}
		catch (Exception e) {
			log.warn("Could not parse global property '" + AttachmentsConstants.GP_CONCEPT_COMPLEX_UUID_MAP
					+ "'. Falling back to the built-in attachment concept UUIDs.", e);
			return Collections.emptyMap();
		}
	}

	private String getConfiguredUuidOrDefault(String propertyName, String defaultValue) {
		return validateUuid(propertyName, getGlobalPropertyOrDefault(propertyName, defaultValue));
	}

	private String validateUuid(String propertyName, String uuid) {
		try {
			UUID.fromString(uuid);
		}
		catch (IllegalArgumentException e) {
			throw new APIException("Global property " + propertyName + " must be a valid UUID: " + uuid, e);
		}
		return uuid;
	}

	private String getGlobalPropertyOrDefault(String propertyName, String defaultValue) {
		String value = getGlobalProperty(propertyName);
		return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
	}

	protected String getGlobalProperty(String propertyName) {
		return Context.getAdministrationService().getGlobalProperty(propertyName);
	}

	static class ConceptComplexDefinition {

		private final String uuid;

		private final String name;

		private final String description;

		private final String handler;

		ConceptComplexDefinition(String uuid, String name, String description, String handler) {
			this.uuid = uuid;
			this.name = name;
			this.description = description;
			this.handler = handler;
		}

		String getUuid() {
			return uuid;
		}

		String getName() {
			return name;
		}

		String getDescription() {
			return description;
		}

		String getHandler() {
			return handler;
		}
	}

	/**
	 * see core ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping " + AttachmentsConstants.MODULE_NAME + " Module");
	}

	/**
	 * see core ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info(AttachmentsConstants.MODULE_NAME + " Module stopped");
	}

}
