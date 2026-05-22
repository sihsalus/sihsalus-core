/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.person.image;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.emrapi.EmrApiProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

public class EmrPersonImageServiceImpl extends BaseOpenmrsService implements EmrPersonImageService {

	protected final Log log = LogFactory.getLog(getClass());

	private static final String imageFormat = "jpeg";

	private EmrApiProperties emrApiProperties;

	@Override
	public PersonImage savePersonImage(PersonImage personImage) {
		if (personImage == null) {
			throw new APIException("Person image must not be null");
		}

		Person person = personImage.getPerson();
		validatePerson(person);
		String base64EncodedImage = personImage.getBase64EncodedImage();

		if (StringUtils.isBlank(base64EncodedImage)) {
			return personImage;
		}

		try {
			File imageFile = getPersonImageFile(person);
			File imageDirectory = imageFile.getParentFile();
			if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
				throw new APIException("Could not create person image directory");
			}
			if (!imageDirectory.isDirectory()) {
				throw new APIException("Person image path is not a directory");
			}

			byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedImage);
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(decodedBytes));
			if (bufferedImage == null) {
				throw new APIException("Could not decode person image");
			}

			try {
				ImageIO.write(bufferedImage, imageFormat, imageFile);
			}
			finally {
				bufferedImage.flush();
			}

			personImage.setSavedImage(imageFile);
			log.info("Successfully created patient image at " + imageFile);

		}
		catch (APIException e) {
			throw e;
		}
		catch (IllegalArgumentException e) {
			throw new APIException("Could not decode base64 person image", e);
		}
		catch (Exception e) {
			log.error("Update patient image failed for : " + person);
			throw new APIException("Could not save patient image", e);
		}
		return personImage;
	}

	@Override
	public PersonImage getCurrentPersonImage(Person person) {
		validatePerson(person);
		return new PersonImage(person, getPersonImageFile(person));
	}

	private File getPersonImageFile(Person person) {
		if (emrApiProperties == null) {
			throw new APIException("Emr API properties are not configured");
		}

		File imageDirectory = emrApiProperties.getPersonImageDirectory();
		if (imageDirectory == null) {
			throw new APIException("Person image directory is not configured");
		}

		return new File(imageDirectory, person.getUuid() + "." + imageFormat);
	}

	private void validatePerson(Person person) {
		if (person == null || StringUtils.isBlank(person.getUuid())) {
			throw new APIException("Person with uuid is required for person image operations");
		}
		if (person.getUuid().contains("/") || person.getUuid().contains("\\") || person.getUuid().contains("..")) {
			throw new APIException("Invalid person uuid for person image operations");
		}
	}

	public void setEmrApiProperties(EmrApiProperties emrApiProperties) {
		this.emrApiProperties = emrApiProperties;
	}

}
